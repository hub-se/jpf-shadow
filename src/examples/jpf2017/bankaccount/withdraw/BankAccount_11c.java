package jpf2017.bankaccount.withdraw;

public class BankAccount_11c {

	public int change(int oldVal, int newVal){return oldVal;}
	public float change(float oldVal, float newVal) {return oldVal;}
	public double change(double oldVal, double newVal){return oldVal;}
	public boolean change(boolean oldVal, boolean newVal){return oldVal;}
	public long change(long oldVal, long newVal){return oldVal;}
	
	

	private int balance;

	public BankAccount_11c(int amount) {
		super();
		balance = amount;
	}

	public void withdraw(int amount, int numberOfWithdrawals) {
		if (amount > balance) {
			return;
		}
		if (numberOfWithdrawals >= 5) {
			assert (false);
			return;
		}
		balance = change((balance - amount), (balance / amount));
//		if(execute(OLD)){
//		    balance = balance - amount;
//		}
//		if(execute(NEW)){
//		    balance = balance/amount;
//		}

	}

	public static void main(String[] args) {
		BankAccount_11c b = new BankAccount_11c(0);
		b.withdraw(-1, 1);
	}
}
