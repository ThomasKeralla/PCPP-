// For week 1
// rikj@itu.dk 2017-08-14 ; sestoft@itu.dk * 2014-08-20, 2015-08-27

import java.io.IOException;

public class TestLongCounterNaiv {
  public static void main(String[] args) throws IOException {
    final LongCounter lc = new LongCounter();
    Thread t = new Thread(() -> {
	while (true)		// Forever call increment
	  lc.increment();
      });
    t.start();
    System.out.println("Press Enter to get the current value:");
    while (true) {
      System.in.read();         // Wait for enter key
      long 
      System.out.println(lc.get()); 
    }
  }
}

class LongCounter {
  private long count = 0;
  public  void increment() {
    count = count + 1;
  }
  public  long get() { 
    return count; 
  }
}
