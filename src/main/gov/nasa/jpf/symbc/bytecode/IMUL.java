package gov.nasa.jpf.symbc.bytecode;


import gov.nasa.jpf.symbc.numeric.*;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;

/*
 * Implementation of the shadow symbolic IMUL bytecode instruction
 */

public class IMUL extends gov.nasa.jpf.jvm.bytecode.IMUL {
	@Override
	public Instruction execute (ThreadInfo ti) {

		StackFrame sf = ti.getModifiableTopFrame();
		Object op_v1 = sf.getOperandAttr(0); 
		Object op_v2 = sf.getOperandAttr(1);
		
		if(op_v1==null && op_v2==null){
			return super.execute(ti); //both operands are concrete
		}
		else {
			//Pop (concrete) operands from operand stack and push result
			int v1 = sf.pop();
			int v2 = sf.pop();
			sf.push(v1*v2,false); //for symbolic expressions, the concrete value actually does not matter
			
			//Get symbolic and shadow expressions from the operands
			IntegerExpression sym_v1 = BytecodeUtils.getSymbcExpr(op_v1, v1);
			IntegerExpression shadow_v1 = BytecodeUtils.getShadowExpr(op_v1, v1); 
			
			IntegerExpression sym_v2 = BytecodeUtils.getSymbcExpr(op_v2, v2);
			IntegerExpression shadow_v2 = BytecodeUtils.getShadowExpr(op_v2, v2);
			
			//Calculate resulting expressions
			IntegerExpression sym_result = sym_v1._mul(sym_v2);
			IntegerExpression shadow_result = shadow_v1._mul(shadow_v2);  
			
			//Set result
			//If at least one of the operands was a DiffExpression, the result will also be an DiffExpression
			if(op_v1 instanceof DiffExpression || op_v2 instanceof DiffExpression){
				DiffExpression result = new DiffExpression(shadow_result,sym_result);
				sf.setOperandAttr(result);
			}
			else{
				//shadow_result and sym_result are equal, so we just store one of them
				IntegerExpression result = (IntegerExpression) sym_result;
				sf.setOperandAttr(result);
			}
		}
		//System.out.println("Execute IADD: "+result);	
		return getNext(ti);
	}
}
