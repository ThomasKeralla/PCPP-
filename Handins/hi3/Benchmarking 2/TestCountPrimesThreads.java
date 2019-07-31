// Week 4
// Counting primes, using multiple threads for better performance.
// (Much simplified from CountprimesMany.java)
// sestoft@itu.dk * 2014-08-31, 2015-09-15
// modified rikj@itu.dk 2017-09-20
import java.util.function.IntToDoubleFunction;
import java.util.concurrent.atomic.AtomicLong;

public class TestCountPrimesThreads {
  public static void main(String[] args) {
    SystemInfo();
    final int range = 100_000;
    // Mark6("countSequential", i -> countSequential(range));
    // Mark6("countParallel", i -> countParallelN(range, 10));
    Mark7("countSequential", i -> countSequential(range));
    for (int c=1; c<=32; c++) {
      final int threadCount = c;
      Mark7(String.format("countParallelN %6d", threadCount),
            i -> countParallelN(range, threadCount));
      //Mark7(String.format("countParallelNLocal %6d", threadCount),
      //      i -> countParallelNLocal(range, threadCount));
    }
  }

  private static boolean isPrime(int n) {
    int k = 2;
    while (k * k <= n && n % k != 0)
      k++;
    return n >= 2 && k * k > n;
  }

  // Sequential solution
  private static long countSequential(int range) {
    long count = 0;
    final int from = 0, to = range;
    for (int i=from; i<to; i++)
      if (isPrime(i))
        count++;
    return count;
  }

  // General parallel solution, using multiple threads
  private static long countParallelN(int range, int threadCount) {
    final int perThread = range / threadCount;
    final AtomicLong lc = new AtomicLong();
    Thread[] threads = new Thread[threadCount];
    for (int t=0; t<threadCount; t++) {
        final int from = perThread * t,
            to = (t+1==threadCount) ? range : perThread * (t+1);
        threads[t] = new Thread( () -> {
                long count = 0;
                for (int i=from; i<to; i++) {
                    if (isPrime(i)) {
                      count++;
                    }
                  }
                        lc.getAndAdd(count);
            });
    }
    for (int t=0; t<threadCount; t++)
      threads[t].start();
    try {
      for (int t=0; t<threadCount; t++)
        threads[t].join();
    } catch (InterruptedException exn) { }
    return lc.get();
  }

  // General parallel solution, using multiple threads
  private static long countParallelNLocal(int range, int threadCount) {
    final int perThread = range / threadCount;
    final long[] results = new long[threadCount];
    Thread[] threads = new Thread[threadCount];
    for (int t=0; t<threadCount; t++) {
      final int from = perThread * t,
        to = (t+1==threadCount) ? range : perThread * (t+1);
      final int threadNo = t;
      threads[t] = new Thread( ()-> {
        long count = 0;
        for (int i=from; i<to; i++)
          if (isPrime(i))
            count++;
        results[threadNo] = count;
      });
    }
    for (int t=0; t<threadCount; t++)
      threads[t].start();
    try {
      for (int t=0; t<threadCount; t++)
        threads[t].join();
    } catch (InterruptedException exn) { }
    long result = 0;
    for (int t=0; t<threadCount; t++)
      result += results[t];
    return result;
  }

  // --- Benchmarking infrastructure ---

  // NB: Modified to show microseconds instead of nanoseconds

  public static double Mark6(String msg, IntToDoubleFunction f) {
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
        double time = runningTime * 1e6 / count; // microseconds
        st += time;
        sst += time * time;
        totalCount += count;
      }
      double mean = st/n, sdev = Math.sqrt((sst - mean*mean*n)/(n-1));
      System.out.printf("%-25s %15.1f us %10.2f %10d%n", msg, mean, sdev, count);
    } while (runningTime < 0.25 && count < Integer.MAX_VALUE/2);
    return dummy / totalCount;
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
        double time = runningTime * 1e6 / count; // microseconds
        st += time;
        sst += time * time;
        totalCount += count;
      }
    } while (runningTime < 0.25 && count < Integer.MAX_VALUE/2);
    double mean = st/n, sdev = Math.sqrt((sst - mean*mean*n)/(n-1));
    System.out.printf("%-25s %15.1f us %10.2f %10d%n", msg, mean, sdev, count);
    return dummy / totalCount;
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
}

class LongCounter {
  private long count = 0;
  public synchronized void increment() {
    count = count + 1;
  }
  public synchronized long get() {
    return count;
  }
}
