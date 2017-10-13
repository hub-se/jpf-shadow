package jpf2017.bankaccount.deposit;

public class BankAccount_4c {

	public int change(int oldVal, int newVal){return oldVal;}
	public float change(float oldVal, float newVal) {return oldVal;}
	public double change(double oldVal, double newVal){return oldVal;}
	public boolean change(boolean oldVal, boolean newVal){return oldVal;}
	public long change(long oldVal, long newVal){return oldVal;}

	private int balance;

	public BankAccount_4c(int amount) {
		balance = amount;
	}

	public void deposit(int amount) {
		if (amount > 0) {
			if (change(true, false)) {
				System.out.println("I am easily reachable in deposit");
			}
		}
		balance = (balance + amount);
	}

	public static void main(String[] args) {
		BankAccount_4c b = new BankAccount_4c(0);
		b.deposit(1);
	}
}
