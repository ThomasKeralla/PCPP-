// For week 12
// sestoft@itu.dk * 2014-11-16

// Unbounded list-based lock-free queue by Michael and Scott 1996 (who
// call it non-blocking).

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.lang.NullPointerException;

public class TestMSQueue {
  public static void main(String[] args) {
    //sequientialTest();
    concurrentTest();
}

    public static void sequientialTest() {
      MSQueue queue = new MSQueue();

      assert queue.dequeue() == null;
      System.out.println("dequeue: "+ queue.dequeue()); //null
      queue.enqueue(12);
      //assert queue.dequeue() == (Integer) 12;
      System.out.println("dequeue: "+queue.dequeue()); //12
      queue.enqueue(3);
      queue.enqueue(6);
      queue.enqueue(12);
      queue.enqueue(24);
      assert queue.dequeue() == (Integer) 3; //True
      //System.out.println("dequeue: "+queue.dequeue()); //6
      assert queue.dequeue() == (Integer) 6; //True
      assert queue.dequeue() == (Integer) 12; //True
      assert queue.dequeue() == (Integer) 24; //True
      //System.out.println("dequeue: "+queue.dequeue()); //24
      assert queue.dequeue() == (Integer) null;//True
      System.out.println("dequeue: "+queue.dequeue()); //null
    }

    public static void concurrentTest() {
      final MSQueue queue = new MSQueue();
      int threadCount = 8;
      int nTrials = 8_000_000;


      Runnable enqueue = new TestEnqueueThread(nTrials/2, queue);
      Thread enqueueThread = new Thread(enqueue);
      Runnable enqueue2 = new TestEnqueueThread(nTrials/2, queue);
      Thread enqueueThread2 = new Thread(enqueue2);
      enqueueThread.start();
      enqueueThread2.start();

      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {}

      for(int n = 0; n < threadCount; n++) {
        Runnable dequeue = new TestDequeueThread(nTrials / threadCount, queue);
        Thread dequeueThread = new Thread(dequeue);
        dequeueThread.start();
      }
}

    static class TestEnqueueThread implements Runnable {
      int trials;
      final MSQueue queue;
      public TestEnqueueThread(int trials, MSQueue queue) {
        this.trials = trials;
        this.queue = queue;
      }

    public void run() {
      for(int i = 0; i< trials ; i++) {
        //System.out.print(", " + i);
        queue.enqueue(i);
      }
    }
  }

  static class TestDequeueThread implements Runnable {
    int trials;
    final MSQueue queue;
    public TestDequeueThread(int trials, MSQueue queue) {
      this.trials = trials;
      this.queue = queue;
    }

    public void run() {
      System.out.print("dequeueing: ");
      for(int i = 0; i< trials; i++) {
        //try {
          int val = (Integer) queue.dequeue();
          assert (val <  trials * 8 );
          //System.out.print("trial"+i+"= "+ queue.dequeue()+", ");
        //} catch(NullPointerException ex) {System.out.println(""+ex.getMessage());}

      }
    }
  }



}

interface UnboundedQueue<T> {
  void enqueue(T item);
  T dequeue();
}

// ------------------------------------------------------------
// Unbounded lock-based queue with sentinel (dummy) node

class LockingQueue<T> implements UnboundedQueue<T> {
  // Invariants:
  // The node referred by tail is reachable from head.
  // If non-empty then head != tail,
  //    and tail points to last item, and head.next to first item.
  // If empty then head == tail.

  private static class Node<T> {
    final T item;
    Node<T> next;

    public Node(T item, Node<T> next) {
      this.item = item;
      this.next = next;
    }
  }

  private Node<T> head, tail;

  public LockingQueue() {
    head = tail = new Node<T>(null, null);
  }

  public synchronized void enqueue(T item) { // at tail
    Node<T> node = new Node<T>(item, null);
    tail.next = node;
    tail = node;
  }

  public synchronized T dequeue() {     // from head
    if (head.next == null)
      return null;
    Node<T> first = head;
    head = first.next;
    return head.item;
  }
}


// ------------------------------------------------------------
// Unbounded lock-free queue (non-blocking in M&S terminology),
// using CAS and AtomicReference

// This creates one AtomicReference object for each Node object.  The
// next MSQueueRefl class further below uses one-time reflection to
// create an AtomicReferenceFieldUpdater, thereby avoiding this extra
// object.  In practice the overhead of the extra object apparently
// does not matter much.

class MSQueue<T> implements UnboundedQueue<T> {
  private final AtomicReference<Node<T>> head, tail;

  public MSQueue() {
    Node<T> dummy = new Node<T>(null, null);
    head = new AtomicReference<Node<T>>(dummy);
    tail = new AtomicReference<Node<T>>(dummy);
  }

  public void enqueue(T item) { // at tail
    Node<T> node = new Node<T>(item, null);
    while (true) {
      Node<T> last = tail.get(), next = last.next.get();
      if (last == tail.get()) {         // E7
        if (next == null)  {
          // In quiescent state, try inserting new node
          if (last.next.compareAndSet(next, node)) { // E9
            // Insertion succeeded, try advancing tail
            tail.compareAndSet(last, node);
            return;
          }
        } else
          // Queue in intermediate state, advance tail
          tail.compareAndSet(last, next);
      }
    }
  }

  public T dequeue() { // from head
    while (true) {
      Node<T> first = head.get(), last = tail.get(), next = first.next.get(); // D3
    if (first == head.get()) {        // D5
        if (first == last) {
          if (next == null)
            return null;
          else
            tail.compareAndSet(last, next);
        } else {
          T result = next.item;
          if (head.compareAndSet(first, next)) // D13
            return result;
        }
      }
    }
  }

  private static class Node<T> {
    final T item;
    final AtomicReference<Node<T>> next;

    public Node(T item, Node<T> next) {
      this.item = item;
      this.next = new AtomicReference<Node<T>>(next);
    }
  }
}


// --------------------------------------------------
// Lock-free queue, using CAS and reflection on field Node.next

class MSQueueRefl<T> implements UnboundedQueue<T> {
  private final AtomicReference<Node<T>> head, tail;

  public MSQueueRefl() {
    // Essential to NOT make dummy a field as in Goetz p. 334, that
    // would cause a memory management disaster, huge space leak:
    Node<T> dummy = new Node<T>(null, null);
    head = new AtomicReference<Node<T>>(dummy);
    tail = new AtomicReference<Node<T>>(dummy);
  }

  @SuppressWarnings("unchecked")
  // Java's @$#@?!! generics type system: abominable unsafe double type cast
  private final AtomicReferenceFieldUpdater<Node<T>, Node<T>> nextUpdater
    = AtomicReferenceFieldUpdater.newUpdater((Class<Node<T>>)(Class<?>)(Node.class),
                                             (Class<Node<T>>)(Class<?>)(Node.class),
                                             "next");

  public void enqueue(T item) { // at tail
    Node<T> node = new Node<T>(item, null);
    while (true) {
      Node<T> last = tail.get(), next = last.next;
      if (last == tail.get()) {         // E7
        if (next == null)  {
          // In quiescent state, try inserting new node
          if (nextUpdater.compareAndSet(last, next, node)) {
            // Insertion succeeded, try advancing tail
            tail.compareAndSet(last, node);
            return;
          }
        } else {
          // Queue in intermediate state, advance tail
          tail.compareAndSet(last, next);
        }
      }
    }
  }

  public T dequeue() { // from head
    while (true) {
      Node<T> first = head.get(), last = tail.get(), next = first.next;
      if (first == head.get()) {        // D5
        if (first == last) {
          if (next == null)
            return null;
          else
            tail.compareAndSet(last, next);
        } else {
          T result = next.item;
          if (head.compareAndSet(first, next)) {
            return result;
          }
        }
      }
    }
  }

  private static class Node<T> {
    final T item;
    volatile Node<T> next;

    public Node(T item, Node<T> next) {
      this.item = item;
      this.next = next;
    }
  }
}
