// For week 1
// sestoft@itu.dk * 2016-09-01

public class TestLocking3 {
  public static void main(String[] args) {
    final int counts = 10_000_000;
    Thread t1 = new Thread(() -> {
      for (int i=0; i<counts; i++) 
	MysteryB.increment();
    });
    Thread t2 = new Thread(() -> {
      for (int i=0; i<counts; i++) 
	MysteryB.increment4();
    });
    t1.start(); t2.start();
    try { t1.join(); t2.join(); }
    catch (InterruptedException exn) { 
      System.out.println("Some thread was interrupted");
    }
    System.out.println("Count is " + MysteryA.get() + " and should be " + 5*counts);
  }
}

class MysteryA {
  protected static long count = 0;

  public static synchronized void increment() {
    count++;
  }

  public static synchronized long get() { 
    return count; 
  }
}

class MysteryB extends MysteryA {
  public static synchronized void increment4() {
    count += 4;
  }
}
