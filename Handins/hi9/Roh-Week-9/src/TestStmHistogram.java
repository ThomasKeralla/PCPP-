// For week 10
// sestoft@itu.dk * 2014-11-05, 2015-10-14

// Compile and run like this under Linux and MacOS:
//   javac -cp ~/lib/multiverse-core-0.7.0.jar TestStmHistogram.java
//   java -cp ~/lib/multiverse-core-0.7.0.jar:. TestStmHistogram

// Compile and run like this under Windows -- note the SEMICOLON:
//   javac -cp multiverse-core-0.7.0.jar TestStmHistogram.java
//   java -cp multiverse-core-0.7.0.jar;. TestStmHistogram

// For the Multiverse library:
import org.multiverse.api.callables.TxnCallable;
import org.multiverse.api.references.*;
import static org.multiverse.api.StmUtils.*;

// Multiverse locking:
import org.multiverse.api.LockMode;
import org.multiverse.api.Txn;
import org.multiverse.api.callables.TxnVoidCallable;
import org.multiverse.api.callables.TxnIntCallable;

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
        final int range = 3_999_999;
        final int threadCount = 10;
        final int perThread = range / threadCount;

        final CyclicBarrier startBarrier = new CyclicBarrier(threadCount + 1);
        final CyclicBarrier stopBarrier = startBarrier;

        //Timer timer = new Timer();

        final Thread[] threads = new Thread[threadCount];

        for (int t = 0; t < threadCount; t++) {

            final int from = perThread * t;
            final int to = (t+1 == threadCount) ? range : perThread * (t+1);

            threads[t] = new Thread(() -> {

                try { startBarrier.await(); } catch (Exception exn) { }

                for (int p = from; p < to; p++) histogram.increment(countFactors(p));

                System.out.println(Thread.currentThread().getName());

                try { stopBarrier.await(); } catch (Exception exn) { }
            });

            threads[t].start();
        }

        try { startBarrier.await(); } catch (Exception exn) { }
        final long startTime = System.currentTimeMillis();
        try { stopBarrier.await(); } catch (Exception exn) { }
        final long endTime = System.currentTimeMillis();

        //System.out.println("Time: " + timer.check());
        System.out.println(startTime + " ms " + endTime + " ms " + (endTime - startTime) + " ms ");

        for (int i = 0; i < 200; i++) {
            try {
                total.transferBins(histogram);
                Thread.sleep(30);
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
