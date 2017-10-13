package jpf2017.bankaccount.withdraw;

public class BankAccount_7c {
	
	public int change(int oldVal, int newVal){return oldVal;}
	public float change(float oldVal, float newVal) {return oldVal;}
	public double change(double oldVal, double newVal){return oldVal;}
	public boolean change(boolean oldVal, boolean newVal){return oldVal;}
	public long change(long oldVal, long newVal){return oldVal;}
	
	

	private int balance;

	public BankAccount_7c(int amount) {
		super();
		balance = amount;
	}

	public void withdraw(int amount, int numberOfWithdrawals) {
		if (amount > balance) {
			return;
		}
		if (change((numberOfWithdrawals >= 5), true)) {
			assert (false);
			return;
		}
		balance = (balance - amount);
	}

	public static void main(String[] args) {
		BankAccount_7c b = new BankAccount_7c(0);
		b.withdraw(-1, 1);
	}
}
