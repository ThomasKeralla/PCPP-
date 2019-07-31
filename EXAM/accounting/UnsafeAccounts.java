

public class UnsafeAccounts implements Accounts {
    private int[] accounts;

    public UnsafeAccounts(int n) {
        accounts = new int[n];
    }

    public void init(int n) {
        accounts = new int[n];
    }

    public int get(int account) {
        return accounts[account];
    }

    public int sumBalances() {
        int sum = 0;
        for (int i = 0; i < accounts.length; i++) {
            sum += accounts[i];
        }
        return sum;
    }

    public void deposit(int to, int amount) {
        accounts[to] += amount;
    }

    public void transfer(int from, int to, int amount) {
        accounts[from] -= amount;
        accounts[to] += amount;
    }

    public void transferAccount(Accounts other) {
        for (int i = 0; i < accounts.length; i++) {
            accounts[i] += other.get(i);
        }
    }

    public String toString() {
        String res = "";
        if (accounts.length > 0) {
            res = "" + accounts[0];
            for (int i = 1; i < accounts.length; i++) {
                res = res + " " + accounts[i];
            }
        }
        return res;
    }

}
