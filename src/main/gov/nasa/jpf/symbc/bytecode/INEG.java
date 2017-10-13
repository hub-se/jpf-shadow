package gov.nasa.jpf.symbc.bytecode;


import gov.nasa.jpf.symbc.numeric.*;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;

/*
 * Implementation of the shadow symbolic INEG bytecode instruction
 */

public class INEG extends gov.nasa.jpf.jvm.bytecode.INEG{
	
	@Override
	public Instruction execute (ThreadInfo th) {
		
		StackFrame sf = th.getModifiableTopFrame();
		Object op_v = sf.getOperandAttr();
		
		int v = sf.pop();
		sf.push(-v, false);

		if(op_v != null){
			IntegerExpression sym_v = BytecodeUtils.getSymbcExpr(op_v, v);
			IntegerExpression shadow_v = BytecodeUtils.getShadowExpr(op_v, v);
			
			IntegerExpression sym_result = sym_v._neg();
			IntegerExpression shadow_result = shadow_v._neg();
	
			if(op_v instanceof DiffExpression){
				DiffExpression result = new DiffExpression(shadow_result,sym_result);
				sf.setOperandAttr(result);
			}
			else{
				IntegerExpression result = (IntegerExpression) sym_result;
				sf.setOperandAttr(result);
			}	
			
		}		
		
		return getNext(th);
	}
}
