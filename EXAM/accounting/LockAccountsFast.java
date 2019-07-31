import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

class LockAccountsFast {
  private final int lockCount;
  private ReentrantLock[] locks;
  private final int[] sums; 
  private int threadCount;
  private int actions;
  private Thread[] threads;
  
  //just for checking correctness
  AtomicInteger runnungVal = new AtomicInteger(0);


  public LockAccountsFast(int length, int threadCount, int actions) {
    this.sums = new int[length];
    this.lockCount = length;
    this.threadCount = threadCount;
    this.actions = actions;
    makeLocks(lockCount);
    createAndStartThreads(threadCount);
    //lock and count meanwhile threads are running
    lockAllAndCount();
    //join all threads so that the final count do not happen before end of execution
    //of all threads
      for(int i = 0; i< threadCount; i++) {
        try {
        threads[i].join();
        } catch(InterruptedException e) {}
      }
      //count when all threads are finished executing - printcheck included
      lockAllAndCount();
      
  }

  //makes lockCount many reentranceLocks 
  public void makeLocks(int lockCount) {
    locks = new ReentrantLock[lockCount];
    for(int i = 0; i < lockCount; i++) {
      locks[i] = new ReentrantLock();
    }
  }
//creates and starts threadCount many Runner threads
public void createAndStartThreads(int threadCount) {
  threads = new Thread[threadCount];
  for(int n = 0; n < threadCount; n++) {
    threads[n] = new Thread(new Runner(sums, locks, actions/threadCount));
  }
  for(int n = 0; n < threads.length; n++) {
    threads[n].start();
  }
}


private void lockAllAndCount() {
  int localTotal = 0;
  int atomicTotal = 0;

  //lock all locks
  for(int i = 0; i < lockCount; i++) 
    locks[i].lock();
    try { 
  //read in atomic value and loop over the array to count the sums
  atomicTotal = runnungVal.get();
  for(int n = 0; n < sums.length; n++) {
    localTotal += sums[n];
  }
  //printcheck of atomic value against calculated value
  System.out.println("Atomic = "+atomicTotal+" , lockTotal = "+localTotal);
}
  //unlock all locks
  finally {
    for(int i = 0; i < lockCount; i++) {
      locks[i].unlock();
    }
  }
}

  public static void main(String[] args) {
    //ensure there are more threads than indexes in sums to test striping
    LockAccountsFast fast = new LockAccountsFast(20,30,100_000_000);
  }




  class Runner implements Runnable {
    final int stripe;
    final int actions;
    final int local = 0;
    final int[] array;
    ReentrantLock[] locks;
    public Runner(int[] array, ReentrantLock[] locks, int actions) {
      this.stripe = this.hashCode() % array.length;
      this.actions = actions;
      this.locks = locks;
      this.array = array;
    }

    public void run() {
      //value to potentially update stripe with
      int val = ThreadLocalRandom.current().nextInt(0,10);
      for(int i = 0; i < actions; i++) {
        //around 50% of actions are executed
        if(ThreadLocalRandom.current().nextInt(0,2) == 0) {
          //lock on stripe
          locks[stripe].lock(); 
          try {
            //update stripe
            array[stripe] += val;
            runnungVal.getAndAdd(val); //to evaluate whether the lockAllAndCount behaves correctly
          } finally {
            //unlock stripe
            locks[stripe].unlock();
          }
        } 
      }
    }
  }
}
