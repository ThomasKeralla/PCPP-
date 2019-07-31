import java.util.*;
import java.util.concurrent.*;


class TestSuitStripeWriteMap {

//private final Semaphore availableItems, availableSpaces;
protected CyclicBarrier startBarrier, stopBarrier;
final OurMap<Integer, String> map;
protected final int nTrials, nPairs;
protected final AtomicInteger putSum = new AtomicInteger(0);
protected final AtomicInteger keysAdded = new AtomicInteger(0);


public TestSuitStripeWriteMap() {
  this.map = StripedWriteMap(77, 7);
  this.nTrials = 100;
  this.nPairs = 16;
  startBarrier = new CyclicBarrier(nPairs +1);
  stopBarrier = new CyclicBarrier(nPairs +1);
  startThreads();
}

public void startThreads() {
ExecutorService pool = new ExecutorService.newFixedThreadPool(nPairs);
for (int i = 0; i < nPairs; i++) {
pool.execute(new ProducerThread());
  }
  assert map.size() == keysAdded.get();
  System.out.println("Map size: "+map.size()+ " keysAdded "+keysAdded.get());
  pool.shutdown();
}


class ProducerThread implemets Runnable {
  Random rd = new Random(99);

public void run() {
  try{
    int totalSum = 0;
    int keyNumb = 0;
    startBarrier.await();
    for (int i = nTrials; i > 0; --i) {
      int value = rd.nextInt();
      int test = map.put(value);
      totalSum += value;
      if(test != null)
      keyNumb++;

      int value2 = rd.nextInt();
      if(map.putIfAbsent(value2, rd.nextInt())== null) {
        totalSum += value2;
        keyNumb++;
      }
      map.containsKey(rd.nextInt());

      int value3 = rd.nextInt();
      if(map.remove(value3) != null){
        totalSum -= value
        keyNumb--;
      }
    }
    stopBarrier.await();
    putSum.getAndAdd(totalSum);
    keysAdded.getAndAdd(keyNumb);

    } catch(Exception e) {e.stackTrace();}
  }
}

}

class StripedWriteMap<K,V> implements OurMap<K,V> {
  // Synchronization policy: writing to
  //   buckets[hash] is guarded by locks[hash % lockCount]
  //   sizes[stripe] is guarded by locks[stripe]
  // Visibility of writes to reads is ensured by writes writing to
  // the stripe's size component (even if size does not change) and
  // reads reading from the stripe's size component.
  private volatile ItemNode<K,V>[] buckets;
  private final int lockCount;
  private final Object[] locks;
  private final AtomicIntegerArray sizes;

  public StripedWriteMap(int bucketCount, int lockCount) {
    if (bucketCount % lockCount != 0)
      throw new RuntimeException("bucket count must be a multiple of stripe count");
    this.lockCount = lockCount;
    this.buckets = makeBuckets(bucketCount);
    this.locks = new Object[lockCount];
    this.sizes = new AtomicIntegerArray(lockCount);
    for (int stripe=0; stripe<lockCount; stripe++)
      this.locks[stripe] = new Object();
  }

  @SuppressWarnings("unchecked")
  private static <K,V> ItemNode<K,V>[] makeBuckets(int size) {
    // Java's @$#@?!! type system requires "unsafe" cast here:
    return (ItemNode<K,V>[])new ItemNode[size];
  }

  // Protect against poor hash functions and make non-negative
  private static <K> int getHash(K k) {
    final int kh = k.hashCode();
    return (kh ^ (kh >>> 16)) & 0x7FFFFFFF;
  }

  // Return true if key k is in map, else false
  public boolean containsKey(K k) {
    final ItemNode<K,V>[] bs = buckets;
    final int h = getHash(k), stripe = h % lockCount, hash = h % bs.length;
    // The sizes access is necessary for visibility of bs elements
    return sizes.get(stripe) != 0 && ItemNode.search(bs[hash], k, null);
  }


  // Return value v associated with key k, or null
  public V get(K k) {
    final ItemNode<K,V>[] bs = buckets;
    final Holder<V> holder = new Holder<V>();
    final int h = getHash(k), stripe = h % lockCount, hash = h % bs.length;
    final ItemNode<K,V> node = buckets[hash];
      if(ItemNode.search(node, k, holder) && sizes.get(stripe) !=0)
        return holder.get();
    return null;
}

  public int size() {
    int total = 0;
    for(int i = 0; i< sizes.length(); i++) {
        total += sizes.get(i);
    }
    return total;
  }

  // Put v at key k, or update if already present.  The logic here has
  // become more contorted because we must not hold the stripe lock
  // when calling reallocateBuckets, otherwise there will be deadlock
  // when two threads working on different stripes try to reallocate
  // at the same time.
  public V put(K k, V v) {
    final int h = getHash(k), stripe = h % lockCount;
    final Holder<V> old = new Holder<V>();
    ItemNode<K,V>[] bs;
    int afterSize;
    synchronized (locks[stripe]) {
      bs = buckets;
      final int hash = h % bs.length;
      final ItemNode<K,V> node = bs[hash],
        newNode = ItemNode.delete(node, k, old);
      bs[hash] = new ItemNode<K,V>(k, v, newNode);
      // Write for visibility; increment if k was not already in map
      afterSize = sizes.addAndGet(stripe, newNode == node ? 1 : 0);
    }
    if (afterSize * lockCount > bs.length)
      reallocateBuckets(bs);
    return old.get();
  }


  // Put v at key k only if absent.
  public V putIfAbsent(K k, V v) {
    final ItemNode<K,V>[] bs;
    final Holder<V> holder = new Holder<V>();
    final int h = getHash(k), stripe = h % lockCount, hash = h % buckets.length;
        if(ItemNode.search(buckets[hash], k, holder) && sizes.get(stripe) !=0)
          return holder.get();

          else {
            synchronized(locks[stripe]) {
            bs = buckets;
            final int newHash = h % bs.length;
            buckets[newHash] = new ItemNode<K,V>(k, v, buckets[newHash]);
            sizes.getAndIncrement(stripe);
          }
        }
        return null;
  }

  // Remove and return the value at key k if any, else return null
  public V remove(K k) {
    final ItemNode<K,V>[] bs = buckets;
    final Holder<V> holder = new Holder<V>();
    final int h = getHash(k), stripe = h % lockCount, hash = h % buckets.length;
      if(ItemNode.search(bs[hash], k, holder) && sizes.get(stripe) !=0) {
        synchronized (locks[stripe])  {
          final ItemNode<K,V>[] bsNew = buckets;
          final int newHash = h % bs.length;
          final ItemNode<K,V> node = bsNew[newHash];
          final ItemNode<K,V> newNode = ItemNode.delete(node, k, holder);
          buckets[newHash] = newNode;
            sizes.getAndDecrement(stripe);
            return holder.get();
          }
      }
      return null;
    }


  // Iterate over the hashmap's entries one stripe at a time.
  public void forEach(Consumer<K,V> consumer) {
    final ItemNode<K,V>[] bs = buckets;
    int stripeSize = lockCount;
    int startIndex = 0;
    for(int stripe = 0; stripe < stripeSize; stripe++) {
      int entries = sizes.get(stripe);
      for(int i = startIndex; i < startIndex + stripeSize; i++) {
        ItemNode<K,V> node = bs[i];
        if(node != null) {
          consumer.accept(node.k, node.v);
          while(node.next != null) {
            consumer.accept(node.next.k, node.next.v);
            node = node.next;
          }
        }
      }
      startIndex += stripeSize;
    }
  }

  public int bsl() {
    return buckets.length;
  }


  public void print() {
    System.out.println("Not implemented");
  }

  // Now that reallocation happens internally, do not do it externally
  public void reallocateBuckets() { }

  // First lock all stripes.  Then double bucket table size, rehash,
  // and redistribute entries.  Since the number of stripes does not
  // change, and since buckets.length is a multiple of lockCount, a
  // key that belongs to stripe s because (getHash(k) % N) %
  // lockCount == s will continue to belong to stripe s.  Hence the
  // sizes array need not be recomputed.

  // In any case, do not reallocate if the buckets field was updated
  // since the need for reallocation was discovered; this means that
  // another thread has already reallocated.  This happens very often
  // with 16 threads and a largish buckets table, size > 10,000.

  public void reallocateBuckets(final ItemNode<K,V>[] oldBuckets) {
    lockAllAndThen(() -> {
        final ItemNode<K,V>[] bs = buckets;
        if (oldBuckets == bs) {
          // System.out.printf("Reallocating from %d buckets%n", bs.length);
          final ItemNode<K,V>[] newBuckets = makeBuckets(2 * bs.length);
          for (int hash=0; hash<bs.length; hash++) {
            ItemNode<K,V> node = bs[hash];
            while (node != null) {
              final int newHash = getHash(node.k) % newBuckets.length;
              newBuckets[newHash]
                = new ItemNode<K,V>(node.k, node.v, newBuckets[newHash]);
              node = node.next;
            }
          }
          buckets = newBuckets; // Visibility: buckets field is volatile
        }
      });
  }

  // Lock all stripes, perform action, then unlock all stripes
  private void lockAllAndThen(Runnable action) {
    lockAllAndThen(0, action);
  }

  private void lockAllAndThen(int nextStripe, Runnable action) {
    if (nextStripe >= lockCount)
      action.run();
    else
      synchronized (locks[nextStripe]) {
        lockAllAndThen(nextStripe + 1, action);
      }
  }

  static class ItemNode<K,V> {
    private final K k;
    private final V v;
    private final ItemNode<K,V> next;

    public ItemNode(K k, V v, ItemNode<K,V> next) {
      this.k = k;
      this.v = v;
      this.next = next;
    }

    // These work on immutable data only, no synchronization needed.

    public static <K,V> boolean search(ItemNode<K,V> node, K k, Holder<V> old) {
      while (node != null)
        if (k.equals(node.k)) {
          if (old != null)
            old.set(node.v);
          return true;
        } else
          node = node.next;
      return false;
    }

    public static <K,V> ItemNode<K,V> delete(ItemNode<K,V> node, K k, Holder<V> old) {
      if (node == null)
        return null;
      else if (k.equals(node.k)) {
        old.set(node.v);
        return node.next;
      } else {
        final ItemNode<K,V> newNode = delete(node.next, k, old);
        if (newNode == node.next)
          return node;
        else
          return new ItemNode<K,V>(node.k, node.v, newNode);
      }
    }
  }

  // Object to hold a "by reference" parameter.  For use only on a
  // single thread, so no need for "volatile" or synchronization.

  static class Holder<V> {
    private V value;
    public V get() {
      return value;
    }
    public void set(V value) {
      this.value = value;
    }
  }
}

// ----------------------------------------------------------------------
// A wrapper around the Java class library's sophisticated
// ConcurrentHashMap<K,V>, making it implement OurMap<K,V>

class WrapConcurrentHashMap<K,V> implements OurMap<K,V> {
  final ConcurrentHashMap<K,V> underlying = new ConcurrentHashMap<K,V>();

  public boolean containsKey(K k) {
    return underlying.containsKey(k);
  }

  public V get(K k) {
    return underlying.get(k);
  }

  public V put(K k, V v) {
    return underlying.put(k, v);
  }

  public V putIfAbsent(K k, V v) {
    return underlying.putIfAbsent(k, v);
  }

  public V remove(K k) {
    return underlying.remove(k);
  }

  public int size() {
    return underlying.size();
  }

  public void forEach(Consumer<K,V> consumer) {
    underlying.forEach((k,v) -> consumer.accept(k,v));
  }

  public void reallocateBuckets() { }

  public void print() {
    System.out.println("Not implemented");
  }

  public int bsl() {
    return 0;
  }

}
