/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.hive.ql.exec.vector.expressions;

import org.apache.hadoop.hive.ql.exec.vector.ColumnVector;
import org.apache.hadoop.hive.ql.exec.vector.ListColumnVector;
import org.apache.hadoop.hive.ql.exec.vector.VectorExpressionDescriptor;
import org.apache.hadoop.hive.ql.exec.vector.VectorizedRowBatch;

/**
 * Vectorized instruction to get an element from a list with a scalar index and put
 * the result in an output column.
 */
public class ListIndexColScalar extends VectorExpression {
  private static final long serialVersionUID = 1L;

  private int listColumnNum;
  private int index;

  public ListIndexColScalar() {
    super();
  }

  public ListIndexColScalar(int listColumn, int index, int outputColumnNum) {
    super(outputColumnNum);
    this.listColumnNum = listColumn;
    this.index = index;
  }

  @Override
  public void evaluate(VectorizedRowBatch batch) {
    if (childExpressions != null) {
      super.evaluateChildren(batch);
    }

    ColumnVector outV = batch.cols[outputColumnNum];
    ListColumnVector listV = (ListColumnVector) batch.cols[listColumnNum];
    ColumnVector childV = listV.child;

    outV.noNulls = true;
    if (listV.isRepeating) {
      if (listV.isNull[0]) {
        outV.isNull[0] = true;
        outV.noNulls = false;
      } else {
        if (index >= listV.lengths[0]) {
          outV.isNull[0] = true;
          outV.noNulls = false;
        } else {
          outV.setElement(0, (int) (listV.offsets[0] + index), childV);
          outV.isNull[0] = false;
        }
      }
      outV.isRepeating = true;
    } else {
      for (int i = 0; i < batch.size; i++) {
        int j = (batch.selectedInUse) ? batch.selected[i] : i;
        if (listV.isNull[j] || index >= listV.lengths[j]) {
          outV.isNull[j] = true;
          outV.noNulls = false;
        } else {
          outV.setElement(j, (int) (listV.offsets[j] + index), childV);
          outV.isNull[j] = false;
        }
      }
      outV.isRepeating = false;
    }
  }

  @Override
  public String vectorExpressionParameters() {
    return getColumnParamString(0, listColumnNum) + ", " + getColumnParamString(1, index);
  }

  @Override
  public VectorExpressionDescriptor.Descriptor getDescriptor() {
    return (new VectorExpressionDescriptor.Builder())
        .setMode(
            VectorExpressionDescriptor.Mode.PROJECTION)
        .setNumArguments(2)
        .setArgumentTypes(
            VectorExpressionDescriptor.ArgumentType.LIST,
            VectorExpressionDescriptor.ArgumentType.INT_FAMILY)
        .setInputExpressionTypes(
            VectorExpressionDescriptor.InputExpressionType.COLUMN,
            VectorExpressionDescriptor.InputExpressionType.SCALAR).build();
  }
}
