
public class PrinterTest {
  public static void main(String[] args) {

    Printer lock1 = new Printer();

    Thread t2 = new Thread(()-> { while(true)
      Printer.print();});

    Thread t1 = new Thread(()-> { while(true)
      Printer.print(); });

      t1.start(); t2.start();
  }
}

class Printer {
  public Printer() {}
  public static void print() {

    synchronized ("lock1") {
    System.out.print("-");
    try { Thread.sleep(50); }
    catch (InterruptedException exn) { }
    System.out.print("|");
      }
    }
  }



/*
class Printer {
  private String dash = "-";
  private String linier = "|";
  boolean b = true;
  public void print() {

    if(b) {System.out.print(dash);
    b = false;
    }
    else {System.out.print(linier);
    b = true;
    }
  }
}
*/
