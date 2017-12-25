
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//package org.apache.commons.math.linear;
package org.apache.commons.math;

//import java.io.Serializable;

/*
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.exception.NumberIsTooLargeException;
import org.apache.commons.math.util.FastMath;
import org.apache.commons.math.util.OpenIntToDoubleHashMap;
*/

/**
 * Sparse matrix implementation based on an open addressed map.
 *
 * @version $Id$
 * @since 2.0
 */
public class OpenMapRealMatrix 
	//extends AbstractRealMatrix	
    //implements SparseRealMatrix, Serializable 
	{
	private boolean change(boolean oldVal, boolean newVal){return newVal;}
	private final int INT_MAXVALUE = 100;
	
    /** Serializable version identifier. */
    //private static final long serialVersionUID = -5962461716457143437L;
    /** Number of rows of the matrix. */
    private final int rows;
    /** Number of columns of the matrix. */
    private final int columns;
    /** Storage for (sparse) matrix elements. */
    private final OpenIntToDoubleHashMap entries;

    /**
     * Build a sparse matrix with the supplied row and column dimensions.
     *
     * @param rowDimension Number of rows of the matrix.
     * @param columnDimension Number of columns of the matrix.
     */
    public OpenMapRealMatrix(int rowDimension, int columnDimension) {
    	//super constructor executes same checks as below
    	//super(rowDimension, columnDimension);
        if (rowDimension < 1) {
        	//use built-in exception to keep code compact
            //throw new NotStrictlyPositiveException(rowDimension);
        	throw new IllegalArgumentException("rowDimension not strictly positive.");
        }
        if (columnDimension < 1) {
            //throw new NotStrictlyPositiveException(columnDimension);
        	throw new IllegalArgumentException("columnDimension not strictly positive.");
        }
        
        if(change(false,true)){
	        int lRow =  rowDimension;
	        int lCol =  columnDimension;
	        if (lRow * lCol >  INT_MAXVALUE) {
	            //throw new NumberIsTooLargeException(lRow * lCol, Integer.MAX_VALUE, false);
	        	throw new ArithmeticException();
	        }
        }
        this.rows = rowDimension;
        this.columns = columnDimension;
        this.entries = new OpenIntToDoubleHashMap(0.0);
    }
    
    //Test driver
    static void test(int rowDimension, int columnDimension){
    	OpenMapRealMatrix m = new OpenMapRealMatrix(rowDimension, columnDimension);
    }
    static void main(String[] args){
    	test(10,20);
    }

}


