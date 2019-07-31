// For week 12
// sestoft@itu.dk * 2015-11-05

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public class TestChaseLevQueue {
  final static int size = 100_000_000; // Number of integers to sort

  public static void main(String[] args) {
    multiQueueMultiThreadCL(8);
  }

  // ----------------------------------------------------------------------
  // Version E: Multi-queue multi-thread setup SOLUTION, thread-local queues

  @SuppressWarnings("unchecked") 
  private static void multiQueueMultiThreadCL(final int threadCount) {
    // Java's @$#@?!! type system requires this unsafe cast 
    ChaseLevDeque<SortTask>[] queues 
      = (ChaseLevDeque<SortTask>[])(new ChaseLevDeque[threadCount]);
    for (int t=0; t<threadCount; t++) 
      queues[t] = new ChaseLevDeque<SortTask>(100000);
    int[] arr = IntArrayUtil.randomIntArray(size);
    queues[0].push(new SortTask(arr, 0, arr.length-1));
    Timer t = new Timer();
    mqmtWorkers(queues, threadCount);
    System.out.printf("multiQueueMultiThreadCL %3d %15.3f s ", threadCount, t.check());
    System.out.println(IntArrayUtil.isSorted(arr));
    // IntArrayUtil.printout(arr, 100);
  }

  private static void mqmtWorkers(Deque<SortTask>[] queues, int threadCount) {
    final Thread[] threads = new Thread[threadCount];
    final LongAdder ongoing = new LongAdder();
    ongoing.add(1);
    for (int t=0; t<threadCount; t++) {
      final int myNumber = t;
      threads[t] = new Thread(() -> {
        SortTask task;
        while (null != (task = getTask(myNumber, queues, ongoing))) {
          final int[] arr = task.arr;
          final int a = task.a, b = task.b;
          if (a < b) { 
            int i = a, j = b;
            int x = arr[(i+j) / 2];         
            do {                            
              while (arr[i] < x) i++;       
              while (arr[j] > x) j--;       
              if (i <= j) {
                swap(arr, i, j);
                i++; j--;
              }                             
            } while (i <= j); 
            ongoing.add(2);
            queues[myNumber].push(new SortTask(arr, a, j)); 
            queues[myNumber].push(new SortTask(arr, i, b));               
          }
          ongoing.decrement();
        }
      });
    } 
    for (int t=0; t<threadCount; t++) 
      threads[t].start();
    try {
      for (int t=0; t<threadCount; t++) 
        threads[t].join();
    } catch (InterruptedException exn) { }
  }

  // Swap arr[s] and arr[t]
  private static void swap(int[] arr, int s, int t) {
    int tmp = arr[s];  arr[s] = arr[t];  arr[t] = tmp;
  }

  // Tries to get a sorting task.  If task queue is empty, repeatedly
  // try to steal, cyclically, from other threads and if that fails
  // wait a moment, while some tasks are computing.

  private static SortTask getTask(final int myNumber, final Deque<SortTask>[] queues, 
                                  LongAdder ongoing) {
    final int threadCount = queues.length;
    SortTask task = queues[myNumber].pop();
    if (null != task) 
      return task;
    else {
      do {
        for (int t=0; t<threadCount-1; t++) 
          if (null != (task = queues[(myNumber+t) % threadCount].steal())) 
            return task;
        Thread.yield();
      } while (ongoing.longValue() > 0);
      return null;
    }
  }
}

// ----------------------------------------------------------------------
// SortTask class, Deque<T> interface, SimpleDeque<T> 

// Represents the task of sorting arr[a..b]
class SortTask {
  public final int[] arr;
  public final int a, b;

  public SortTask(int[] arr, int a, int b) {
    this.arr = arr; 
    this.a = a;
    this.b = b;
  }
}

interface Deque<T> {
  void push(T item);    // at bottom
  T pop();              // from bottom
  T steal();            // from top
}

// ----------------------------------------------------------------------

// A lock-free queue simplified from Chase and Lev: Dynamic circular
// work-stealing queue, SPAA 2005.  We simplify it by not reallocating
// the array; hence this queue may overflow.  This is close in spirit
// to the original ABP work-stealing queue (Arora, Blumofe, Plaxton:
// Thread scheduling for multiprogrammed multiprocessors, 2000,
// section 3) but in that paper an "age" tag needs to be added to the
// top pointer to avoid the ABA problem (see ABP section 3.3).  This
// is not necessary in the Chase-Lev dequeue design, where the top
// index never assumes the same value twice.

class ChaseLevDeque<T> implements Deque<T> {
  // Invariants and meaning of fields is the same as in the SimpleDeque.
  private volatile long bottom = 0;
  private final AtomicLong top = new AtomicLong();
  private final T[] items;

  public ChaseLevDeque(int size) {
    this.items = makeArray(size);
  }

  @SuppressWarnings("unchecked") 
  private static <T> T[] makeArray(int size) {
    // Java's @$#@?!! type system requires this unsafe cast    
    return (T[])new Object[size];
  }

  private static int index(long i, int n) {
    return (int)(i % (long)n);
  }

  public void push(T item) { // at bottom
    final long b = bottom, t = top.get(), size = b - t;
    if (size == items.length) 
      throw new RuntimeException("queue overflow");
    items[index(b, items.length)] = item;
    bottom = b+1;
  }

  public T pop() { // from bottom
    final long b = bottom - 1;
    bottom = b;
    final long t = top.get(), afterSize = b - t;
    if (afterSize < 0) { // empty before call
      bottom = t;
      return null;
    } else {
      T result = items[index(b, items.length)];
      if (afterSize > 0) // non-empty after call
        return result;
      else {             // became empty, update both top and bottom
        if (!top.compareAndSet(t, t+1)) // somebody stole result
          result = null;
        bottom = t+1;
        return result;
      }
    }
  }

  public T steal() { // from top
    final long t = top.get(), b = bottom, size = b - t;
    if (size <= 0)
      return null;
    else {
      T result = items[index(t, items.length)];
      if (top.compareAndSet(t, t+1))
        return result;
      else 
        return null;
    }
  }
}

// ----------------------------------------------------------------------

class IntArrayUtil {
  public static int[] randomIntArray(final int n) {
    int[] arr = new int[n];
    for (int i = 0; i < n; i++)
      arr[i] = (int) (Math.random() * n * 2);
    return arr;
  }

  public static void printout(final int[] arr, final int n) {
    for (int i=0; i < n; i++)
      System.out.print(arr[i] + " ");
    System.out.println("");
  }

  public static boolean isSorted(final int[] arr) {
    for (int i=1; i<arr.length; i++)
      if (arr[i-1] > arr[i])
        return false;
    return true;
  }
}
