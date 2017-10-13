package gov.nasa.jpf.symbc.bytecode;



import gov.nasa.jpf.symbc.numeric.*;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;

/*
 * Implementation of the symbolic IINC bytecode instruction
 */

public class IINC extends gov.nasa.jpf.jvm.bytecode.IINC {
	public IINC(int localVarIndex, int incConstant){
		super(localVarIndex, incConstant);
	}
  @Override
  public Instruction execute (ThreadInfo th) {

    StackFrame sf = th.getModifiableTopFrame();
    Object op_v = sf.getLocalAttr(index);
    
    if (op_v == null) {
    	return super.execute(th); //operand is concrete
    }
    else {
    	int v = sf.getLocalVariable(index);
    	sf.setLocalVariable(index, v+increment, false);
    	
    	//symbolic and shadow expressions of the operand and result
    	IntegerExpression sym_v = BytecodeUtils.getSymbcExpr(op_v, v);
    	IntegerExpression shadow_v = BytecodeUtils.getShadowExpr(op_v, v);
    	
    	IntegerExpression sym_result = sym_v._plus(increment);
    	IntegerExpression shadow_result = shadow_v._plus(increment);
    	
    	if(op_v instanceof DiffExpression){
    		DiffExpression result = new DiffExpression(shadow_result,sym_result);
    		sf.setLocalAttr(index, result);
    	}
    	else{
    		IntegerExpression result = (IntegerExpression) sym_result;
    		sf.setLocalAttr(index,result);
    	}

    	//System.out.println("IINC "+sf.getLocalAttr(index));
    }
    return getNext(th);
  }

}
