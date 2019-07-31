// For week 7
// sestoft@itu.dk * 2014-10-22, 2016-10-12

// It is possible to obtain a modest speedup (2x or so) by using 16 or
// 32 AtomicLongs instead of one, and index by thread hashcode.  But
// the built-in LongAdder is 6-30 x faster still.  The overhead for
// getting the thread's hashcode is 3 ns and that may overshadow any
// real benefits, but more likely there are other problems, such as
// false sharing in the cache, which is carefully avoided in the real
// LongAdder implementation.

// The false sharing can be removed to some extent on the 4 core (x 2
// hyperthreading) Intel i7 using the bizarre allocation idea shown
// in NewLongAdderPadded, but the effect of this on the 32 core AMD
// Opteron is modest.  In any case, technology-dependent allocation
// code such as that in NewLongAdderPadded should be hidden in standard
// libraries where it can be updated by experts as technology and JVM
// implementation techniques evolve. 

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntToDoubleFunction;


interface Adder {
  void add(long delta);
  long longValue();
}

public class TestLongAdders {
  private static final int threadCount = 32, iterations = 100_000;
    
  public static void main(String[] args) {
    SystemInfo();
    // Mark7("current thread hashCode", 
    //       i -> Thread.currentThread().hashCode());
    // Mark7("ThreadLocalRandom", 
    //       i -> ThreadLocalRandom.current().nextInt());
    Mark7("AtomicLong", 
          i -> exerciseAtomicLong());
    Mark7("LongCounter",
          i -> exerciseLongCounter());
    Mark7("LongCounter",
          i -> exerciseGeneric(new LongCounter()));
    Mark7("NewLongAdderArray", 
          i -> exerciseNewLongAdderArray());
    Mark7("NewLongAdder", 
          i -> exerciseNewLongAdder());
    Mark7("NewLongAdderPadded", 
          i -> exerciseNewLongAdderPadded());
    Mark7("NewLongAdderPaddedR", 
          i -> exerciseNewLongAdderPaddedR());
    Mark7("NewLongAdderPaddedR", 
          i -> exerciseGenericA(NewLongAdderPaddedR::new ));
    Mark7("NewLongAdderPaddedR", 
          i -> exerciseGeneric(new NewLongAdderPaddedR() ));
    Mark7("LongAdder", 
          i -> exerciseLongAdder());
  }

  private static double exerciseGeneric( final Adder adder) {
    Thread[] threads = new Thread[threadCount];
    for (int t=0; t<threadCount; t++) {
      final int myThread = t;
      threads[t] = new Thread(() -> {
        for (int i=0; i<iterations; i++) 
          adder.add(i);
      });
    }
    for (int t=0; t<threadCount; t++) 
      threads[t].start();
    try {
      for (int t=0; t<threadCount; t++) 
        threads[t].join();
    } catch (InterruptedException exn) { }
    return adder.longValue();
  }
  
  private static double exerciseGenericA (java.util.function.Supplier<Adder> f) {
    final Adder adder = f.get();
    Thread[] threads = new Thread[threadCount];
    for (int t=0; t<threadCount; t++) {
      final int myThread = t;
      threads[t] = new Thread(() -> {
        for (int i=0; i<iterations; i++) 
          adder.add(i);
      });
    }
    for (int t=0; t<threadCount; t++) 
      threads[t].start();
    try {
      for (int t=0; t<threadCount; t++) 
        threads[t].join();
    } catch (InterruptedException exn) { }
    return adder.longValue();
  }

    
  // Timing of Java's AtomicLong
  private static double exerciseAtomicLong() {
    final AtomicLong adder = new AtomicLong();
    Thread[] threads = new Thread[threadCount];
    for (int t=0; t<threadCount; t++) {
      final int myThread = t;
      threads[t] = new Thread(() -> {
        for (int i=0; i<iterations; i++) 
          adder.getAndAdd(i);
      });
    }
    for (int t=0; t<threadCount; t++) 
      threads[t].start();
    try {
      for (int t=0; t<threadCount; t++) 
        threads[t].join();
    } catch (InterruptedException exn) { }
    return adder.longValue();
  }

  // Timing of a simple long with synchronized add and get methods
  private static double exerciseLongCounter() {
    final LongCounter adder = new LongCounter();
    Thread[] threads = new Thread[threadCount];
    for (int t=0; t<threadCount; t++) {
      final int myThread = t;
      threads[t] = new Thread(() -> {
        for (int i=0; i<iterations; i++) 
          adder.add(i);
      });
    }
    for (int t=0; t<threadCount; t++) 
      threads[t].start();
    try {
      for (int t=0; t<threadCount; t++) 
        threads[t].join();
    } catch (InterruptedException exn) { }
    return adder.longValue();
  }

  // Timing of a striped long, with dense allocation of stripes
  private static double exerciseNewLongAdderArray() {
    final NewLongAdderArray adder = new NewLongAdderArray();
    Thread[] threads = new Thread[threadCount];
    for (int t=0; t<threadCount; t++) {
      final int myThread = t;
      threads[t] = new Thread(() -> {
        for (int i=0; i<iterations; i++) 
          adder.add(i);
      });
    }
    for (int t=0; t<threadCount; t++) 
      threads[t].start();
    try {
      for (int t=0; t<threadCount; t++) 
        threads[t].join();
    } catch (InterruptedException exn) { }
    return adder.longValue();
  }

  // Timing of a striped long, with less dense allocation of stripes
  private static double exerciseNewLongAdder() {
    final NewLongAdder adder = new NewLongAdder();
    Thread[] threads = new Thread[threadCount];
    for (int t=0; t<threadCount; t++) {
      final int myThread = t;
      threads[t] = new Thread(() -> {
        for (int i=0; i<iterations; i++) 
          adder.add(i);
      });
    }
    for (int t=0; t<threadCount; t++) 
      threads[t].start();
    try {
      for (int t=0; t<threadCount; t++) 
        threads[t].join();
    } catch (InterruptedException exn) { }
    return adder.longValue();
  }

  // Timing of a striped long, with attempted scattered allocation of stripes
  private static double exerciseNewLongAdderPadded() {
    final NewLongAdderPadded adder = new NewLongAdderPadded();
    Thread[] threads = new Thread[threadCount];
    for (int t=0; t<threadCount; t++) {
      final int myThread = t;
      threads[t] = new Thread(() -> {
        for (int i=0; i<iterations; i++) 
          adder.add(i);
      });
    }
    for (int t=0; t<threadCount; t++) 
      threads[t].start();
    try {
      for (int t=0; t<threadCount; t++) 
        threads[t].join();
    } catch (InterruptedException exn) { }
    return adder.longValue();
  }
  // Timing of a striped long, with attempted scattered allocation of stripes / bigger array
  private static double exerciseNewLongAdderPaddedR() {
    final NewLongAdderPaddedR adder = new NewLongAdderPaddedR();
    Thread[] threads = new Thread[threadCount];
    for (int t=0; t<threadCount; t++) {
      final int myThread = t;
      threads[t] = new Thread(() -> {
        for (int i=0; i<iterations; i++) 
          adder.add(i);
      });
    }
    for (int t=0; t<threadCount; t++) 
      threads[t].start();
    try {
      for (int t=0; t<threadCount; t++) 
        threads[t].join();
    } catch (InterruptedException exn) { }
    return adder.longValue();
  }

  // Timing of Java 8's built-in LongAdder, supposedly highly scalable
  private static double exerciseLongAdder() {
    final LongAdder adder = new LongAdder();
    Thread[] threads = new Thread[threadCount];
    for (int t=0; t<threadCount; t++) {
      final int myThread = t;
      threads[t] = new Thread(() -> {
        for (int i=0; i<iterations; i++) 
          adder.add(i);
      });
    }
    for (int t=0; t<threadCount; t++) 
      threads[t].start();
    try {
      for (int t=0; t<threadCount; t++) 
        threads[t].join();
    } catch (InterruptedException exn) { }
    return adder.longValue();
  }
  
  // --- Benchmarking infrastructure ---

  private static class Timer {
    private long start, spent = 0;
    public Timer() { play(); }
    public double check() { return (System.nanoTime()-start+spent)/1e9; }
    public void pause() { spent += System.nanoTime()-start; }
    public void play() { start = System.nanoTime(); }
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

// ----------------------------------------------------------------------

// An atomic long that consists of a single private long field and
// synchronized methods, following Java monitor pattern.

class LongCounter implements Adder {
  private long count = 0;
  public synchronized void add(long delta) {
    count += delta;
  }
  public synchronized long longValue() { 
    return count; 
  }
}

// ----------------------------------------------------------------------

// An atomic long that is composed of NSTRIPES AtomicLongs stored next
// to each other in an array.  Probably not a good idea except deep in
// the Java class libraries.  In any case, presumably a thread
// hashcode could be negative, so should use 
// (Thread.currentThread().hashCode() & 0x7FFFFFFF) % NSTRIPES.

class NewLongAdderArray implements Adder {
  private final static int NSTRIPES = 31;
  private final AtomicLongArray counters = new AtomicLongArray(NSTRIPES);

  public void add(long delta) {
    counters.addAndGet(Thread.currentThread().hashCode() % NSTRIPES, delta);
  }

  public long longValue() {
    long result = 0;
    for (int stripe=0; stripe<NSTRIPES; stripe++)
      result += counters.get(stripe);
    return result;
  }
}

// ----------------------------------------------------------------------

// An atomic long that is composed of NSTRIPES AtomicLongs
// (presumably) more scattered in the heap than the elements of an
// AtomicLongArray.  Inspired by the innards of Java 8's LongAdder.

class NewLongAdder implements Adder {
  private final static int NSTRIPES = 31;
  private final AtomicLong[] counters;

  public NewLongAdder() {
    this.counters = new AtomicLong[NSTRIPES];
    for (int stripe=0; stripe<NSTRIPES; stripe++) {
      counters[stripe] = new AtomicLong();
    }
  }

  public void add(long delta) {
    counters[Thread.currentThread().hashCode() % NSTRIPES].addAndGet(delta);
  }

  public long longValue() {
    long result = 0;
    for (int stripe=0; stripe<NSTRIPES; stripe++)
      result += counters[stripe].get();
    return result;
  }
}

// ----------------------------------------------------------------------

// An atomic long that is composed of NSTRIPES AtomicLongs
// (presumably) scattered in the heap because of the seemingly useless
// Object allocations.  Inspired by the innards of Java 8's LongAdder.

class NewLongAdderPadded implements Adder {
  private final static int NSTRIPES = 31;
  private final AtomicLong[] counters;

  public NewLongAdderPadded() {
    this.counters = new AtomicLong[NSTRIPES];
    for (int stripe=0; stripe<NSTRIPES; stripe++) {
      // Believe it or not, this sometimes speeds up the code,
      // presumably because it avoids false sharing of cache lines:
      new Object(); new Object(); new Object(); new Object(); new Object(); new Object(); new Object();
      counters[stripe] = new AtomicLong();
    }
  }

  public void add(long delta) {
    counters[Thread.currentThread().hashCode() % NSTRIPES].addAndGet(delta);
  }

  public long longValue() {
    long result = 0;
    for (int stripe=0; stripe<NSTRIPES; stripe++)
      result += counters[stripe].get();
    return result;
  }
}
class NewLongAdderPaddedR implements Adder {
  private final static int NSTRIPES = 31;
  private final static int PADDING = 11;
  private final static int NSIZE = NSTRIPES*PADDING;
  //private final AtomicLong[] counters;
    
  private final AtomicLongArray counters = new AtomicLongArray(NSIZE);

  // public NewLongAdderPaddedR() {
  //   this.counters = new AtomicLong[NSIZE];
  //   for (int stripe=0; stripe<NSIZE; stripe++) {
  //     // Believe it or not, this sometimes speeds up the code,
  //     // presumably because it avoids false sharing of cache lines:
  //       //      new Object(); new Object(); new Object(); new Object(); new Object(); new Object(); new Object();
  //     counters[stripe] = new AtomicLong();
  //   }
  // }

  public void add(long delta) {
      counters.addAndGet(PADDING*(Thread.currentThread().hashCode() % NSTRIPES), delta);
      //      counters[PADDING*(Thread.currentThread().hashCode() % NSTRIPES)].addAndGet(delta);
  }

  public long longValue() {
    long result = 0;
    for (int stripe=0; stripe<NSTRIPES; stripe++)
        result += counters.get(PADDING*stripe);
    //      result += counters[PADDING*stripe].get();
    return result;
  }
}

// MacOS i7 4 core x hyperthreading
// # OS:   Mac OS X; 10.9.5; x86_64
// # JVM:  Oracle Corporation; 1.8.0_101
// # CPU:  null; 8 "cores"
// # Date: 2016-10-10T14:58:46+0200
// AtomicLong                       974126.2 us   93929.75          2
// LongCounter                      498849.2 us  267251.31          2
// NewLongAdderArray                421806.2 us   41099.06          2
// NewLongAdder                     183443.1 us   17010.34          2
// NewLongAdderPadded               114280.8 us   18110.60          2
// LongAdder                         64305.0 us     348.26          4


// P3 Windows server Xeon E5-2680 v3 @ 2.50 Ghz: 2 chips x 12 cores x hyperthreading
// # OS:   Windows 10; 10.0; amd64
// # JVM:  Oracle Corporation; 1.8.0_102
// # CPU:  Intel64 Family 6 Model 63 Stepping 2, GenuineIntel; 48 "cores"
// # Date: 2016-10-10T15:11:57+0200
// AtomicLong                       666823.6 us   19030.19          2
// LongCounter                      814858.5 us  496968.58          2
// NewLongAdderArray                956406.5 us  282949.16          2
// NewLongAdder                     295820.5 us   52748.46          2
// NewLongAdderPadded               134781.7 us   34264.59          4
// LongAdder                         22487.4 us    2275.96         16


