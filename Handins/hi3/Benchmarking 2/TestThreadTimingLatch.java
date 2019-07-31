// For week 4
// sestoft@itu.dk * 2014-09-08

import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class TestThreadTimingLatch {
  public static void main(String[] args) throws InterruptedException {
    final int range = 100_000;
    final Factorizer factorizer = new CachingFactorizer();
    System.out.printf("Time %10.3f s %n", timeTasks(10, () -> {
	  for (int i=2; i<range; i++) {
	    long[] result = factorizer.getFactors(i);
	  }
	}));
  }

  /**
   * Using CountDownLatch for starting and stopping threads in timing tests.
   * @author Brian Goetz and Tim Peierls; modifications by sestoft@itu.dk
   */
  
  public static double timeTasks(int threadCount, final Runnable task)
    throws InterruptedException {
    final CountDownLatch startGate = new CountDownLatch(1);
    final CountDownLatch endGate = new CountDownLatch(threadCount);
    for (int i = 0; i < threadCount; i++) {
      Thread t = new Thread(() -> {
	try {
	  startGate.await();
	  try { task.run(); } 
	  finally { endGate.countDown(); }
	} catch (InterruptedException ignored) { }
      });
      t.start();
    }

    Timer timer = new Timer();
    startGate.countDown();
    endGate.await();
    return timer.check();
  }
}

interface Factorizer {
  public long[] getFactors(long p);
  public long getCount();
}

// Like Goetz p. 31.
class CachingFactorizer implements Factorizer {
  private long lastNumber = 1;
  private long[] lastFactors = new long[0];

  public long[] getFactors(long p) {
    long[] factors = null;
    synchronized (this) {
      if (p == lastNumber)
	factors = lastFactors.clone();
    }
    if (factors == null) {
      factors = PrimeFactors.compute(p);
      synchronized (this) {
	lastNumber = p;
	lastFactors = factors.clone();
      }
    }
    return factors;
  }
  public long getCount() { return 0; }
}

class PrimeFactors {
  public static long[] compute(long p) {
    ArrayList<Long> factors = new ArrayList<Long>();
    long k = 2;
    while (p >= k * k) {
      if (p % k == 0) {
	factors.add(k);
	p /= k;
      } else 
	k++;
    }
    // Now k * k > p and no number in 2..k divides p
    factors.add(p);
    long[] result = new long[factors.size()];
    for (int i=0; i<result.length; i++) 
      result[i] = factors.get(i);
    return result;
  }

  public static boolean check(long p, long[] factors) {
    long prod = 1;
    for (int i=0; i<factors.length; i++)
      prod *= factors[i];
    return p == prod;
  }
}

// Crude wall clock timing utility, measuring time in seconds
   
class Timer {
  private long start, spent = 0;
  public Timer() { play(); }
  public double check() { return (System.nanoTime()-start+spent)/1e9; }
  public void pause() { spent += System.nanoTime()-start; }
  public void play() { start = System.nanoTime(); }
}
