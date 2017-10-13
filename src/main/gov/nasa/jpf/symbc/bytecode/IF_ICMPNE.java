package gov.nasa.jpf.symbc.bytecode;

import gov.nasa.jpf.symbc.numeric.*;
import gov.nasa.jpf.symbc.numeric.PathCondition.Diff;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.ThreadInfo.Execute;

/*
 * Implementation of the shadow symbolic IF_ICMPNE bytecode instruction
 */

public class IF_ICMPNE extends gov.nasa.jpf.jvm.bytecode.IF_ICMPNE{
	public IF_ICMPNE(int targetPosition){
	    super(targetPosition);
	  }
	@Override
	public Instruction execute (ThreadInfo ti) {
		StackFrame sf = ti.getModifiableTopFrame();

		Object op_v1 = sf.getOperandAttr(1);
		Object op_v2 = sf.getOperandAttr(0);

		if ((op_v1 == null) && (op_v2 == null)) { //Conditions are concrete
			return super.execute(ti);
		}else{
			if(!ti.isFirstStepInsn()){
				PCChoiceGenerator nextCg;
				PCChoiceGenerator curPcCg;
				
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
				int v2 = ti.getModifiableTopFrame().pop();
				int v1 = ti.getModifiableTopFrame().pop();
				
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
				IntegerExpression sym_v1 = BytecodeUtils.getSymbcExpr(op_v1, v1);
				IntegerExpression shadow_v1 = BytecodeUtils.getShadowExpr(op_v1, v1);
				
				IntegerExpression sym_v2 = BytecodeUtils.getSymbcExpr(op_v2, v2);
				IntegerExpression shadow_v2 = BytecodeUtils.getShadowExpr(op_v2, v2);
				
				//Restore execution mode
				ti.setExecutionMode(curCg.getExecutionMode());
				
				int choice = curCg.getNextChoice();
				
				switch(choice){
				case 0: //True path
					if(ti.getExecutionMode() == Execute.NEW || ti.getExecutionMode() == Execute.BOTH){
						pc._addDet(Comparator.NE,sym_v1,sym_v2);
					}
					if(ti.getExecutionMode() == Execute.OLD || ti.getExecutionMode() == Execute.BOTH){
						if(!pc.isDiffPC()){
							pc._addDet(Comparator.NE,shadow_v1,shadow_v2);
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
						pc._addDet(Comparator.EQ,sym_v1,sym_v2);
					}
					if(ti.getExecutionMode() == Execute.OLD || ti.getExecutionMode() == Execute.BOTH){
						if(!pc.isDiffPC()){
							pc._addDet(Comparator.EQ,shadow_v1,shadow_v2);
						}
					}
					if(!pc.simplify()){ //Unsat, path infeasible
						ti.getVM().getSystemState().setIgnored(true);
					}
					else{
						curCg.setCurrentPC(pc);
					}
					return this.getNext(ti);
					
				case 3: //Diff true path
					pc._addDet(Comparator.NE,sym_v1,sym_v2);
					pc._addDet(Comparator.EQ,shadow_v1,shadow_v2);
					if(!pc.simplify()){ //Unsat, path infeasible
						ti.getVM().getSystemState().setIgnored(true);
					}
					else{
						pc.markAsDiffPC(this.getLineNumber(),Diff.diffTrue);
						curCg.setCurrentPC(pc);
					}
					return this.getTarget();
					
				case 4: //Diff false path
					pc._addDet(Comparator.EQ,sym_v1,sym_v2);
					pc._addDet(Comparator.NE,shadow_v1,shadow_v2);
					if(!pc.simplify()){
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
						
						old_pc._addDet(Comparator.NE, shadow_v1, shadow_v2);
						BytecodeUtils.addConcreteValues(old_pc);

						new_pc._addDet(Comparator.NE, sym_v1, sym_v2);						
						BytecodeUtils.addConcreteValues(new_pc);
						
						boolean old_result = old_pc.simplify();
						boolean new_result = new_pc.simplify();
						
						if(old_result && new_result){
							//Both versions follow the true path
							pc._addDet(Comparator.NE, sym_v1, sym_v2);
							pc._addDet(Comparator.NE, shadow_v1, shadow_v2);
							curCg.setCurrentPC(pc);
							return this.getTarget();
						}
						else if(!old_result && !new_result){
							//Both versions follow the false path
							pc._addDet(Comparator.EQ, sym_v1, sym_v2);
							pc._addDet(Comparator.EQ, shadow_v1, shadow_v2);
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
						IntegerExpression expr_v1 = (ti.getExecutionMode() == Execute.OLD) ? shadow_v1 : sym_v1;
						IntegerExpression expr_v2 = (ti.getExecutionMode() == Execute.OLD) ? shadow_v2 : sym_v2;
						
						PathCondition next_pc = pc.make_copy();
						BytecodeUtils.addConcreteValues(next_pc);

						next_pc._addDet(Comparator.NE, expr_v1, expr_v2);
						if(next_pc.simplify()){
							pc._addDet(Comparator.NE, expr_v1, expr_v2);
							curCg.setCurrentPC(pc);
							return this.getTarget();
						}
						else{
							pc._addDet(Comparator.EQ, expr_v1, expr_v2);
							curCg.setCurrentPC(pc);
							return this.getNext(ti);
						}
					}
				default:
					assert(false);
					return this;
				}
			}
		}//end else
	}
}