/*
 * Copyright (C) 2014, United States Government, as represented by the
 * Administrator of the National Aeronautics and Space Administration.
 * All rights reserved.
 *
 * Symbolic Pathfinder (jpf-symbc) is licensed under the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0. 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

//Copyright (C) 2007 United States Government as represented by the
//Administrator of the National Aeronautics and Space Administration
//(NASA).  All Rights Reserved.

//This software is distributed under the NASA Open Source Agreement
//(NOSA), version 1.3.  The NOSA has been approved by the Open Source
//Initiative.  See the file NOSA-1.3-JPF at the top of the distribution
//directory tree for the complete NOSA document.

//THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
//KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
//LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
//SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
//A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
//THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
//DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.

package gov.nasa.jpf.symbc.bytecode;

import gov.nasa.jpf.symbc.numeric.*;
import gov.nasa.jpf.symbc.numeric.PathCondition.Diff;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.Types;
import gov.nasa.jpf.vm.ThreadInfo.Execute;



public class DCMPL extends gov.nasa.jpf.jvm.bytecode.DCMPL{

	@Override
	public Instruction execute (ThreadInfo ti) {
		StackFrame sf = ti.getModifiableTopFrame();

		Object op_v1 = sf.getOperandAttr(1);
		Object op_v2 = sf.getOperandAttr(3);
		
		// Map (choice+1) to condition value, choice 2 is concrete execution
		// choices < 2 are paths where both versions yield same results and choices > 2 are divergent results
		int[] oldConditionValues = {-1, 0, 1, 2,-1,-1, 0, 0, 1, 1};
		int[] newConditionValues = {-1, 0, 1, 2,-1, 1,-1, 1,-1, 0};

		if ((op_v1 == null) && (op_v2 == null)) { // both conditions are concrete
			//System.out.println("Execute IF_ICMPEQ: The conditions are concrete");
			return super.execute(ti);
		}else{ // at least one condition is symbolic
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
						
				//When exploring a diff path, we only need to consider the choices -1, 0 and 1 since we only execute the new version.
				if(pc.isDiffPC()){
					nextCg = new PCChoiceGenerator(-1,1,1);
				}
				else{
					/*
					 * When evaluating an if(change(boolean,boolean)) stmt, each parameter is evaluated separately so we cannot directly 
					 * check for divergences. When evaluating the old expression, we use the choices -1,0,1,2. However, when evaluating
					 * the new expression, we only use choice 2 (concrete execution) if we also used concrete values for the old expression.
					 * Otherwise, we only need to follow the true and false path in the new version.
					 */
					if(BytecodeUtils.isChangeBoolean(this, ti)){
						if(curPcCg.getExecutionMode() == Execute.BOTH){
							//Evaluating the old expression
							nextCg = new PCChoiceGenerator(2,-1,-1);
						}
						else{
							//Evaluating the new expression
							if(curPcCg.getNextChoice() == 2){
								//Concrete values used for old expression
								nextCg = new PCChoiceGenerator(2,2,1);
							}
							else{
								nextCg = new PCChoiceGenerator(-1,1,1);
							}
						}
					}
					else{
						//The standard case: Until we detect a divergence, we follow the concrete execution and check for divergences along the path (choices 2 to 8).
						nextCg = new PCChoiceGenerator(2,8,1);
					}
				}
				nextCg.setOffset(this.position);
				nextCg.setMethodName(this.getMethodInfo().getFullName());
				nextCg.setExecutionMode(ti.getExecutionMode());
				ti.getVM().getSystemState().setNextChoiceGenerator(nextCg);
				return this;
			}
			else{
				//This actually returns the next instruction
				PCChoiceGenerator curCg = (PCChoiceGenerator) ti.getVM().getSystemState().getChoiceGenerator();
				double v1 = Types.longToDouble(sf.popLong());
				double v2 = Types.longToDouble(sf.popLong());
				
				PathCondition pc;
				PCChoiceGenerator prevCg = curCg.getPreviousChoiceGeneratorOfType(PCChoiceGenerator.class);
				
				if(prevCg == null){
					pc = new PathCondition();
				}
				else{
					pc = prevCg.getCurrentPC();
				}
				
				assert(pc != null);
				
				RealExpression sym_v1 = BytecodeUtils.getSymbcExpr(op_v1, v1);
				RealExpression shadow_v1 = BytecodeUtils.getShadowExpr(op_v1, v1);
				
				RealExpression sym_v2 = BytecodeUtils.getSymbcExpr(op_v2, v2);
				RealExpression shadow_v2 = BytecodeUtils.getShadowExpr(op_v2, v2);
				
				ti.setExecutionMode(curCg.getExecutionMode());
				
				// Note that the minimum choice is -1
				int choice = curCg.getNextChoice() + 1;
				
				// Symbolic execution
				if(choice != 3){
					int newConditionValue = newConditionValues[choice];
					int oldConditionValue = oldConditionValues[choice];
					
					if(ti.getExecutionMode() == Execute.NEW || ti.getExecutionMode() == Execute.BOTH){
						addDet(newConditionValue, pc, sym_v2, sym_v1);
					}
					
					if(ti.getExecutionMode() == Execute.OLD || ti.getExecutionMode() == Execute.BOTH){
						if(!pc.isDiffPC()){
							addDet(oldConditionValue, pc, shadow_v2, shadow_v1);
						}
					}
					
					if(!pc.simplify()){
						//path not feasible
						ti.getVM().getSystemState().setIgnored(true);
					}
					else{
						curCg.setCurrentPC(pc);
					}
					sf.push(newConditionValue, false);
					
					DiffExpression result = new DiffExpression(new IntegerConstant(oldConditionValue),
																	new IntegerConstant(newConditionValue));
					sf.setOperandAttr(result);
				}
				else{
					// Concrete execution
					
					for(int i = -1; i <= 1; i++){
						// Executing both versions
						if(ti.getExecutionMode() == Execute.BOTH){
							
							PathCondition old_pc = pc.make_copy();
							PathCondition new_pc = pc.make_copy();
							
							// Check whether both versions yield the same result
							addDet(i, new_pc, sym_v2, sym_v1);
							addDet(i, old_pc, shadow_v2, shadow_v1);
							
							// "Concrete execution"
							BytecodeUtils.addConcreteValues(old_pc);
							BytecodeUtils.addConcreteValues(new_pc);
							
							boolean old_result = old_pc.simplify();
							boolean new_result = new_pc.simplify();
							
							if(old_result && new_result){
								addDet(i, pc, sym_v2, sym_v1);
								addDet(i, pc, shadow_v2, shadow_v1);
								curCg.setCurrentPC(pc);
								sf.push(i, false);
								return this.getNext(ti);
							}
						}
						else{ // Executing the old or new expression in an if(change(boolean,boolean)) stmt
							RealExpression expr_v1 = (ti.getExecutionMode() == Execute.OLD) ? shadow_v1 : sym_v1;
							RealExpression expr_v2 = (ti.getExecutionMode() == Execute.OLD) ? shadow_v2 : sym_v2;
							
							PathCondition next_pc = pc.make_copy();
							BytecodeUtils.addConcreteValues(next_pc);
							
							addDet(i, next_pc, expr_v2, expr_v1);
							if(next_pc.simplify()){
								addDet(i, pc, expr_v2, expr_v1);
								curCg.setCurrentPC(pc);
								sf.push(i, false);
								return this.getNext(ti);
							}
						
							//At least one of the condition values must result in a satisfiable pc
							assert(i != 1);
						}
					}
					
					// Both versions yield different condition values
					//We ignore this state since one of the following choices should detect this divergence as well (but might generate a different test case)
					//TODO: skip the respective choice instead
					System.out.println(this.getMnemonic()+": Concrete executions diverge at line "+this.getLineNumber());
					ti.getVM().getSystemState().setIgnored(true);
				}
				return this.getNext(ti);
			}
		}
	}
	
	private void addDet(int conditionValue, PathCondition pc, RealExpression v2, RealExpression v1){
		switch(conditionValue){
		case -1:
			pc._addDet(Comparator.LT, v2, v1);
			break;
		case 0:
			pc._addDet(Comparator.EQ, v2, v1);
			break;
		case 1:
			pc._addDet(Comparator.GT, v2, v1);
		}
	}
}