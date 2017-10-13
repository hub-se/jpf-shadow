package jpf2017.bankaccount.main;

import gov.nasa.jpf.vm.Verify;

public class BankAccount_5_18_23c {
	private int balance;
	private int numberOfWithdrawals;
	
	public int change(int oldVal, int newVal){return oldVal;}
	public float change(float oldVal, float newVal) {return oldVal;}
	public double change(double oldVal, double newVal){return oldVal;}
	public boolean change(boolean oldVal, boolean newVal){return oldVal;}
	public long change(long oldVal, long newVal){return oldVal;}
	
	
	
	
	public BankAccount_5_18_23c(int amount) {
		balance = amount;
	}

	public void deposit(int amount) {
		if (amount > 0) {
			System.out.println("I am easily reachable in deposit");
		} 
		balance = change(balance + amount, balance * amount);
	}

	public void withdraw(int amount) {
		if (amount > balance) {
			return;
		}
		if (numberOfWithdrawals >= 5) {
			assert (false);
			return;
		}
		balance = change(balance - amount, balance + amount);
		numberOfWithdrawals++;
	}
	
	public int flag(boolean x) {
		if (change(x, true)) {
			return 1;
		} else {
			return 0;
		}
	}
	
	public void test(boolean flag, int withdrawAmount, int depositAmount){
		int length = 2;
		for (int i = 0; i < length; i++) {
			Verify.beginAtomic();
			switch (flag(flag)) {
			case 0:
				deposit(depositAmount);
				break;
			case 1:
				withdraw(withdrawAmount);
				break;
			}
			Verify.endAtomic();
		}
	}

	public static void main(String[] args) {
		BankAccount_5_18_23c b = new BankAccount_5_18_23c(0);
		b.test(false, 1, 10);
	}
	
}