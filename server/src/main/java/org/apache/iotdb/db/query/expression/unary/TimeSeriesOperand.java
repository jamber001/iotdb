/*
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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.iotdb.db.query.expression.unary;

import org.apache.iotdb.db.exception.query.LogicalOptimizeException;
import org.apache.iotdb.db.exception.query.QueryProcessException;
import org.apache.iotdb.db.metadata.PartialPath;
import org.apache.iotdb.db.qp.physical.crud.UDTFPlan;
import org.apache.iotdb.db.qp.utils.WildcardsRemover;
import org.apache.iotdb.db.query.expression.Expression;
import org.apache.iotdb.db.query.udf.core.layer.IntermediateLayer;
import org.apache.iotdb.db.query.udf.core.layer.UDFLayer;
import org.apache.iotdb.tsfile.file.metadata.enums.TSDataType;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class TimeSeriesOperand extends Expression {

  protected PartialPath path;
  protected TSDataType dataType;

  public TimeSeriesOperand(PartialPath path) {
    this.path = path;
  }

  public PartialPath getPath() {
    return path;
  }

  public void setPath(PartialPath path) {
    this.path = path;
  }

  @Override
  public void concat(List<PartialPath> prefixPaths, List<Expression> resultExpressions) {
    for (PartialPath prefixPath : prefixPaths) {
      resultExpressions.add(new TimeSeriesOperand(prefixPath.concatPath(path)));
    }
  }

  @Override
  public void removeWildcards(WildcardsRemover wildcardsRemover, List<Expression> resultExpressions)
      throws LogicalOptimizeException {
    for (PartialPath actualPath : wildcardsRemover.removeWildcardFrom(path)) {
      resultExpressions.add(new TimeSeriesOperand(actualPath));
    }
  }

  @Override
  public void collectPaths(Set<PartialPath> pathSet) {
    pathSet.add(path);
  }

  @Override
  public IntermediateLayer constructIntermediateLayer(
      UDTFPlan udtfPlan,
      UDFLayer rawTimeSeriesInputLayer,
      Map<Expression, IntermediateLayer> expressionIntermediateLayerMap)
      throws QueryProcessException {
    if (!expressionIntermediateLayerMap.containsKey(this)) {
      //      expressionIntermediateLayerMap.put(
      //          this,
      //          new SingleInputColumnMultiReferenceIntermediateLayer(
      //              rawTimeSeriesInputLayer.constructPointReader(
      //                  udtfPlan.getReaderIndex(path.getFullPath())),
      //              -1,
      //              -1));
    }

    return expressionIntermediateLayerMap.get(this);
  }

  @Override
  public String toString() {
    return path.isMeasurementAliasExists() ? path.getFullPathWithAlias() : path.getFullPath();
  }
}
