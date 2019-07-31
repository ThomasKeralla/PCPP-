// For week 2
// sestoft@itu.dk * 2014-09-04
import java.util.*;

class SimpleHistogram {
  public static void main(String[] args) {
    final Histogram histogram = new Histogram2(30);
    final int range = 5_000_000;
    Threads[] threads = Threads[histogram.getSpan();]
    //might not need this
    final int perthread = range / histogram.getSpan();
    //Make same number of threads as bins
    for(int i = 0; i < histogram.getSpan(); i++ ) {
      int threadStart = i*perthread;
      int threadStop = threadStart+perthread;
      Thread[i] = new Thread({() ->
        for(int j = threadStart; j<threadStop;j++) {
          int factors = countFactors(j);
          histogram.increment(factors);
        }
      });
      threads[i].start();
    }
    try {
      for(int t = 0; t<histogram.getSpan();t++) {
        threads[t].join();
      }
    }
    catch (InterruptedException exn) {System.out.println("Something happened while joining")}
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

  private static int countFactors(int p) {
           if (p < 2)
           {  return 0;}
           int factorCount = 1;
           int k = 2;
           while (p >= k * k) {
                 if (p % k == 0) {
                    factorCount++;
                    p /= k;
                 }else{
                  k++;
                 }
          }
      return factorCount;
  }

}

interface Histogram {
  public void increment(int bin);
  public int getCount(int bin);
  public int getSpan();
}

class Histogram1 implements Histogram {
  private int[] counts;
  public Histogram1(int span) {
    this.counts = new int[span];
  }
  public void increment(int bin) {
    counts[bin] = counts[bin] + 1;
  }
  public int getCount(int bin) {
    return counts[bin];
  }
  public int getSpan() {
    return counts.length;
  }
}

class Histogram2 implements Histogram {
  private final int[] counts;
  public Histogram1(int span) {
    this.counts = new int[span];
  }
  public synchronized void increment(int bin) {
    counts[bin] = counts[bin] + 1;
  }
  public synchronized int getCount(int bin) {
    return counts[bin];
  }
  public int getSpan() {
    return counts.length;
  }
}
