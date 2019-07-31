// For week 10
// sestoft@itu.dk * 2014-11-05, 2015-10-14

// Compile and run like this under Linux and MacOS:
//   javac -cp ~/lib/multiverse-core-0.7.0.jar TestStmHistogram.java
//   java -cp ~/lib/multiverse-core-0.7.0.jar:. TestStmHistogram

// Compile and run like this under Windows -- note the SEMICOLON:
//   javac -cp multiverse-core-0.7.0.jar TestStmHistogram.java
//   java -cp multiverse-core-0.7.0.jar;. TestStmHistogram

// For the Multiverse library:
import org.multiverse.api.references.*;
import static org.multiverse.api.StmUtils.*;

// Multiverse locking:
import org.multiverse.api.LockMode;
import org.multiverse.api.Txn;
import org.multiverse.api.callables.TxnVoidCallable;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CyclicBarrier;

class TestStmHistogram {
  public static void main(String[] args) {
    countPrimeFactorsWithStmHistogram();
  }

  private static void countPrimeFactorsWithStmHistogram() {
    final Histogram histogram = new StmHistogram(30);
    final Histogram total = new StmHistogram(30);
    final int range = 4_000_000;
    final int threadCount = 10, perThread = range / threadCount;
    final CyclicBarrier startBarrier = new CyclicBarrier(threadCount + 1),
      stopBarrier = startBarrier;
    final Thread[] threads = new Thread[threadCount];
    for (int t=0; t<threadCount; t++) {
      final int from = perThread * t, to = (t+1 == threadCount) ? range : perThread * (t+1);
        threads[t] = new Thread(() -> {
	      try { startBarrier.await(); } catch (Exception exn) { }
	      for (int p=from; p<to; p++)
		histogram.increment(countFactors(p));
	      System.out.print("*");
	      try { stopBarrier.await(); } catch (Exception exn) { }
	    });

        threads[t].start();
    }

    try { startBarrier.await(); } catch (Exception exn) { }
    try { stopBarrier.await(); } catch (Exception exn) { }


    for (int i = 0; i < 200; i++) {
        try {
            total.transferBins(histogram);
            Thread.sleep(30);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    System.out.println("Histogram dump");
    dump(histogram);
    System.out.println("Total Histogram dump");
    dump(total);

  }

  public static void dump(Histogram histogram) {
    int totalCount = 0;
    for (int bin=0; bin<histogram.getSpan(); bin++) {
      System.out.printf("%4d: %9d%n", bin, histogram.getCount(bin));
      totalCount += histogram.getCount(bin);
    }
    System.out.printf("      %9d%n", totalCount);
  }

  public static int countFactors(int p) {
    if (p < 2)
      return 0;
    int factorCount = 1, k = 2;
    while (p >= k * k) {
      if (p % k == 0) {
        factorCount++;
        p /= k;
      } else
        k++;
    }
    return factorCount;
  }
}

interface Histogram {
  void increment(int bin);
  int getCount(int bin);
  int getSpan();
  int[] getBins();
  int getAndClear(int bin);
  void transferBins(Histogram hist);
}

class StmHistogram implements Histogram {
  private final TxnInteger[] counts;

  public StmHistogram(int span) {
      counts = new TxnInteger[span];

      for (int i = 0; i < span; i++)
          counts[i] = newTxnInteger(0);
  }

  public void increment(int bin) {
      counts[bin].atomicIncrementAndGet(1);
  }

  public int getCount(int bin) {
     return counts[bin].atomicGet();
  }

  public int getSpan() {
    return counts.length;
  }

  public int[] getBins() {
      int[] bins = new int[counts.length];
      for (int i = 0; i < counts.length; i++)
          bins[i] = counts[i].atomicGet();

      return bins;
  }

  public int getAndClear(int bin) {
      return counts[bin].atomicGetAndSet(0);
  }

  public void transferBins(Histogram hist) {
      for (int i=0; i<hist.getSpan(); i++) {
        final int n = i;
          atomic(() -> counts[n].increment(hist.getAndClear(n)));
      }
  }
}
