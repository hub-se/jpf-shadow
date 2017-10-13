package gov.nasa.jpf.symbc.bytecode;



import gov.nasa.jpf.symbc.numeric.*;
import gov.nasa.jpf.symbc.numeric.PathCondition.Diff;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.ThreadInfo.Execute;

/*
 * Implementation of the shadow symbolic IDIV bytecode instruction
 */

public class IDIV extends gov.nasa.jpf.jvm.bytecode.IDIV {

	@Override
	public Instruction execute (ThreadInfo ti) {
		StackFrame sf = ti.getModifiableTopFrame();
		Object op_v1 = sf.getOperandAttr(0);
		Object op_v2 = sf.getOperandAttr(1);

		if(op_v1==null && op_v2==null){
			return super.execute(ti); //Operands are concrete
		}
		
		//We have at least one symbolic operand and should check whether the denominator can be zero
		if (!ti.isFirstStepInsn()) { //First time around
			
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
			* Choice 0: Denominator is zero
			* Choice 1: Denominator is not zero
			* Choice 2: New denominator is zero, old is not --> regression
			* Choice 3: New denominator is not zero, old is zero --> "fix"
			* 
			*  In a diff path we only need to check for v1 == 0 and v1 != 0 since we only execute the new version.
			*  Division during the evaluation of a change(boolean,boolean) expression also only requires 2 choices, since
			*  both expressions are handled separately.
			*/
			
			if(!pc.isDiffPC() && ti.getExecutionMode() == Execute.BOTH){
				nextCg = new PCChoiceGenerator(4);
			}
			else{
				nextCg = new PCChoiceGenerator(2);
			}
			
			nextCg.setOffset(this.position);
			nextCg.setMethodName(this.getMethodInfo().getFullName());
			nextCg.setExecutionMode(ti.getExecutionMode());
			ti.getVM().setNextChoiceGenerator(nextCg);
			return this;
		} 
		else{  //This is what really returns results
			int v1 = sf.pop();
			int v2 = sf.pop();
			sf.push(0,false);
			
			//Symbolic and shadow expressions of the operands and the result
			IntegerExpression sym_v1 = BytecodeUtils.getSymbcExpr(op_v1, v1); 
			IntegerExpression shadow_v1 = BytecodeUtils.getShadowExpr(op_v1, v1); 
			
			IntegerExpression sym_v2 = BytecodeUtils.getSymbcExpr(op_v2, v2); 
			IntegerExpression shadow_v2 = BytecodeUtils.getShadowExpr(op_v2, v2); 
			
			IntegerExpression sym_result = null; 
			IntegerExpression shadow_result = null; 
			
			
			PCChoiceGenerator curCg = (PCChoiceGenerator) ti.getVM().getChoiceGenerator();
			PathCondition pc;
			PCChoiceGenerator prevCg = curCg.getPreviousChoiceGeneratorOfType(PCChoiceGenerator.class);
			
			//Get current path condition
			if(prevCg == null){
				pc = new PathCondition();

			}
			else{
				pc = prevCg.getCurrentPC();
			}
			assert(pc != null);
			
			ti.setExecutionMode(curCg.getExecutionMode());
			
			int choice = curCg.getNextChoice();
			switch(choice){
			case 0: //Denominator is zero --> throw arithmetic exception
				if(ti.getExecutionMode() == Execute.NEW || ti.getExecutionMode() == Execute.BOTH){
					pc._addDet(Comparator.EQ, sym_v1, 0);
				}
				if(ti.getExecutionMode() == Execute.OLD || ti.getExecutionMode() == Execute.BOTH){
					pc._addDet(Comparator.EQ, shadow_v1, 0);
				}
				
				if(!pc.simplify()){
					ti.getVM().getSystemState().setIgnored(true);
					return this.getNext(ti);
				}
				else{
					pc.markAsDiffPC(this.getLineNumber(),Diff.divByZero); //technically not a diff path, just to generate test case
					curCg.setCurrentPC(pc);
					
					//If only the old version divides by zero (e.g. if(change(2/x == 2, x == 2)) we can continue with the execution
					//TODO: Handle division in change(int,int)
					if(ti.getExecutionMode() == Execute.OLD){
						sym_result = sym_v2._div(sym_v1);
						shadow_result = new IntegerConstant(0);
						DiffExpression result = new DiffExpression(shadow_result,sym_result);
						sf.setOperandAttr(result);
						return this.getNext();
					}
					return ti.createAndThrowException("java.lang.ArithmeticException","div by 0");
				}
				
			case 1: //Denominator is not zero --> set result and continue normally
				if(ti.getExecutionMode() == Execute.NEW || ti.getExecutionMode() == Execute.BOTH){
					pc._addDet(Comparator.NE, sym_v1, 0);
				}
				if(ti.getExecutionMode() == Execute.OLD || ti.getExecutionMode() == Execute.BOTH){
					pc._addDet(Comparator.NE, shadow_v1, 0);
				}
				
				if(!pc.simplify()){
					ti.getVM().getSystemState().setIgnored(true);
				}
				else{
					curCg.setCurrentPC(pc);
					sym_result = sym_v2._div(sym_v1);
					shadow_result = shadow_v2._div(shadow_v1);
						
					if((op_v1 instanceof DiffExpression) || (op_v2 instanceof DiffExpression)){
						DiffExpression result = new DiffExpression(shadow_result,sym_result);
						sf.setOperandAttr(result);
					}
					else{
						IntegerExpression result = sym_result;
						sf.setOperandAttr(result);
					}
				}
				return this.getNext(ti);
			case 2: //"true" diff, new denominator is zero while old is not --> regression
				pc._addDet(Comparator.EQ, sym_v1, 0);
				pc._addDet(Comparator.NE, shadow_v1, 0);
				
				if(!pc.simplify()){
					ti.getVM().getSystemState().setIgnored(true);
					return this.getNext(ti);
				}
				else{
					pc.markAsDiffPC(this.getLineNumber(),Diff.divByZero);
					curCg.setCurrentPC(pc);
					return ti.createAndThrowException("java.lang.ArithmeticException","div by 0");
				}
			case 3: //"false" diff, new is not zero but old is --> bug fix
				pc._addDet(Comparator.NE, sym_v1, 0);
				pc._addDet(Comparator.EQ, shadow_v1, 0);
				
				if(!pc.simplify()){
					ti.getVM().getSystemState().setIgnored(true);
				}
				else{
					pc.markAsDiffPC(this.getLineNumber(),Diff.divByZero);
					curCg.setCurrentPC(pc);
					
					sym_result = sym_v2._div(sym_v1);
					shadow_result = new IntegerConstant(0); //old is NaN
					DiffExpression result = new DiffExpression(shadow_result,sym_result);
					sf.setOperandAttr(result);
				}
				return this.getNext(ti);	
			default:
				assert(false);
				return this;
			}	
		}
	}
}