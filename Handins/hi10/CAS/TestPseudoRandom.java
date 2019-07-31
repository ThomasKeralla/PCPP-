// For week 10
// sestoft@itu.dk * 2014-11-12, 2015-11-03

// Implementing simple (java.util.Random-style) pseudo-random number
// generators and testing their scalability under high contention.

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntToDoubleFunction;

public class TestPseudoRandom {
  public static void main(String[] args) {
    SystemInfo();
    for (int threadCount=1; threadCount<=16; threadCount++) {
      timeMyRandom(new LockingRandom(42), threadCount);
      timeMyRandom(new CasRandom(42), threadCount);
      timeMyRandom(new TLLockingRandom(42), threadCount);
      timeMyRandom(new TLCasRandom(42), threadCount);
      timeMyRandom(new WrappedTLRandom(), threadCount);
    }
  }

  private static void timeMyRandom(final MyRandom random, final int threadCount) {
    final int totalCount = 1_000_000,
      count = (int)(0.5 + (double)totalCount / (double)threadCount);
    Mark7(String.format("%-16s %3d", random.getClass().toString().substring(6), threadCount),
      (int i) -> {
        final CyclicBarrier startBarrier = new CyclicBarrier(threadCount + 1),
          stopBarrier = startBarrier;
        for (int t=0; t<threadCount; t++) {
          new Thread(() -> {
	      try { startBarrier.await(); } catch (Exception exn) { }
	      int result = 0;
	      for (int p=0; p<count; p++)
		result += random.nextInt();
	      try { stopBarrier.await(); } catch (Exception exn) { }
	  }).start();
        }
        try { startBarrier.await(); } catch (Exception exn) { }
        try { stopBarrier.await(); } catch (Exception exn) { }
        return 1.0;
      });
  }

  public static void SystemInfo() {
    System.out.printf("# OS:   %s; %s; %s%n",
                      System.getProperty("os.name"),
                      System.getProperty("os.version"),
                      System.getProperty("os.arch"));
    System.out.printf("# JVM:  %s; %s%n",
                      System.getProperty("java.vendor"),
                      System.getProperty("java.version"));
    // The processor identifier works only on MS Windows:
    System.out.printf("# CPU:  %s; %d \"cores\"%n",
		      System.getenv("PROCESSOR_IDENTIFIER"),
		      Runtime.getRuntime().availableProcessors());
    java.util.Date now = new java.util.Date();
    System.out.printf("# Date: %s%n",
      new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(now));
  }

  public static double Mark7(String msg, IntToDoubleFunction f) {
    int n = 10, count = 1, totalCount = 0;
    double dummy = 0.0, runningTime = 0.0, st = 0.0, sst = 0.0;
    do {
      count *= 2;
      st = sst = 0.0;
      for (int j=0; j<n; j++) {
        Timer t = new Timer();
        for (int i=0; i<count; i++)
          dummy += f.applyAsDouble(i);
        runningTime = t.check();
        double time = runningTime * 1e6 / count;
        st += time;
        sst += time * time;
        totalCount += count;
      }
    } while (runningTime < 0.25 && count < Integer.MAX_VALUE/2);
    double mean = st/n, sdev = Math.sqrt((sst - mean*mean*n)/(n-1));
    System.out.printf("%-25s %15.1f us %10.2f %10d%n", msg, mean, sdev, count);
    return dummy / totalCount;
  }
}

interface MyRandom {
  int nextInt();
}

class LockingRandom implements MyRandom {
  private long seed;

  public LockingRandom(long seed) {
    this.seed = seed;
  }

  // Recipe from java.util.Random.next(int bits) documentation
  public synchronized int nextInt() {
    seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
    return (int)(seed >>> 16);
  }
}

class CasRandom implements MyRandom {
  private final AtomicLong seed;

  public CasRandom(long seed) {
    this.seed = new AtomicLong(seed);
  }

  // Recipe from java.util.Random.next(int bits) documentation
  public int nextInt() {
    long oldSeed, newSeed;
    do {
      oldSeed = seed.get();
      newSeed = (oldSeed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
    } while (!seed.compareAndSet(oldSeed, newSeed));
    return (int)(newSeed >>> 16);
  }
}

class TLLockingRandom implements MyRandom {
  private final ThreadLocal<MyRandom> myRandomGenerator;

  public TLLockingRandom(final long seed) {
    this.myRandomGenerator =
      new ThreadLocal<MyRandom>() { public MyRandom initialValue() {
        return new LockingRandom(seed);
      }};
  }

  public int nextInt() {
    return myRandomGenerator.get().nextInt();
  }
}

class TLCasRandom implements MyRandom {
  private final ThreadLocal<MyRandom> myRandomGenerator;

  public TLCasRandom(final long seed) {
    this.myRandomGenerator =
      new ThreadLocal<MyRandom>() { public MyRandom initialValue() {
        return new CasRandom(seed);
      }};
  }

  public int nextInt() {
    return myRandomGenerator.get().nextInt();
  }
}

class WrappedTLRandom implements MyRandom {
  public int nextInt() {
    return ThreadLocalRandom.current().nextInt();
  }
}
