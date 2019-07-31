// For week 1
// sestoft@itu.dk * 2016-09-01

import java.io.IOException;

public class TestLongCounterBetter {
  public static void main(String[] args) throws IOException {
    final LongCounterBetter lc = new LongCounterBetter();
    Thread t = new Thread(() -> {
	while (true)		// Forever call increment
	  lc.increment();
      });
    t.start();
    System.out.println("Press Enter to get the current value:");
    while (true) {
      System.in.read();         // Wait for enter key
      System.out.println(lc.get()); 
    }
  }
}

class LongCounterBetter {
  private long count = 0;
  private final Object myLock = new Object();
  public void increment() {
    synchronized (myLock) {
      count = count + 1;
    }
  }
  public long get() { 
    synchronized (myLock) {
      return count;       
    }
  }
}
