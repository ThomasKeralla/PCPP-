// For PCPP

// sestoft@itu.dk * 2016-11-14

// Microbenchmarks for volatile and compareAndSet (CAS) speed.  On the
// Intel i7 they show that a volatile field access is 10x slower than
// a non-volatile one, that a CAS executed without interference from
// other threads accessing the same variable is almost as fast as a
// volatile, but that a CAS to a contended variable if roughly 4x
// slower.


import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntToDoubleFunction;

public class TestTimeCas {
  public static void main(String[] args) {
    SystemInfo();
    {
      final DataNonVolatile data = new DataNonVolatile();
      Mark7("nonvolatile",
	    i -> {
	      data.x = data.x + 1;
	      return data.x;
	    });
    }
    {
      final DataVolatile data = new DataVolatile();
      Mark7("volatile",
	    i -> {
	      data.x = data.x + 1;
	      return data.x;
	    });
    }
    {
      final AtomicInteger data = new AtomicInteger();
      Mark7("addAndGet alone",
	    i -> {
	      return data.addAndGet(1);
	    });      
    }
    {
      final AtomicInteger data = new AtomicInteger();
      Mark7("cas alone",
	    i -> {
	      int old = data.get();
	      data.compareAndSet(old, old+1);
	      return old;
	    });      
    }
    {
      final AtomicInteger data = new AtomicInteger();
      Thread annoyance =
	new Thread(() -> {
	    for (;;)
	      data.addAndGet(1);
	  });
      annoyance.start();
      Mark7("cas w interference",
	    i -> {
	      int old = data.get();
	      data.compareAndSet(old, old+1);
	      return old;
	    });
    }
    {
      final AtomicInteger data2 = new AtomicInteger();
      Mark7("cas no interference",
	    i -> {
	      int old = data2.get();
	      data2.compareAndSet(old, old+1);
	      return old;
	    });
    }
  }

  // --- Benchmarking infrastructure ---

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
        double time = runningTime * 1e9 / count; // nanoseconds
        st += time; 
        sst += time * time;
        totalCount += count;
      }
    } while (runningTime < 0.25 && count < Integer.MAX_VALUE/2);
    double mean = st/n, sdev = Math.sqrt((sst - mean*mean*n)/(n-1));
    System.out.printf("%-25s %15.1f ns %10.2f %10d%n", msg, mean, sdev, count);
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

class DataNonVolatile {
  public int x;
}

class DataVolatile {
  public volatile int x;
}

// # OS:   Mac OS X; 10.9.5; x86_64
// # JVM:  Oracle Corporation; 1.8.0_101
// # CPU:  null; 8 "cores"
// # Date: 2016-11-14T15:13:17+0100
// nonvolatile                           0.9 ns       0.01  268435456
// volatile                              8.8 ns       0.10   33554432
// addAndGet alone                      10.4 ns       0.04   33554432
// cas alone                            11.4 ns       0.09   33554432
// cas w interference                   74.5 ns       0.28    4194304
// cas no interference                  11.7 ns       0.02   33554432
