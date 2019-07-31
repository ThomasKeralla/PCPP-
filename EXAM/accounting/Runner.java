import java.util.stream.*;
import java.util.function.*;
import static java.util.stream.Collectors.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Random;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Arrays;
import java.util.List;
import java.util.*;





public class Runner {


    public static void main(String[] args) {
        final int n = 100;
        //testAccounts(new UnsafeAccounts(n), n);
        //testAccounts(new LockAccounts(n), n);

        final int numberOfTransactions = 1000;
        applyTransactionsLoop(n, numberOfTransactions, () -> new UnsafeAccounts(n));
        applyTransactionsCollect(n, numberOfTransactions, () -> new UnsafeAccounts(n));

       
        //conccurentTest(new UnsafeAccounts(n), n, numberOfTransactions);
        //conccurentTest( new LockAccounts(n), n, numberOfTransactions);
        //conccurentTest(new CASAccounts(n), n, numberOfTransactions);
        
        //VISIBILITY TESTING
        //Accounts account = new UnsafeAccounts(n);
        //Accounts account = new LockAccounts(n);
        //Accounts account = new CASAccounts(n);
        /*
        account.deposit(0,0);
        visibilityTest(account,n,10);
        account.deposit(0,10);
        try{
          Thread.sleep(1000);
        } catch(InterruptedException e) {}
        account.deposit(0,10);

*/
      
      }

    

    public static void testAccounts(Accounts accounts, final int n) {
        if (n <= 2) {
            System.out.println("Accounts must be larger that 2 for this test to work");
            assert (false); // test only supports larger accounts than 2.
            return;
        }
        assert (accounts.sumBalances() == 0);
        accounts.deposit(n - 1, 55);
        assert (accounts.get(n - 1) == 55);
        assert (accounts.get(n - 2) == 0);
        assert (accounts.sumBalances() == 55);
        accounts.deposit(0, 45);
        assert (accounts.sumBalances() == 100);

        accounts.transfer(0, n - 1, -10);
        assert (accounts.sumBalances() == 100);
        assert (accounts.get(n - 1) == 45);
        assert (accounts.get(0) == 55);
        accounts.transfer(1, n - 1, 10);
        assert (accounts.get(n - 1) == 55);
        assert (accounts.get(1) == -10);
        assert (accounts.get(0) == 55);
        assert (accounts.sumBalances() == 100);

        accounts.transferAccount(accounts);
        assert (accounts.get(n - 1) == 55 * 2);
        assert (accounts.get(1) == -10 * 2);
        assert (accounts.get(0) == 55 * 2);
        assert (accounts.sumBalances() == 200);

        System.out.printf(accounts.getClass() + " passed sequential tests\n");
    }

    // Question 1.7.1
    private static void applyTransactionsLoop(int numberOfAccounts, int numberOfTransactions,
            Supplier<Accounts> generator) {
        // remember that if "from" is -1 in transaction then it is considered a deposit
        // otherwise it is a transfer.
        final Accounts accounts = generator.get();
        
        Stream<Transaction> transaction = IntStream.range(0, numberOfTransactions).parallel().mapToObj((i) -> new Transaction(numberOfAccounts, i));
        // implement applying each transaction by using a for-loop
        // Modify it to run with a parallel stream.
 // YOUR CODE GOES HERE
    
    final LockAccounts safeAccounts = new LockAccounts(numberOfAccounts);
    transaction.forEach( t -> { if( t.from == -1) {safeAccounts.deposit(t.to, t.amount);} else safeAccounts.transfer(t.from, t.to, t.amount); }); 
    
    System.out.println("numb accounts: "+numberOfAccounts+" numb transactions: "+numberOfTransactions);

    int total=0;
    for(int i =0; i<numberOfAccounts;i++){
       System.out.print(safeAccounts.get(i)+" , "); 
       total += safeAccounts.get(i); 
    }     
    System.out.println( "\n Printing accumulated total: "+total);

    System.out.println("sumBalance: "+safeAccounts.sumBalances()+"\n");
    }

    // Question 1.7.2
    private static void applyTransactionsCollect(int numberOfAccounts, int numberOfTransactions,
                                                 Supplier<Accounts> generator) {
        // remember that if "from" is -1 in transaction then it is considered a deposit
        // otherwise it is a transfer.
        Stream<Transaction> transactions = IntStream.range(0, numberOfTransactions).parallel()
                .mapToObj((i) -> new Transaction(numberOfAccounts, i));

        // Implement applying each transaction by using the collect stream operator.
        // Modify it to run with a parallel stream.
 // YOUR CODE GOES HERE

LockAccounts accounts = new LockAccounts(numberOfAccounts);

  Map<Boolean, List<Transaction>> result = transactions.collect(partitioningBy((Transaction s) -> s.from == -1));
  result.get(true).parallelStream().forEach(t -> accounts.deposit(t.to, t.amount));
  result.get(false).parallelStream().forEach(t -> accounts.transfer(t.from, t.to, t.amount));

  Accounts ac = generator.get();
  ac.transferAccount(accounts);

  System.out.println("numb accounts: "+numberOfAccounts+" numb transactions: "+numberOfTransactions);

  int total=0;
  for(int i =0; i<numberOfAccounts;i++){
     System.out.print(accounts.get(i)+" , "); 
     total += accounts.get(i); 
  }     
  System.out.println( "\n Printing accumulated total: "+total);

  System.out.println("sumBalance: "+accounts.sumBalances());
  System.out.println("sumBalance after transfer: "+ac.sumBalances());
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