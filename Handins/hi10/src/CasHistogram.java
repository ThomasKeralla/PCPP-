
import java.util.concurrent.atomic.AtomicInteger;


import java.util.concurrent.CyclicBarrier;

class CasTestHistogram {
  public static void main(String[] args) {
    countPrimeFactorsWithStmHistogram();
  }

  private static void countPrimeFactorsWithStmHistogram() {
    final Histogram histogram = new CasHistogram(30);
    final int range = 4_000_000;
    final int threadCount = 10, perThread = range / threadCount;
    final CyclicBarrier startBarrier = new CyclicBarrier(threadCount + 1),
      stopBarrier = startBarrier;
    final Thread[] threads = new Thread[threadCount];
    for (int t=0; t<threadCount; t++) {
      final int from = perThread * t,
                  to = (t+1 == threadCount) ? range : perThread * (t+1);
        threads[t] =
          new Thread(() -> {
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
    dump(histogram);
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






class CasHistogram implements Histogram {

private final AtomicInteger[] counts;

public CasHistogram(int span) {
    counts = new AtomicInteger[span];

    for (int i = 0; i < span; i++) {
        counts[i] = new AtomicInteger();
    }
}

public void increment(int bin) {
    int value;
    do {
        value = counts[bin].get();
    } while (!counts[bin].compareAndSet(value, value + 1)); {
        value = counts[bin].get();
    }
}

public int[] getBins() {
    int[] bins = new int[counts.length];

    for (int i = 0; i < counts.length; i++) {
        bins[i] = counts[i].get();
    }

    return bins;
}


public int getAndClear(int bin) {
    int value;
    do {
        value = counts[bin].get();
    } while (!counts[bin].compareAndSet(value, 0));
        return value;
}




public void transferBins(Histogram hist) {

    for (int i=0; i<counts.length; i++) {

        int value;

        do {
            value = counts[i].get();
        } while (!counts[i].compareAndSet(value, value + hist.getAndClear(i)));

      }
}

public int getCount(int bin) {
    return counts[bin].get();
}


public int getSpan() {
    return counts.length;
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
