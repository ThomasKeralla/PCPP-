// For week 2
// sestoft@itu.dk * 2014-08-29

import java.util.concurrent.atomic.*;

class TestCountFactors {
    public static void main(String[] args) {
        final int range = 5_000_000;
        int count = 0;
        int threadCount = 10;
        AtomicInteger newCount = new AtomicInteger();

        count = countAndDivedeOnThreads(range, threadCount, newCount);
        System.out.printf("Total number of factors is %9d%n", count);
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


    private static int countAndDivedeOnThreads(int range, int threadCount, AtomicInteger myAtomic){
        final int perThread =  range / threadCount;

        Thread[] threads = new Thread[threadCount];
        for(int t = 0; t< threadCount; t++){
            final int from = perThread *t;
            final int to = (t+1==threadCount) ? range : perThread * (t+1);
            threads[t] = new Thread(() -> {
                for(int i=from; i<to;i++){

                    myAtomic.addAndGet(countFactors(i));
                }
            });
        }
        for(int l =0; l < threadCount; l++){
            threads[l].start();
        }
        try{
            for(int l =0; l < threadCount; l++){
                threads[l].join();
            }

        }catch (InterruptedException exn){}

        return myAtomic.get();
    }


    //Old class, now we are using AtomicInteger from java.util.concurrent.atomic
    static class MyAtomicInteger{
        private int integerValue;
        public  MyAtomicInteger(){
            integerValue = 0;
        }

        public int addAndGet(int amount){
            synchronized(this){
                integerValue += amount;
                return integerValue;
            }
        }
        public int get(){
            return integerValue;
        }

    }