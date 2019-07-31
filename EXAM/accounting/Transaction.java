import java.util.Random;

//. A transaction is a data object describing either a deposit or a transfer between two accounts.
//. If "from" is -1 then the transaction is considered a Deposit otherwise its a transfer.
//. You should not modify this class.
public class Transaction {
    public final int from, to, amount, n;

    // default constructor for creating a transaction.
    Transaction(int nn, int fr, int t, int amnt) {
        from = fr;
        to = t;
        amount = amnt;
        n = nn;
    };

    // Given a max account number and the position in a list of transactions,
    // generate a random transaction.
    Transaction(int nn, int i) { // n is the max account nr, i is the pos in the list
        n = nn;
        if (i < n) { // we initialize all accounts to some (arbitrary) value
            from = -1;
            to = i;
            amount = 1000; 
        } else {
            Random rng = new Random(i ^ 213412431);
            // chose pseudo random source and destination
            from = (rng.nextInt() & 0x7fffffff) % n; 
            to = (rng.nextInt() & 0x7fffffff) % n; 
            // choose pseudo random amount 0..255
            amount = rng.nextInt() & 0xff; 
        }
    }

    public String toString() {
        return "(" + from + " -> " + to + ": " + amount + ")";
    }
}
