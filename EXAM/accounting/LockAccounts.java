import java.util.concurrent.locks.ReentrantLock;

class LockAccounts implements Accounts {
  private int[] accounts;
  ReentrantLock[] locks;

  public LockAccounts(int size) {
    makeLocks(size);
    init(size);
  }

  public void makeLocks(int size) {
    locks = new ReentrantLock[size];
    for(int i = 0; i < size; i++) {
      locks[i] = new ReentrantLock();
    }
  }
  //no need for locking as no interaction between interleaving threads will happen
  public void init(int n) {
      accounts = new int[n];
  }

  public int get(int account) {
    int val;
    locks[account].lock();
      val = accounts[account];
    locks[account].unlock();
    return val;
  }

  public int sumBalances() {
      int sum = 0;
      for (int i = 0; i < accounts.length; i++) {
        locks[i].lock();
          sum += accounts[i];
        locks[i].unlock();
      }
      return sum;
  }

  public void deposit(int to, int amount) {
    locks[to].lock();
      accounts[to] += amount;
    locks[to].unlock();
  }

  public void transfer(int from, int to, int amount) {

    locks[from].lock();
    locks[to].lock();
    try {
      accounts[from] -= amount;
      accounts[to] += amount;
    } finally {
    locks[from].unlock();
    locks[to].unlock();
    }
  }

  public void transferAccount(Accounts other) {
      for (int i = 0; i < accounts.length; i++) {
        locks[i].lock();
        //other.locks[i].lock(); - if I knew other was same implementation
          accounts[i] += other.get(i);
          locks[i].unlock();
          //other.locks[i].unlock();
      }
  }

  public String toString() {
      String res = "";
      if (accounts.length > 0) {
        locks[0].lock();
          res = "" + accounts[0];
        locks[0].unlock();
          for (int i = 1; i < accounts.length; i++) {
            locks[i].lock();
              res = res + " " + accounts[i];
            locks[i].unlock();
          }
      }
      return res;
  }

}
