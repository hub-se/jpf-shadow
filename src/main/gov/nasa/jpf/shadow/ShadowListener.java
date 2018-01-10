package gov.nasa.jpf.shadow;


import gov.nasa.jpf.Config;

import gov.nasa.jpf.JPF;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.ThreadInfo.Execute;
import gov.nasa.jpf.vm.VM;
import gov.nasa.jpf.jvm.bytecode.*;
import gov.nasa.jpf.symbc.numeric.*;
import gov.nasa.jpf.util.MethodSpec;

import java.lang.Object;

import gov.nasa.jpf.symbc.SymbolicListener;
import gov.nasa.jpf.symbc.bytecode.BytecodeUtils;

public class ShadowListener extends SymbolicListener{
	
	//Method specifications for the change()-methods
	MethodSpec[] changeMethods = new MethodSpec[5]; 
	
	public ShadowListener(Config conf, JPF jpf) {
		super(conf,jpf);
		changeMethods[0] = MethodSpec.createMethodSpec("*.change(boolean,boolean)");
		changeMethods[1] = MethodSpec.createMethodSpec("*.change(int,int)");
		changeMethods[2] = MethodSpec.createMethodSpec("*.change(float,float)");
		changeMethods[3] = MethodSpec.createMethodSpec("*.change(double, double)");
		changeMethods[4] = MethodSpec.createMethodSpec("*.change(long,long)");
	}
	
	//Choice generator debugging
	@Override
	public void choiceGeneratorAdvanced (VM vm, ChoiceGenerator<?> currentCG) {
		if(ShadowInstructionFactory.debugCG){
			if(currentCG instanceof PCChoiceGenerator){
				System.out.println("PC CG at line "+((PCChoiceGenerator)currentCG).getInsn().getLineNumber()+" advanced, choice: "+currentCG.getNextChoice());
			}
		}
	}
	@Override
	public void choiceGeneratorSet (VM vm, ChoiceGenerator<?> newCG) {
		if(ShadowInstructionFactory.debugCG){
			if(newCG instanceof PCChoiceGenerator){
				System.out.println("Registered PC CG ("+newCG.getInsn()+") in execution mode: "+((PCChoiceGenerator)newCG).getExecutionMode()+" at line "+((PCChoiceGenerator)newCG).getInsn().getLineNumber());
			}
			else{
				System.out.println("Registered ChoiceGenerator of class: "+newCG.getClass());
			}
		}
	}
	@Override
	public void choiceGeneratorProcessed (VM vm, ChoiceGenerator<?> processedCG) {
		if(ShadowInstructionFactory.debugCG){
			if(processedCG instanceof PCChoiceGenerator){
				System.out.println("Processed PC CG from line "+((PCChoiceGenerator)processedCG).getInsn().getLineNumber());
			}
		 }
	 }
	
	
	@Override
	public void executeInstruction(VM vm, ThreadInfo currentThread, Instruction instructionToExecute) {
		Instruction insn = instructionToExecute;
		ThreadInfo ti = currentThread;
		
		//We might have to reset the execution mode to BOTH while executing a change(boolean,boolean) statement
		//See BytecodeUtils.getIfInsnExecutionMode()
		if(!BytecodeUtils.resetInstructions.isEmpty()){
			boolean reset = false;
			Execute currentExecutionMode = ti.getExecutionMode();
			for(Instruction i : BytecodeUtils.resetInstructions){
				if(insn.equals(i)){
					reset = true;
					ti.setExecutionMode(Execute.BOTH);
					if(ShadowInstructionFactory.debugChangeBoolean) System.out.println("Reset execution mode from " + currentExecutionMode + " to BOTH before executing "+insn.getMnemonic());
				}
			}
			if(reset) BytecodeUtils.resetInstructions.clear();
		}
		
		
		//Get the current choice generator
		ChoiceGenerator<?> curCg = ti.getVM().getSystemState().getChoiceGenerator();
		
		//Get the current PCChoiceGenerator and the current PathCondition
		PCChoiceGenerator pcCg;
		if(curCg instanceof PCChoiceGenerator){
			pcCg = (PCChoiceGenerator) curCg;
		}
		else{
			pcCg = curCg.getPreviousChoiceGeneratorOfType(PCChoiceGenerator.class); //also returns ShadowPCCg
		}
		
		PathCondition  pc = null;
		if(pcCg != null){
			pc = pcCg.getCurrentPC();
			
			//If we are about to execute an if-insn while exploring a diff path, we have to handle the special case if(change(boolean,boolean))
			//(specifically, skip the evaluation of the first (old) boolean expression since it affects the path condition)
			if(insn instanceof IfInstruction && pc.isDiffPC()){
				checkChangeDuringDiff(vm, ti, (IfInstruction)insn);				
			}
		}
		
		//If we are about to execute a return-insn, we can check whether a change() method has been executed
		if (insn instanceof JVMReturnInstruction){ 
			MethodInfo mi = insn.getMethodInfo();

			//Check whether the returned method matches with one of the change methods
			int index = -1;
			for(int i = 0; i < changeMethods.length; i++){
				if(changeMethods[i].matches(mi)){
					index = i;
					break;
				}
			}
			
			if(index != -1){ //change() detected
				processChangeMethod(index, ti, insn, pcCg, pc);
				return;
			}
		}
	}
	
	public void processChangeMethod(int index, ThreadInfo ti, Instruction insn, PCChoiceGenerator pcCg, PathCondition pc){
		//TODO: Change to use generic expressions and only distinguish between occupied stack slots
		StackFrame sf = ti.getModifiableTopFrame();
		pcCg.setCurrentPC(pc);
		
		switch(index){
		case 0: //Matched with change(boolean,boolean)
			//The evaluated boolean expressions (0->true, 1->false; it's the other way round bescause the generated bytecode always contains the negated if-insn)
			
			int old_value = sf.getLocalVariable(1);
			int new_value = sf.getLocalVariable(2);

			Object old_expr = sf.getLocalAttr(1);
			Object new_expr = sf.getLocalAttr(2);
			
			if(old_expr==null){
				old_expr = new IntegerConstant(old_value);
			}
			
			if(new_expr==null){
				new_expr = new IntegerConstant(new_value);
			}
			
			IntegerExpression result_b;
			
			//If we execute a change(boolean,boolean) while exploring a diff path, we only want to consider the value of the new expression
			if(pc.isDiffPC()){
				sf.setOperandAttr(new_expr);
				return;
			}
			
			if(!ti.isFirstStepInsn()){
				/* Choice 0: Follow concrete execution
				 * Choice 1: Diff true path
				 * Choice 2: Diff false path
				 */
				PCChoiceGenerator nextCg = new PCChoiceGenerator(0,2,1);
				nextCg.setOffset(insn.getPosition());
				nextCg.setMethodName(insn.getMethodInfo().getFullName());
				nextCg.setExecutionMode(Execute.BOTH);
				ti.getVM().getSystemState().setNextChoiceGenerator(nextCg);
				ti.skipInstruction(insn); //reexecute insn and break transition
				return;
			}
			else{
				PCChoiceGenerator curCg = (PCChoiceGenerator) ti.getVM().getSystemState().getChoiceGenerator();
				PCChoiceGenerator prevCg = curCg.getPreviousChoiceGeneratorOfType(PCChoiceGenerator.class);
				PathCondition nextPc;
				
				if(prevCg == null){
					nextPc = new PathCondition();
				}
				else{
					nextPc = prevCg.getCurrentPC();
				}
				
				int choice = curCg.getNextChoice();
				ti.setExecutionMode(curCg.getExecutionMode());
				assert(ti.getExecutionMode()==Execute.BOTH);
				
				switch(choice){
				case 0: //Concrete execution
					//If we are in a diff path, we don't do the concrete execution anymore
					if(old_value == new_value){
						result_b = new IntegerConstant(new_value);
						nextPc._addDet(Comparator.EQ, (IntegerExpression)new_expr, new_value);
						nextPc._addDet(Comparator.EQ, (IntegerExpression)old_expr, old_value);
						curCg.setCurrentPC(nextPc);
						sf.setOperandAttr(result_b);
						return;
					}
					else{
						/* Concrete executions diverge.
						 * Resulting operand attr is a DiffExpression containing the divergent values
						 * The IFEQ bytecode that is executed afterwards will detect this as a divergence.
						 */
						/*
						Diff diffType = (new_value == 0) ? Diff.diffFalse : Diff.diffTrue;
						System.out.println(this.getMnemonic()+": Concrete executions diverge at line "+insn.getLineNumber()+" ("+diffType+")");
						DiffExpression result_diff = new DiffExpression(new IntegerConstant(old_value), new IntegerConstant(new_value));
						sf.setOperandAttr(result_diff);
						*/
						
						//Possible divergence will be detected in the following choices
						curCg.setCurrentPC(nextPc);
						ti.getVM().getSystemState().setIgnored(true);
						return;
					}
				case 1: //Diff true
					DiffExpression result_diffTrue = new DiffExpression((IntegerExpression)old_expr,(IntegerExpression)new_expr);
					nextPc._addDet(Comparator.EQ, (IntegerExpression)new_expr, 1); //--> True
					nextPc._addDet(Comparator.EQ, (IntegerExpression)old_expr, 0); //--> False
					if(!nextPc.simplify()){
						ti.getVM().getSystemState().setIgnored(true);
					}
					curCg.setCurrentPC(nextPc);
					sf.setOperandAttr(result_diffTrue);
					return;
				case 2: //Diff false
					DiffExpression result_diffFalse = new DiffExpression((IntegerExpression)old_expr,(IntegerExpression)new_expr);
					nextPc._addDet(Comparator.EQ, (IntegerExpression)new_expr, 0); //--> False
					nextPc._addDet(Comparator.EQ, (IntegerExpression)old_expr, 1); //--> True
					if(!nextPc.simplify()){
						ti.getVM().getSystemState().setIgnored(true);
					}
					curCg.setCurrentPC(nextPc);
					sf.setOperandAttr(result_diffFalse);
					return;
				}
			}
		case 1:
			//Matched with change(int,int)
			
			//Get the current symbolic expressions of the two params, index 0 refers to 'this'
			Object op_v1 = sf.getLocalAttr(1);
			Object op_v2 = sf.getLocalAttr(2);
			
			int v1 = sf.getLocalVariable(1);
			int v2 = sf.getLocalVariable(2);
			
			
			DiffExpression result;
			IntegerExpression shadow_expr_i = BytecodeUtils.getShadowExpr(op_v1, v1);
			IntegerExpression sym_expr_i = BytecodeUtils.getSymbcExpr(op_v2, v2);
			
			result = new DiffExpression(shadow_expr_i,sym_expr_i);
			sf.setOperandAttr(result);
			return;
		
		case 2:
			//Matched with change(float,float) probably not needed as float operands will be casted to double if they are not used like 3.14f

			Object op_v3 = sf.getLocalAttr(1);
			Object op_v4 = sf.getLocalAttr(2);
			
			float v3 = sf.getFloatLocalVariable(1);
			float v4 = sf.getFloatLocalVariable(2);
			
			DiffExpression result_f;
			RealExpression shadow_expr_f = BytecodeUtils.getShadowExpr(op_v3, v3);
			RealExpression sym_expr_f = BytecodeUtils.getSymbcExpr(op_v4, v4);
			
			result_f = new DiffExpression(shadow_expr_f,sym_expr_f);
			sf.setOperandAttr(result_f);
			return;
			
		case 3:
			//Matched with change(double,double)
			Object op_v5 = sf.getLongLocalAttr(1);
			Object op_v6 = sf.getLongLocalAttr(3); //double and long operands occupy two consecutive attribute slots
			
			double v5 = sf.getDoubleLocalVariable(1);
			double v6 = sf.getDoubleLocalVariable(3);
			
			DiffExpression result_d;
			RealExpression shadow_expr_d = BytecodeUtils.getShadowExpr(op_v5, v5);
			RealExpression sym_expr_d = BytecodeUtils.getSymbcExpr(op_v6, v6);

			result_d = new DiffExpression(shadow_expr_d,sym_expr_d);
			sf.setLongOperandAttr(result_d);
			return;
			
		case 4:
			//Matched with change(long,long)
			Object op_v7 = sf.getLongLocalAttr(1);
			Object op_v8 = sf.getLongLocalAttr(3);
			
			long v7 = sf.getLongLocalVariable(1);
			long v8 = sf.getLongLocalVariable(3);

			DiffExpression result_l;
			IntegerExpression shadow_expr_l = BytecodeUtils.getShadowExpr(op_v7, v7);
			IntegerExpression sym_expr_l = BytecodeUtils.getSymbcExpr(op_v8, v8);
			
			result_l = new DiffExpression(shadow_expr_l,sym_expr_l);
			sf.setLongOperandAttr(result_l);
			return;default:
			//Should not happen
			assert(false);
			return;
		}
	}
	
	
	public void checkChangeDuringDiff(VM vm, ThreadInfo ti, IfInstruction insn){
		/*
		* We are currently exploring a diff path and are about to execute an if-instruction.
		* If the if-insn is executed in an if(change(boolean,boolean)) statement, 
		* we have to skip the evaluation of the first (the old) boolean expression,
		* since it would modify the path condition.
		*/
		//We only have to handle the case where the if-insn is executed in the OLD version of change(boolean,boolean)
		if(BytecodeUtils.getIfInsnExecutionMode(insn, ti) == Execute.OLD){
			ti.setExecutionMode(Execute.BOTH); //TODO: or NEW?
			//First search for bytecode pattern that pushes the old result on stack	
			MethodInfo mi = insn.getMethodInfo();
			Instruction first = insn;
			Instruction second = first.getNext();
			Instruction third = second.getNext();
			boolean foundPattern = false;
			while(!foundPattern){

				if(first instanceof ICONST && second instanceof GOTO && third instanceof ICONST){
					assert(!(third.getNext() instanceof JVMInvokeInstruction));
					foundPattern = true;
					
					//Skip the evaluation of the old boolean expression by removing the operands from the stack
					//and setting the next insn to the first insn that evaluates the new expression
					
					//These insns have two operands
					if(insn instanceof IF_ICMPEQ ||
						insn instanceof IF_ICMPGE ||
						insn instanceof IF_ICMPGT ||
						insn instanceof IF_ICMPLE ||
						insn instanceof IF_ICMPLT ||
						insn instanceof IF_ICMPNE){
							ti.getModifiableTopFrame().pop();	
						}
									
					ti.getModifiableTopFrame().pop();
					ti.getModifiableTopFrame().push(0);
							
					//Skip bytecode instruction to the evaluation of the new boolean expression
					ti.setNextPC(third.getNext());
					return;
				}
				else{
					first = second;
					second = third;
					third = third.getNext();
					assert(mi.containsLineNumber(third.getLineNumber()));
				}
			}
		}
	}				
							
		
	
	
	
}
