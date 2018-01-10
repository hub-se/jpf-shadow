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
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.ThreadInfo.Execute;
import gov.nasa.jpf.vm.Types;

public class DDIV extends gov.nasa.jpf.jvm.bytecode.DDIV {

	@Override
	public Instruction execute (ThreadInfo ti) {
		StackFrame sf = ti.getModifiableTopFrame();
		Object op_v1 = sf.getOperandAttr(1);
		Object op_v2 = sf.getOperandAttr(3);

		if(op_v1==null && op_v2==null && ti.getExecutionMode() == Execute.BOTH){
			return super.execute(ti); // we'll still do the concrete execution
		}
		/*
		 * Either we have at least one symbolic operand and/or we execute only
		 * the old or only the new version (which results in divergent results).
		 * In both cases, we should check whether the denominator can be zero,
		 * which affects the path condition.
		 */
		
		if (!ti.isFirstStepInsn()) { // first time around
			PCChoiceGenerator nextCg;
			if(ti.getExecutionMode() ==Execute.BOTH){
				nextCg = new PCChoiceGenerator(4);
			}
			else{
				//if we only execute one version, we only have to check for == 0 and != 0
				nextCg = new PCChoiceGenerator(2);
			}
			
			nextCg.setOffset(this.position);
			nextCg.setMethodName(this.getMethodInfo().getFullName());
			nextCg.setExecutionMode(ti.getExecutionMode());
			ti.getVM().setNextChoiceGenerator(nextCg);
			return this;
		} 
		else{  // this is what really returns results
			double v1 = Types.longToDouble(sf.popLong());
			double v2 = Types.longToDouble(sf.popLong());
			sf.pushLong(0);
			
			//Symbolic and shadow expressions of the operands and the result
			RealExpression sym_v1 = BytecodeUtils.getSymbcExpr(op_v1, v1); 
			RealExpression shadow_v1 = BytecodeUtils.getShadowExpr(op_v1, v1); 
			
			RealExpression sym_v2 = BytecodeUtils.getSymbcExpr(op_v2, v2); 
			RealExpression shadow_v2 = BytecodeUtils.getShadowExpr(op_v2, v2); 
			
			RealExpression sym_result = null;
			RealExpression shadow_result = null;
			
			PCChoiceGenerator curCg = (PCChoiceGenerator) ti.getVM().getChoiceGenerator();
			PathCondition pc;
			PCChoiceGenerator prevCg = curCg.getPreviousChoiceGeneratorOfType(PCChoiceGenerator.class);
			
			//get current path condition
			if(prevCg == null){
				pc = new PathCondition();
			}
			else{
				pc = prevCg.getCurrentPC();
			}
			assert(pc != null);
			
			//Restore execution mode
			ti.setExecutionMode(curCg.getExecutionMode());
			int choice = curCg.getNextChoice();
			switch(choice){
			case 0: //denominator is zero --> throw arithmetic exception
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
					pc.markAsDiffPC(this.getLineNumber(),Diff.divByZero); //technically not a diffpath, just to generate test case
					curCg.setCurrentPC(pc);
					return ti.createAndThrowException("java.lang.ArithmeticException","div by 0");
				}
				
			case 1: //denominator is not zero --> set result and continue normally
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
					/*
					 * We might want to determine results based on the execution mode.
					 * However, the StackFrame already considers the execution mode when
					 * propagating values.
					 */
					curCg.setCurrentPC(pc);
					sym_result = sym_v2._div(sym_v1);
					shadow_result = shadow_v2._div(shadow_v1);
					if((op_v1 instanceof DiffExpression) || (op_v2 instanceof DiffExpression)){
						DiffExpression result = new DiffExpression(shadow_result,sym_result);
						sf.setLongOperandAttr(result);
					}
					else{
						RealExpression result = sym_result;
						sf.setLongOperandAttr(result);
					}
					
				}
				return this.getNext(ti);
			case 2: //"True" diff, new denominator is zero while old is not --> regression
				pc._addDet(Comparator.EQ, sym_v1, 0);
				pc._addDet(Comparator.NE, shadow_v1, 0);
				
				if(!pc.simplify()){
					ti.getVM().getSystemState().setIgnored(true);
					return this.getNext(ti);
				}
				else{
					pc.markAsDiffPC(this.getLineNumber(),Diff.divByZero); //technically not a diffpath, just to generate test case
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
					pc.markAsDiffPC(this.getLineNumber(),Diff.divByZero); //technically not a diffpath, just to generate test case
					curCg.setCurrentPC(pc);
					
					sym_result = sym_v2._div(sym_v1);
					shadow_result = new RealConstant(0); //old is NaN
						
					if((op_v1 instanceof DiffExpression) || (op_v2 instanceof DiffExpression)){
						DiffExpression result = new DiffExpression(shadow_result,sym_result);
						sf.setLongOperandAttr(result);
					}
					else{
						RealExpression result = sym_result;
						sf.setLongOperandAttr(result);
					}
				}
				return this.getNext(ti);	
			default:
				assert(false);
				return this;
			}	
		}
	}
}