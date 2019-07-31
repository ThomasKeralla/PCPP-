// For week 10 and 11
// sestoft@itu.dk * 2014-11-12, 2015-11-03

// Implementation of CAS in terms of lock, for describing its meaning,
// and implementation of AtomicInteger style operations in terms of
// CAS.

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntToDoubleFunction;

public class TestCasAtomicInteger {
  public static void main(String[] args) {
    // testMyAtomicInteger();
    timeSequentialGetAndAdd();
    timeParallelGetAndAdd();
  }

  private static void testMyAtomicInteger() {
    MyAtomicInteger mai = new MyAtomicInteger();
    System.out.println(mai.incrementAndGet());
    System.out.println(mai.incrementAndGet());
    System.out.println(mai.addAndGet(30));
    System.out.println(mai.get());
    System.out.println(mai.getAndSet(67));
    System.out.println(mai.get());
    System.out.println(mai.compareAndSet(67, 42));
    System.out.println(mai.get());
    System.out.println(mai.compareAndSet(67, 42));
    System.out.println(mai.get());
  }

  private static void timeSequentialGetAndAdd() {
    SystemInfo();
    final int count = 1_000_000;
    // This is so fast, 0.4 ns per iteration, that some kind of lock
    // coarsening or lock elision must be done in the JVM JIT.
    System.out.println(Mark7("MyAtomicInteger", (int i) -> { 
          final MyAtomicInteger ai = new MyAtomicInteger();
          int res = 0;
          for (int j=0; j<count; j++)
            res += ai.addAndGet(j);
          return res;
        }));
    System.out.println(Mark7("AtomicInteger", (int i) -> { 
          final AtomicInteger ai = new AtomicInteger();
          int res = 0;
          for (int j=0; j<count; j++)
            res += ai.addAndGet(j);
          return res;
        }));
  }

  private static void timeParallelGetAndAdd() {
    SystemInfo();
    final int count = 10_000_000, threadCount = 1;
    final CyclicBarrier startBarrier = new CyclicBarrier(threadCount + 1), 
      stopBarrier = startBarrier;
    {
      final MyAtomicInteger ai = new MyAtomicInteger();
      for (int t=0; t<threadCount; t++) {
        new Thread(() -> { 
	    try { startBarrier.await(); } catch (Exception exn) { }
	    for (int p=0; p<count; p++) 
	      ai.addAndGet(p);
	    try { stopBarrier.await(); } catch (Exception exn) { }
	}).start();
      }
      try { startBarrier.await(); } catch (Exception exn) { }
      Timer t = new Timer();
      try { stopBarrier.await(); } catch (Exception exn) { }
      double time = t.check() * 1e6;
      System.out.printf("MyAtomicInteger sum = %d; time = %10.2f us; per op = %6.1f ns %n", 
                        ai.get(), time, time * 1000.0 / count / threadCount);
    }
    {
      final AtomicInteger ai = new AtomicInteger();
      for (int t=0; t<threadCount; t++) {
        new Thread(() -> { 
	    try { startBarrier.await(); } catch (Exception exn) { }
	    for (int p=0; p<count; p++) 
	      ai.addAndGet(p);
	    try { stopBarrier.await(); } catch (Exception exn) { }
	}).start();
      }
      try { startBarrier.await(); } catch (Exception exn) { }
      Timer t = new Timer();
      try { stopBarrier.await(); } catch (Exception exn) { }
      double time = t.check() * 1e6;
      System.out.printf("AtomicInteger   sum = %d; time = %10.2f us; per op = %6.1f ns %n", 
                        ai.get(), time, time * 1000.0 / count / threadCount);
    }
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
        double time = runningTime * 1e9 / count;
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

// Model implementation of an AtomicInteger
class MyAtomicInteger {
  private int value;    // Visibility ensured by locking

  // Model implementation of compareAndSet to illustrate its meaning.
  // In reality, compareAndSet is not implemented using locks; the
  // opposite is usually the case.  
  public synchronized boolean compareAndSet(int oldValue, int newValue) {
    if (this.value == oldValue) {
      this.value = newValue;
      return true;
    } else
      return false;
  }

  public synchronized int get() { 
    return this.value;
  }

  public int addAndGet(int delta) {
    int oldValue, newValue;
    do {
      oldValue = get();
      newValue = oldValue + delta;
    } while (!compareAndSet(oldValue, newValue));
    return newValue;
  }

  public int getAndAdd(int delta) {
    int oldValue, newValue;
    do {
      oldValue = get();
      newValue = oldValue + delta;
    } while (!compareAndSet(oldValue, newValue));
    return oldValue;
  }

  public int incrementAndGet() {
    return addAndGet(1);
  }

  public int decrementAndGet() {
    return addAndGet(-1);
  }

  public int getAndSet(int newValue) {
    int oldValue;
    do { 
      oldValue = get();
    } while (!compareAndSet(oldValue, newValue));
    return oldValue;
  }
}

