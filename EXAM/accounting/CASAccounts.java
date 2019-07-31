import java.util.concurrent.atomic.AtomicInteger;

public class CASAccounts implements Accounts {
int size;
AtomicInteger[] accounts;
AtomicInteger total = new AtomicInteger(0);
  public CASAccounts(int size) {
    this.size = size;
    accounts = new AtomicInteger[size];
    init(size);
  }
  public void init(int n) {
    for(int i = 0; i<n; i++) {
      accounts[i] = new AtomicInteger(0);
    }
  }

  // Returns the balance of account "account"
  public int get(int account) {
    return accounts[account].get();
  }

  // Returns the sum of all balances.
  public int sumBalances() {
    return total.get();
  }

  // Change the balance of account "to", by amount;
  // negative amount is allowed (a withdrawel)
  public void deposit(int to, int amount) {
    int toAccount;
    do {
      toAccount = accounts[to].get();
    } while(!accounts[to].compareAndSet(toAccount, toAccount+amount));
    total.getAndAdd(amount);
  }

  // Transfer amount from account "from" to account "to"
  public void transfer(int from, int to, int amount) {
    int val;
    do {
      val = accounts[to].get();
    } while(!accounts[to].compareAndSet(val, val + amount));
    int val2;
    do {
      val2 = accounts[from].get();
    } while(!accounts[from].compareAndSet(val2, val2 - amount));
  }
/*
  public void transfer(int from, int to, int amount) {
    int toAccount;
    int fromAccount;
    do{
      toAccount = accounts[to].get();
      fromAccount = accounts[from].get();
    } while((!accounts[to].compareAndSet(toAccount, toAccount + amount) && !accounts[from].compareAndSet(fromAccount, fromAccount - amount)) || accounts[to].compareAndSet(toAccount + amount, toAccount));
  }
  */

  // Transfer all the balances from other to this accounts object.
  public void transferAccount(Accounts other) {
    for(int i = 0; i < accounts.length; i++) {
      int check;
      do{
        check = accounts[i].get();
      } while(!accounts[i].compareAndSet(check, check + other.get(i)));
      total.getAndAdd(other.get(i));
    }
  }
}
