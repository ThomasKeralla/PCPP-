import java.util.Random;
import java.io.*;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;

import java.io.IOException;

class new_ABCsystem {

public static void main(String[] args) {
//  try{
final ActorSystem system = ActorSystem.create("ABCSystem"); //Creating the different Actors
final ActorRef b1 = system.actorOf(Props.create(BankActor.class), "Bank1");
final ActorRef b2 = system.actorOf(Props.create(BankActor.class), "Bank2");
final ActorRef a1 = system.actorOf(Props.create(AccountActor.class), "Account1");
final ActorRef a2 = system.actorOf(Props.create(AccountActor.class), "Account2");
final ActorRef c1 = system.actorOf(Props.create(ClerkActor.class), "Clerk1");
final ActorRef c2 = system.actorOf(Props.create(ClerkActor.class), "Clerk2");
//Tell clerk to start a 100 transactions to (first argument), from (second argument), using bank (third argument).
StartTransferMessage m1 = new StartTransferMessage(a1,a2,b1);
c1.tell(m1, ActorRef.noSender());
c2.tell(new StartTransferMessage(a2,a1,b2), ActorRef.noSender());
//} catch (IOException ioe) {System.out.println("line 22");}

try {
  System.out.println("Press return to inspect..."); System.in.read();
  //Tell the accounts to print their balance
a1.tell(new PrintBalanceMessage(), ActorRef.noSender());
a2.tell(new PrintBalanceMessage(), ActorRef.noSender());

System.out.println("Press return to terminate..."); System.in.read();
} catch(IOException e) {e.printStackTrace();}

finally {
     system.shutdown();
       }
     }//end main
   }//end class

  class AccountActor extends UntypedActor{
    private int balance;
    //Check whether it is a DepositMessage or a PrintBalanceMessage
    //Update balance if deposit, print balance if PrintBalanceMessage
  public void onReceive(Object o) {
  if(o instanceof DepositMessage) {
    DepositMessage message = (DepositMessage) o;
    balance += message.getAmount();
  }
  else if(o instanceof PrintBalanceMessage) {
  System.out.println("Balance at "+ getSelf() + " = " + balance); }
  else
  System.out.println("Unknown Message received at " + getSelf());
  }
}//end AccountActor

class BankActor extends UntypedActor {
public void onReceive(Object o) {
  if(o instanceof TransferMessage) {
      TransferMessage message = (TransferMessage) o;
      //Send DepositMessage to both debit and credit account, one being positive the other negative
message.getTo().tell(new DepositMessage(message.getAmount()), ActorRef.noSender());
message.getFrom().tell(new DepositMessage(message.getAmount() - (message.getAmount()*2)),ActorRef.noSender());
} else
System.out.println("Unknown Message at Bank: " + getSelf()); }
} //end BankActor

class ClerkActor extends UntypedActor {
private Random rd = new Random();
//If it is a StartTransferMessage loop a 100 times sending a TransferMessage to the bank with a random value+1 to avoid zero
public void onReceive(Object o) {
if(o instanceof StartTransferMessage) {
  StartTransferMessage message = (StartTransferMessage) o;
for(int i = 0; i<100; i++)
message.getBank().tell(new TransferMessage(message.getTo(),
message.getFrom(), rd.nextInt(100) +1), ActorRef.noSender());
  } else
System.out.println("Unknown Message at clerk: " + getSelf());
  }
}//end ClerkActor

//simple class which holds details about, sender, reciever, and bank for a transaction
//Values can be accessed with get methods
class StartTransferMessage implements Serializable {
private final ActorRef from;
private final ActorRef to;
private final ActorRef bank;

public StartTransferMessage(ActorRef from, ActorRef to, ActorRef bank) {
  this.from = from;
  this.to = to;
  this.bank = bank;
}
 public ActorRef getTo() {return to; }

 public ActorRef getFrom() {return from; }

public ActorRef getBank() {return bank; }
}//end StartTransferMessage

//simple class which holds details about, sender, reciever, and amount for a transaction
//Values can be accessed with get methods
class TransferMessage implements Serializable {
private final int amount;
private final ActorRef to;
private final ActorRef from;

public TransferMessage(ActorRef to, ActorRef from, int amount) {
this.amount = amount;
this.to = to;
this.from = from;
}
public int getAmount() {return amount; }
public ActorRef getTo() {return to; }
public ActorRef getFrom() {return from; }
}//end TransferMessage

//simple class which holds details about amount that should be deposit
//Value can be accessed with get methods

class DepositMessage implements Serializable {
private final int amount;

public DepositMessage(int amount){
this.amount = amount; }

public int getAmount() {return amount; }
}//end DepositMessage

//Just used as an object for evaluation in AccountActor
class PrintBalanceMessage implements Serializable { /* TODO */ }
