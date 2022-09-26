/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.pinot.core.data.table;

import org.apache.pinot.common.data.table.Record;


/**
 * Helper class to store the values to be ordered. It also wraps the Key and Record of the record.
 * - When ordering on an aggregation, stores the final result of the aggregation
 * - When ordering on a column/transform, stores the actual value of the expression
 */
@SuppressWarnings("rawtypes")
public class IntermediateRecord {
  public final Key _key;
  public final Record _record;
  public final Comparable[] _values;

  IntermediateRecord(Key key, Record record, Comparable[] values) {
    _key = key;
    _record = record;
    _values = values;
  }
}
