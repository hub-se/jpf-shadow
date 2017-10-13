package jpf2017.bankaccount.withdraw;

public class BankAccount_8c {
	
	public int change(int oldVal, int newVal){return oldVal;}
	public float change(float oldVal, float newVal) {return oldVal;}
	public double change(double oldVal, double newVal){return oldVal;}
	public boolean change(boolean oldVal, boolean newVal){return oldVal;}
	public long change(long oldVal, long newVal){return oldVal;}
	
	

	private int balance;

	public BankAccount_8c(int amount) {
		super();
		balance = amount;
	}

	public void withdraw(int amount, int numberOfWithdrawals) {
		if (amount > balance) {
			return;
		}
		if (numberOfWithdrawals >= 5) {
			assert (false);
			if (change(true, false)) {
				return;
			}
		}
		balance = (balance - amount);
	}

	public static void main(String[] args) {
		BankAccount_8c b = new BankAccount_8c(0);
		b.withdraw(-1, 6);
	}
}
