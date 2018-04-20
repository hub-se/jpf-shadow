package joda.time;

import gov.nasa.jpf.vm.Verify;

public class ZonedChronology{
	private boolean change(boolean oldVal, boolean newVal){ return oldVal;}
	private int change(int oldVal, int newVal){return newVal;}
	
	public int localToUTC(int localInstant, int offsetFromLocal){
		//final int MAX_INT = 100;
		//final int MIN_INT = -100;
		final int max_offset = 24;
		
		System.out.println(7L * 24 * 60 * 60 * 1000);
		
		if(change(false,true)){
			if(localInstant == Integer.MAX_VALUE){
				return Integer.MAX_VALUE;
			} else if(localInstant == Integer.MIN_VALUE){
				return Integer.MIN_VALUE;
			}
		}
		
		int offset = offsetFromLocal;
		Verify.ignoreIf(offset > max_offset || offset < -max_offset);
		int utcInstant = localInstant - offset;
		
		if(change(false,true)){
			//if(localInstant > 0 && utcInstant < 0 || utcInstant > MAX_INT){
			if(localInstant > 0 && utcInstant < 0){
				return Integer.MAX_VALUE;
			//}else if(localInstant < 0 && utcInstant > 0 || utcInstant < MIN_INT){
			}else if(localInstant < 0 && utcInstant > 0){
				return Integer.MIN_VALUE;
			}
		}
		
		return utcInstant;

	}
	
	public static void main(String[] args){
		ZonedChronology z = new ZonedChronology();
		int x = z.localToUTC(0,20);
	}
}
