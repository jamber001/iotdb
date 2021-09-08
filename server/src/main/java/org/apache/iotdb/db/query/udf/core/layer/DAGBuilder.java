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

package org.apache.iotdb.db.query.udf.core.layer;

import org.apache.iotdb.db.exception.query.QueryProcessException;
import org.apache.iotdb.db.qp.physical.crud.UDTFPlan;
import org.apache.iotdb.db.query.expression.Expression;
import org.apache.iotdb.db.query.expression.ResultColumn;
import org.apache.iotdb.db.query.udf.core.reader.LayerPointReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DAGBuilder {

  private final int queryId;
  private final UDTFPlan udtfPlan;
  private final UDFLayer rawTimeSeriesInputLayer;

  // input
  private final List<Expression> resultColumnExpressions;
  // output
  private final LayerPointReader[] resultColumnPointReaders;

  private final LayerMemoryAssigner memoryAssigner;

  // all result column expressions will be split into several sub-expressions, each expression has
  // its own result point reader. different result column expressions may have the same
  // sub-expressions, but they can share the same point reader. we cache the point reader here to
  // make sure that only one point reader will be built for one expression.
  private final Map<Expression, IntermediateLayer> expressionIntermediateLayerMap;

  public DAGBuilder(int queryId, UDTFPlan udtfPlan, UDFLayer inputLayer, float memoryBudgetInMB) {
    this.queryId = queryId;
    this.udtfPlan = udtfPlan;
    this.rawTimeSeriesInputLayer = inputLayer;

    resultColumnExpressions = new ArrayList<>();
    for (ResultColumn resultColumn : udtfPlan.getResultColumns()) {
      resultColumnExpressions.add(resultColumn.getExpression());
    }
    resultColumnPointReaders = new LayerPointReader[resultColumnExpressions.size()];

    memoryAssigner = new LayerMemoryAssigner(memoryBudgetInMB);

    expressionIntermediateLayerMap = new HashMap<>();
  }

  public DAGBuilder buildLayerMemoryAssigner() {
    for (Expression expression : resultColumnExpressions) {
      memoryAssigner.increaseExpressionReference(expression);
      expression.updateStatisticsForMemoryAssigner(memoryAssigner);
    }
    memoryAssigner.build();
    return this;
  }

  public DAGBuilder buildResultColumnPointReaders() throws QueryProcessException, IOException {
    for (int i = 0; i < resultColumnExpressions.size(); ++i) {
      resultColumnPointReaders[i] =
          resultColumnExpressions
              .get(i)
              .constructIntermediateLayer(
                  queryId,
                  udtfPlan,
                  rawTimeSeriesInputLayer,
                  expressionIntermediateLayerMap,
                  memoryAssigner)
              .constructPointReader();
    }
    return this;
  }

  public LayerPointReader[] getResultColumnPointReaders() {
    return resultColumnPointReaders;
  }
}
