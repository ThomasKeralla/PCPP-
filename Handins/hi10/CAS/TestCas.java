// For week 10
// sestoft@itu.dk * 2014-11

// javac TestCas.java
// javap -c TestCas
// java -XX:+UnlockDiagnosticVMOptions -XX:+PrintAssembly TestCas a

import java.util.concurrent.atomic.AtomicInteger;

public class TestCas {
  public static void main(String[] args) {
    AtomicInteger ai = new AtomicInteger();
    
    for (int i=0; i<100000; i++) {
      ai.compareAndSet(65, ("foo" + args[0]).length());
    }
  }
}


/*

bipush        65
...
invokevirtual AtomicInteger.compareAndSet:(II)Z


mov    $0x41,%eax
...
lock cmpxchg %esi,(%rbx)


*/
