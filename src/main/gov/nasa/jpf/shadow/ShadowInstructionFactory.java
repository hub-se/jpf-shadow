package gov.nasa.jpf.shadow;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.symbc.SymbolicInstructionFactory;

public class ShadowInstructionFactory extends SymbolicInstructionFactory {
	static public boolean debugCG; //Print choice generator information during execution
	static public boolean debugConstraints; //Print constraint solver warnings (e.g. timeouts)
	static public boolean debugChangeBoolean; //Debugging of if(change(boolean,boolean)) case
	public ShadowInstructionFactory(Config conf){
		super(conf);
		
		String debugChoiceGenerator  = conf.getProperty("debug.choiceGenerators");
		if (debugChoiceGenerator != null && debugChoiceGenerator.equals("true")) {
			debugCG = true;
			System.out.println("debug.choiceGenerators=true");
		} else {
			debugCG = false;
		}
		
		String debugConstraintSolver  = conf.getProperty("debug.constraints");
		if (debugConstraintSolver != null && debugConstraintSolver.equals("true")) {
			debugConstraints = true;
			System.out.println("debug.constraints=true");
		} else {
			debugConstraints = false;
		}
		
		String changeBoolean  = conf.getProperty("debug.changeBoolean");
		if (changeBoolean != null && changeBoolean.equals("true")) {
			debugChangeBoolean = true;
			System.out.println("debug.changeBoolean=true");
		} else {
			debugChangeBoolean = false;
		}
		
		
		System.out.println("Running jpf-shadow...");
	}
}
