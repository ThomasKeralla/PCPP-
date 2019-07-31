import org.multiverse.api.references.TxnInteger;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import static org.multiverse.api.StmUtils.atomic;
import static org.multiverse.api.StmUtils.newTxnInteger;

/**
 * Project:  Week-10
 * Package:  PACKAGE_NAME
 * Date:     17-11-2017
 * Time:     19:05
 * Author:   Johnni Hested
 */

public class TestCasHistogram {
    public static void main(String[] args) {
        countPrimeFactorsWithStmHistogram();
    }


    private static void countPrimeFactorsWithStmHistogram() {

        // Use CasHistogram from week 10
        final Histogram histogram = new CasHistogram(30);
        final Histogram total = new CasHistogram(30);

        // Use StmHistogram from week 9
        //final Histogram histogram = new StmHistogram(30);
        //final Histogram total = new StmHistogram(30);

        // Use Histogram2 from week 2
        //final Histogram histogram = new Histogram2(30);
        //final Histogram total = new Histogram2(30);

        final int range = 4_000_000;
        final int threadCount = 10;
        final int perThread = range / threadCount;

        final CyclicBarrier barrier = new CyclicBarrier(threadCount + 1);

        Timer timer = new Timer();

        final Thread[] threads = new Thread[threadCount];

        for (int t = 0; t < threadCount; t++) {

            final int from = perThread * t;
            final int to = (t+1 == threadCount) ? range : perThread * (t+1);

            threads[t] = new Thread(() -> {
                try { barrier.await(); } catch (Exception exn) { }

                for (int p = from; p < to; p++) histogram.increment(countFactors(p));

                System.out.println(Thread.currentThread().getName() + " complete at time: " + timer.check());

                try { barrier.await(); } catch (Exception exn) { }
            });

            threads[t].start();
        }

        try { barrier.await(); } catch (Exception exn) { }
        try { barrier.await(); } catch (Exception exn) { }

        System.out.println("Total Time: " + timer.check());

        for (int i = 0; i < 50; i++) {
            try {
                total.transferBins(histogram);
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }



        System.out.println("----------------");
        System.out.println("Histogram dump");
        dump(histogram);
        System.out.println("----------------");
        System.out.println("Total dump");
        dump(total);
    }

    public static void dump(Histogram histogram) {
        int totalCount = 0;
        for (int bin = 0; bin < histogram.getSpan(); bin++) {
            System.out.printf("%4d: %9d%n", bin, histogram.getCount(bin));
            totalCount += histogram.getCount(bin);
        }
        System.out.printf("      %9d%n", totalCount);
    }

    public static int countFactors(int p) {
        if (p < 2) return 0;

        int factorCount = 1;
        int k = 2;

        while (p >= k * k) {

            if (p % k == 0) {
                factorCount++;
                p /= k;
            } else k++;
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
        } while (!counts[bin].compareAndSet(value, value + 1));
    }


    public int getCount(int bin) {
        return counts[bin].get();
    }


    public int getSpan() {
        return counts.length;
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

            //int value = counts[i].get();
            //counts[i].compareAndSet(value, value + hist.getAndClear(i));

            int value;
            do {
                value = counts[i].get();
            } while (!counts[i].compareAndSet(value, value + hist.getAndClear(i)));
        }
    }
}

class StmHistogram implements Histogram {
    private final TxnInteger[] counts;

    public StmHistogram(int span) {
        counts = new TxnInteger[span];

        for (int i = 0; i < span; i++) {
            counts[i] = newTxnInteger(0);
        }
    }

    public void increment(int bin) {
        //System.out.println("increment called");
        counts[bin].atomicIncrementAndGet(1);
    }

    public int getCount(int bin) {
        //System.out.println("getCount called");
        return counts[bin].atomicGet();
    }

    public int getSpan() {
        //System.out.println("getSpan called");
        return counts.length;
    }

    public int[] getBins() {
        //System.out.println("getBins called");
        int[] bins = new int[counts.length];

        for (int i = 0; i < counts.length; i++) {
            bins[i] = counts[i].atomicGet();
        }

        return bins;
    }

    public int getAndClear(int bin) {
        //System.out.println("getAndClear called");
        return counts[bin].atomicGetAndSet(0);
    }

    public void transferBins(Histogram hist) {
        //System.out.println("transferBins called");
        for (int i=0; i<counts.length; i++) {
            int ii = i;
            atomic(() -> counts[ii].increment(hist.getAndClear(ii)));
        }
    }
}

class Histogram2 implements Histogram {
    private int[] counts;

    public Histogram2(int span) {
        this.counts = new int[span];
    }

    public void increment(int bin) {
        synchronized(this) {
            counts[bin] = counts[bin] + 1;
        }
    }

    public int getCount(int bin) {
        synchronized(this) {
            return counts[bin];
        }
    }

    public final int getSpan() {
        return counts.length;
    }

    public int[] getBins() {
        synchronized(this) {
            int[] arr = new int[counts.length];

            for(int i=0; i<counts.length; i++) {
                arr[i] = counts[i];
            }

            return arr;
        }
    }

    public int getAndClear(int bin) {
        return 0;
    }

    public void transferBins(Histogram hist) {

    }
}
