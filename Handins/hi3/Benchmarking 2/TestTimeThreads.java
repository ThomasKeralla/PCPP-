// For week 4

// sestoft@itu.dk * 2014-09-10, 2015-09-15

// Microbenchmarks for small object creation, Thread creation, thread
// start, thread execution and join, taking an uncontended lock.

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntToDoubleFunction;

public class TestTimeThreads {
  public static void main(String[] args) {
    SystemInfo();
    final Point myPoint = new Point(42, 39);
    Mark7("hashCode()", i -> myPoint.hashCode());
    Mark7("Point creation",
          i -> {
            Point p = new Point(i, i);
            return p.hashCode();
          });
    final AtomicInteger ai = new AtomicInteger();
    Mark7("Thread's work",
          i -> {
            for (int j=0; j<1000; j++)
              ai.getAndIncrement();
            return ai.doubleValue();
          });
    Mark7("Thread create",
          i -> {
            Thread t = new Thread(() -> {
                for (int j=0; j<1000; j++)
                  ai.getAndIncrement();
              });
            return t.hashCode();
          });
    Mark7("Thread create start",
          i -> {
            Thread t = new Thread(() -> {
              for (int j=0; j<1000; j++)
                ai.getAndIncrement();
            });
            t.start();
            return t.hashCode();
          });
    Mark7("Thread create start join",
          i -> {
            Thread t = new Thread(() -> {
              for (int j=0; j<1000; j++)
                ai.getAndIncrement();
              });
            t.start();
            try { t.join(); }
            catch (InterruptedException exn) { }
            return t.hashCode();
          });
    System.out.printf("ai value = %d%n", ai.intValue());
    final Object obj = new Object();
    Mark7("Uncontended lock",
          i -> {
            synchronized (obj) {
              return i;
            }
          });
  }

  // --- Benchmarking infrastructure ---

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
        double time = runningTime * 1e9 / count; // nanoseconds
        st += time;
        sst += time * time;
        totalCount += count;
      }
      double mean = st/n, sdev = Math.sqrt((sst - mean*mean*n)/(n-1));
      System.out.printf("%-25s %15.1f ns %10.2f %10d%n", msg, mean, sdev, count);
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

/**
 * Immutable Point class used by DelegatingVehicleTracker
 * @author Brian Goetz and Tim Peierls
 */
class Point {
  public final int x, y;
  public Point(int x, int y) {
    this.x = x; this.y = y;
  }
}
