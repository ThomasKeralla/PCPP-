import java.util.Random;
import java.io.*;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;

public class ABCsystem2 { // Demo showing how things work:


 public static void main(String[] args) {
 final ActorSystem system = ActorSystem.create("ABCSystem");
 //Creating the different Actors
 final ActorRef[] clerks = new ActorRef[20];
 final ActorRef[] accounts = new ActorRef[200];
 final ActorRef[] banks = new ActorRef[7];


Random r = new Random();

final ActorRef supervisor = system.actorOf(Props.create(Supervisor.class), "Supervisor");
supervisor.tell(new NumbOfAccounts(accounts.length),ActorRef.noSender());

  for(int n = 0; n < banks.length; n++) {
    final ActorRef bank = system.actorOf(Props.create(BankActor.class), "Bank"+n+1);
     banks[n] = bank;
  }

 for(int t = 0; t < accounts.length; t++) {
   final ActorRef account = system.actorOf(Props.create(AccountActor.class), "Account"+t+1);
   accounts[t] = account;
 }

for(int j = 0; j < clerks.length; j++) {
  final ActorRef clerk = system.actorOf(Props.create(ClerkActor.class), "Clerk"+j+1);
  clerks[j] = clerk;
}

//Tell clerk to start a 100 transactions to (first argument), from (second argument), using bank (third argument).
for(int i = 0; i<10000; i++) {
  clerks[r.nextInt(clerks.length)].tell(new StartTransferMessage(accounts[r.nextInt(accounts.length)],accounts[r.nextInt(accounts.length)],banks[r.nextInt(banks.length)]), ActorRef.noSender());
}

 try {
   System.out.println("Press return to inspect...");
   System.in.read();

   for(int p = 0; p < accounts.length; p++)
   accounts[p].tell(new PrintBalanceMessage(supervisor), ActorRef.noSender());

   try {
     Thread.sleep(1000);
   } catch(InterruptedException en) {}

   //supervisor.tell(new Print(),ActorRef.noSender());

   System.out.println("Press return to terminate...");
   System.in.read();
   } catch(IOException e) { e.printStackTrace(); }
   finally {
   system.shutdown();
   }
  }
}

class AccountActor extends UntypedActor{
private int balance;
private int recievedMessages;
//Check whether it is a DepositMessage or a PrintBalanceMessage
//Update balance if deposit, print balance if PrintBalanceMessage
public void onReceive(Object o) {
  if(o instanceof DepositMessage) {
    DepositMessage message = (DepositMessage) o;
    balance += message.getAmount();
    recievedMessages++;
  }

  else if(o instanceof PrintBalanceMessage) {
    //System.out.println("Balance at "+ getSelf() + " = " + balance+ " from "+recievedMessages + " transactions");
    PrintBalanceMessage message = (PrintBalanceMessage) o;
    message.getAddress().tell(new SendBalanceMessage(balance, recievedMessages),getSelf());
  }
  else
  System.out.println("Unknown Message received at " + getSelf());
  }
}

class BankActor extends UntypedActor {

public void onReceive(Object o) {
  if(o instanceof TransferMessage) {
    TransferMessage message = (TransferMessage) o;
    //Send DepositMessage to both debit and credit account, one being positive the other negative
    message.getTo().tell(new DepositMessage(message.getAmount()), ActorRef.noSender());
    message.getFrom().tell(new DepositMessage(message.getAmount() - (message.getAmount()*2)),ActorRef.noSender());
  } else {
    System.out.println("Unknown Message at Bank: " + getSelf());
  }
  }
}

class ClerkActor extends UntypedActor {

private Random rd = new Random();
//If it is a StartTransferMessage loop a 100 times sending a TransferMessage to the bank with a random value+1 to avoid zero
  public void onReceive(Object o) {
    if(o instanceof StartTransferMessage) {
      StartTransferMessage message = (StartTransferMessage) o;

      for(int i = 0; i<10; i++)
          message.getBank().tell(new TransferMessage(message.getTo(), message.getFrom(), rd.nextInt(100) +1), ActorRef.noSender());
    } else
      System.out.println("Unknown Message at clerk: " + getSelf());
  }
}

class Supervisor extends UntypedActor {
int total;
int accountsNumb;
int messages;
int messagescount;


public void onReceive(Object o) {
  if(o instanceof SendBalanceMessage) {
    SendBalanceMessage message = (SendBalanceMessage) o;
    total += message.getAmount();
    accountsNumb++;
    messages += message.getMessages();
    messagescount--;
    //figure out how to get the accounts.length value
    if(messagescount == 0)
    print();
  } if(o instanceof NumbOfAccounts) {
    NumbOfAccounts message = (NumbOfAccounts) o;
    messagescount = message.getAccounts();
  }
    else {
      System.out.println("Unknown Message at Supervisor: " + getSelf());
    }
}
public void print() {
  System.out.println("Total balance = "+total+" from "+accountsNumb+" accounts and " +messages+" messages");
}
}

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
  public ActorRef getTo() {
    return to;
  }
  public ActorRef getFrom() {
    return from;
  }
  public ActorRef getBank() {
    return bank;
  }
}
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
  public int getAmount() {
    return amount;
  }

  public ActorRef getTo() {
    return to;
  }

  public ActorRef getFrom() {
    return from;
  }
}
//simple class which holds details aboutamount that should be deposit
//Value can be accessed with get methods
class DepositMessage implements Serializable {
  private final int amount;
  public DepositMessage(int amount){
    this.amount = amount;
  }
  public int getAmount() {
    return amount;
  }
}
//Just used as an object for evaluation in AccountActor
class PrintBalanceMessage implements Serializable {
private ActorRef supervisor;
public PrintBalanceMessage(ActorRef supervisor) {
  this.supervisor = supervisor;
  }
  public ActorRef getAddress() {
    return supervisor;
  }
}

class SendBalanceMessage implements Serializable {
private int total;
private int messages;

public SendBalanceMessage(int balance, int messages) {
  this.total = balance;
  this.messages = messages;
}
public int getAmount() {
  return total;
}
public int getMessages() {
  return messages;
}
}

class NumbOfAccounts implements Serializable {
  int accounts;
  public NumbOfAccounts(int accounts) {
    this.accounts = accounts;
  }
  public int getAccounts(){
    return accounts;
  }
}
