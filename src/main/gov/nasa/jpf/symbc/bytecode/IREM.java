package gov.nasa.jpf.symbc.bytecode;



import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;

public class IREM extends gov.nasa.jpf.jvm.bytecode.IREM {

	@Override
	public Instruction execute (ThreadInfo th) {
		StackFrame sf = th.getModifiableTopFrame();
		Object op_v1 = sf.getOperandAttr(0);
		Object op_v2 = sf.getOperandAttr(1);

		if(op_v1==null && op_v2==null){
			return super.execute(th); // we'll still do the concrete execution
		}
		else{
			throw new UnsupportedOperationException();
		}
		

	}
}