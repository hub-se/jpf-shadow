package jpf2017.bankaccount.withdraw;

public class BankAccount_10c {
	
	public int change(int oldVal, int newVal){return oldVal;}
	public float change(float oldVal, float newVal) {return oldVal;}
	public double change(double oldVal, double newVal){return oldVal;}
	public boolean change(boolean oldVal, boolean newVal){return oldVal;}
	public long change(long oldVal, long newVal){return oldVal;}
	
	

	private int balance;

	public BankAccount_10c(int amount) {
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
		balance = change((balance - amount), (balance + amount));
	}

	public static void main(String[] args) {
		BankAccount_10c b = new BankAccount_10c(0);
		b.withdraw(-1, 1);
	}
}
