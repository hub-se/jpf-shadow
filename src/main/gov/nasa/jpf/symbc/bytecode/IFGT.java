package gov.nasa.jpf.symbc.bytecode;

import gov.nasa.jpf.symbc.numeric.Comparator;
import gov.nasa.jpf.symbc.numeric.IntegerExpression;
import gov.nasa.jpf.symbc.numeric.PCChoiceGenerator;
import gov.nasa.jpf.symbc.numeric.PathCondition.Diff;
import gov.nasa.jpf.symbc.numeric.PathCondition;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.ThreadInfo.Execute;

/*
 * Implementation of the shadow symbolic IFGT bytecode instruction
 */

public class IFGT extends gov.nasa.jpf.jvm.bytecode.IFGT {
	public IFGT(int targetPosition){
	    super(targetPosition);
	  }
	@Override
	public Instruction execute (ThreadInfo ti) {
		StackFrame sf = ti.getModifiableTopFrame();
		Object op_v = sf.getOperandAttr();

		if(op_v == null) { //Condition is concrete
			return super.execute(ti);
		}
		else {
			if(!ti.isFirstStepInsn()){
				PCChoiceGenerator curPcCg;
				PCChoiceGenerator nextCg;

				ChoiceGenerator<?> curCg = ti.getVM().getSystemState().getChoiceGenerator();
				if(curCg instanceof PCChoiceGenerator){
					curPcCg = (PCChoiceGenerator) curCg;
				}
				else{
					curPcCg = curCg.getPreviousChoiceGeneratorOfType(PCChoiceGenerator.class);
				}
				
				PathCondition pc;
				if(curPcCg != null){
					pc = curPcCg.getCurrentPC();
				}
				else{
					pc = new PathCondition();
				}
				
				/*
				 * Choice 0 -> True path
				 * Choice 1 -> False path
				 * Choice 2 -> Concrete execution
				 * Choice 3 -> Diff-true path
				 * Choice 4 -> Diff-false path
				 */
				
				//When exploring a diff path, we only need to consider the true and false path (choices: 0,1) since we only execute the new version.
				if(pc.isDiffPC()){
					nextCg = new PCChoiceGenerator(0,1,1);
				}
				else{
					/*
					 * When evaluating an if(change(boolean,boolean)) stmt, each parameter is evaluated separately so we cannot directly 
					 * check for divergences. When evaluating the old expression, we use the choices 0,1,2. However, when evaluating
					 * the new expression, we only use choice 2 (concrete execution) if we also used concrete values for the old expression.
					 * Otherwise, we only need to follow the true and false path in the new version.
					 */
					if(BytecodeUtils.isChangeBoolean(this, ti)){
						if(curPcCg.getExecutionMode() == Execute.BOTH){
							//Evaluating the old expression
							nextCg = new PCChoiceGenerator(2,0,-1);
						}
						else{
							//Evaluating the new expression
							if(curPcCg.getNextChoice() == 2){
								//Concrete values used for old expression
								nextCg = new PCChoiceGenerator(2,2,1);
							}
							else{
								nextCg = new PCChoiceGenerator(0,1,1);
							}
						}
					}
					else{
						//The standard case: Until we detect a divergence, we follow the concrete execution and check for divergences along the path (choices: 2,3,4).
						nextCg = new PCChoiceGenerator(2,4,1);
					}
				}
				
				nextCg.setOffset(this.position);
				nextCg.setMethodName(this.getMethodInfo().getFullName());
				nextCg.setExecutionMode(ti.getExecutionMode());
				ti.getVM().getSystemState().setNextChoiceGenerator(nextCg);
				return this;

			}
			else{
				//"Lower part" of cg method, process choice now
				PCChoiceGenerator curCg = (PCChoiceGenerator) ti.getVM().getSystemState().getChoiceGenerator();
				int v = ti.getModifiableTopFrame().pop();
				
				//Get current pc from previous cg
				PathCondition pc;
				PCChoiceGenerator prevCg = curCg.getPreviousChoiceGeneratorOfType(PCChoiceGenerator.class);
				
				if(prevCg == null){
					pc = new PathCondition();
				}
				else{
					pc = prevCg.getCurrentPC();
				}
				
				assert(pc != null);

				//Get symbolic and shadow expressions
				IntegerExpression sym_v = BytecodeUtils.getSymbcExpr(op_v, v);
				IntegerExpression shadow_v = BytecodeUtils.getShadowExpr(op_v, v);
				
				//Restore execution mode
				ti.setExecutionMode(curCg.getExecutionMode());
				
				int choice = curCg.getNextChoice();
				
				switch(choice){
				case 0: //True path
					if(ti.getExecutionMode() == Execute.NEW || ti.getExecutionMode() == Execute.BOTH){
						pc._addDet(Comparator.GT, sym_v, 0);
					}
					if(ti.getExecutionMode() == Execute.OLD || ti.getExecutionMode() == Execute.BOTH){
						if(!pc.isDiffPC()){
							pc._addDet(Comparator.GT, shadow_v, 0);
						}
					}
					if(!pc.simplify()){ //Unsat, path infeasible
						ti.getVM().getSystemState().setIgnored(true);
					}
					else{
						curCg.setCurrentPC(pc);
					}
					return this.getTarget();
					
				case 1: //False path
					if(ti.getExecutionMode() == Execute.NEW || ti.getExecutionMode() == Execute.BOTH){
						pc._addDet(Comparator.LE, sym_v, 0);
					}
					if(ti.getExecutionMode() == Execute.OLD || ti.getExecutionMode() == Execute.BOTH){
						if(!pc.isDiffPC()){
							pc._addDet(Comparator.LE, shadow_v, 0);
						}
					}
					if(!pc.simplify()){ //Path 
						ti.getVM().getSystemState().setIgnored(true);
					}
					else{
						curCg.setCurrentPC(pc);
					}
					return this.getNext(ti);
					
				case 3: //Diff true path
					pc._addDet(Comparator.GT, sym_v, 0);
					pc._addDet(Comparator.LE, shadow_v, 0);
					if(!pc.simplify()){ //Unsat, path infeasible
						ti.getVM().getSystemState().setIgnored(true);
					}
					else{
						pc.markAsDiffPC(this.getLineNumber(), Diff.diffTrue);
						curCg.setCurrentPC(pc);
					}
					return this.getTarget();
				
				case 4: //Diff false path
					pc._addDet(Comparator.LE, sym_v, 0);
					pc._addDet(Comparator.GT, shadow_v, 0);
					if(!pc.simplify()){ //Unsat, path infeasible
						ti.getVM().getSystemState().setIgnored(true);
					}
					else{
						pc.markAsDiffPC(this.getLineNumber(), Diff.diffFalse);
						curCg.setCurrentPC(pc);
					}
					return this.getNext(ti);
				case 2: //Concrete execution	
					if(ti.getExecutionMode() == Execute.BOTH){
						PathCondition old_pc = pc.make_copy();
						PathCondition new_pc = pc.make_copy();
						
						old_pc._addDet(Comparator.GT, shadow_v, 0);
						BytecodeUtils.addConcreteValues(old_pc);

						new_pc._addDet(Comparator.GT, sym_v, 0);						
						BytecodeUtils.addConcreteValues(new_pc);
						
						boolean old_result = old_pc.simplify();
						boolean new_result = new_pc.simplify();
						
						if(old_result && new_result){
							//Both versions follow the true path
							pc._addDet(Comparator.GT, sym_v, 0);
							pc._addDet(Comparator.GT, shadow_v, 0);
							curCg.setCurrentPC(pc);
							return this.getTarget();
						}
						else if(!old_result && !new_result){
							//Both versions follow the false path
							pc._addDet(Comparator.LE, sym_v, 0);
							pc._addDet(Comparator.LE, shadow_v, 0);
							curCg.setCurrentPC(pc);
							return this.getNext(ti);
						}
						else{
							//Both versions follow different paths
							//We ignore this state since one of the following choices should detect this divergence as well (but might generate a different test case)
							//TODO: skip the respective choice instead
							System.out.println(this.getMnemonic()+": Concrete executions diverge at line "+this.getLineNumber()+" (old: "+!old_result+", new: "+!new_result+").");
							ti.getVM().getSystemState().setIgnored(true);
							//return new_result ? this.getTarget() : this.getNext(ti);
							return this.getTarget();
						}

					}
					else{
						//Evaluating the old or new expression in an if(change(boolean,boolean)) stmt
						IntegerExpression expr_v = (ti.getExecutionMode() == Execute.OLD) ? shadow_v : sym_v;
						
						PathCondition next_pc = pc.make_copy();
						next_pc._addDet(Comparator.GT, expr_v, 0);
						BytecodeUtils.addConcreteValues(next_pc);

						if(next_pc.simplify()){
							pc._addDet(Comparator.GT, expr_v, 0);
							curCg.setCurrentPC(pc);
							return this.getTarget();
						}
						else{
							pc._addDet(Comparator.LE, expr_v, 0);
							curCg.setCurrentPC(pc);
							return this.getNext(ti);
						}
					}
				default:
					assert(false);
					return this;
				}
			}
		}
	}
}
