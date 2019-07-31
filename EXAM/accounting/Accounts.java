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
