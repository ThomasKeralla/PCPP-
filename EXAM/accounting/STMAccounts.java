import org.multiverse.api.references.*;
import static org.multiverse.api.StmUtils.*;
import org.multiverse.api.PropagationLevel;

import org.multiverse.api.*;
import org.multiverse.api.functions.*;
import org.multiverse.api.predicates.*;
import org.multiverse.api.references.TxnInteger;


import org.multiverse.api.Txn;
import org.multiverse.api.callables.TxnVoidCallable;
import org.multiverse.api.LockMode;

import org.multiverse.api.references.*;
import static org.multiverse.api.StmUtils.*;

// Multiverse locking:
import org.multiverse.api.LockMode;
import org.multiverse.api.callables.TxnVoidCallable;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ThreadLocalRandom;

public class STMAccounts implements Accounts {
  TxnInteger[] accounts;
  int size;

  public STMAccounts(int size) {
    this.accounts = new TxnInteger[size];
    init(size);
  }

  public void init(int n) {
    for(int i = 0; i < n; i++){
      accounts[i] = newTxnInteger(0);
    }
  }

public int get(int account) {
  int val = atomic(() -> accounts[account].get());
  return val;
}

public int sumBalances() {
  int total = 0;
  for(int i = 0; i < accounts.length; i++) {
    total += get(i);
  }
  return total;
}

// Change the balance of account "to", by amount;
// negative amount is allowed (a withdrawel)
public void deposit(int to, int amount) {
  atomic(() -> accounts[to].set(accounts[to].get() + amount));
}

// Transfer amount from account "from" to account "to"
public void transfer(int from, int to, int amount) {
  atomic(() -> {
deposit(from,-amount);
deposit(to, amount);
    });
}

// Transfer all the balances from other to this accounts object.
public void transferAccount(Accounts other) {
  atomic(() -> {
    for(int i = 0; i < accounts.length; i++) {
      deposit(i, other.get(i));
    }
  });
}

  //Assignment 1.1
  public static void conccurentTest(Accounts accounts, final int deposits, final int actions) {

    Random rd = new Random();
    AtomicInteger total = new AtomicInteger(0);

    int threadsNumb;
    if((actions/deposits) > 2) {
      threadsNumb = actions/deposits;
    } else {threadsNumb = 5;}

    System.out.println("Total balance = "+accounts.sumBalances());
    int totalDeposit = 0;
    for (int i = 0; i < deposits; i++) {
      int depositValue = rd.nextInt(101)-50;
      accounts.deposit(i,depositValue); //deposit from -50 to +50 in each account
      totalDeposit += depositValue;
    }
    System.out.println("Total balance = "+accounts.sumBalances());
    System.out.println("totalDeposit = "+totalDeposit);

    int actionsPerThread = actions/threadsNumb;
    Thread[] threads = new Thread[threadsNumb];
    for(int n = 0; n < threadsNumb; n++) {
      threads[n] = new Thread(()-> {
        int localTotal = 0;

        for(int j = 0; j < actionsPerThread; j++) {
            int transferAmount = ThreadLocalRandom.current().nextInt(100)-50;
            int from = ThreadLocalRandom.current().nextInt(deposits);
            int to = ThreadLocalRandom.current().nextInt(deposits);
            //int valFrom = accounts.get(from);
            //int valTo = accounts.get(to);
            accounts.transfer(from,to,transferAmount);
            localTotal += transferAmount;
        }
        total.getAndAdd(localTotal);
      });
    }

    for(Thread t: threads) 
      t.start();
      
    try {
      for(Thread t: threads) 
        t.join();
    } catch (InterruptedException e) {}

  System.out.println("Start balance = "+totalDeposit +", total transfered = "+total.get()+ " balance should be equal to = "+totalDeposit+", current total is = "+ accounts.sumBalances());
  assert (accounts.sumBalances() == totalDeposit);
  
}




//Assignment 1.2
public static void visibilityTest(Accounts account, int length, int threads) {
int check=account.get(0);
for(int i = 0; i< threads; i++) {
  Thread t = new Thread ( () -> {
    while(account.get(0)==0){
      //spin
    }
    System.out.println(""+Thread.currentThread()+" Terminated");
  } );
  t.start();
  }
}

//this works
public static void main(String[] args) {
  conccurentTest(new STMAccounts(100), 100, 1000);
  STMAccounts account = new STMAccounts(100);
  account.deposit(0,0);
        visibilityTest(account,100,10);
        account.deposit(0,10);
        try{
          Thread.sleep(1000);
        } catch(InterruptedException e) {}
        account.deposit(0,10);
  }

}

interface Accounts {
    // (Re)initializes n accounts with balance 0 each.
    public void init(int n);

    // Returns the balance of account "account"
    public int get(int account);

    // Returns the sum of all balances.
    public int sumBalances();

    // Change the balance of account "to", by amount;
    // negative amount is allowed (a withdrawel)
    public void deposit(int to, int amount);

    // Transfer amount from account "from" to account "to"
    public void transfer(int from, int to, int amount);

    // Transfer all the balances from other to this accounts object.
    public void transferAccount(Accounts other);
}

