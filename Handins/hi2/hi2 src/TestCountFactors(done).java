// For week 2
// sestoft@itu.dk * 2014-08-29
import java.util.concurrent.atomic.*;

class TestCountFactors {
  public static void main(String[] args) {
    final int range = 5_000_000;
    int count = 0;
    final int threadCount = 10;
    AtomicInteger newCount = new AtomicInteger();

      count += countDividedOnThreads(range, threadCount, newCount);

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

  private static int countDividedOnThreads(int range, int threadCount, AtomicInteger atomicCount) {
    final int perthread = range / threadCount;

    Thread[] threads = new Thread[threadCount];

    for(int i = 0; i<threadCount; i++) {
      final int from = perthread*i;
      final int to = from + perthread;
      threads[i] = new Thread(() -> {
        for(int j = from; j < to; j++) {
          atomicCount.addAndGet(countFactors(j));
        }
      } );
    }
    for(int t = 0; t < threadCount; t++) {
      threads[t].start();
    }
    try {
      for(int k = 0; k < threadCount; k++) {
        threads[k].join();
      }
    }
    catch (InterruptedException exn) {}
    return atomicCount.get();
  }
}

class MyAtomicInt {
  private volatile int intValue;

  public MyAtomicInt() {
    intValue = 0;
  }
  public int addAndGet (int x) {
    //synchronized(this) {
      intValue += x;
    //}
    return intValue;
  }

  public int get(){
    //synchronized(this) {
         return intValue;
    //}
  }
}
