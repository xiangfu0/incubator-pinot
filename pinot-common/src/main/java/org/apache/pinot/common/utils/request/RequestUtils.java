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
package org.apache.pinot.common.utils.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.calcite.sql.SqlAsOperator;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlExplain;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlJoin;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNumericLiteral;
import org.apache.calcite.sql.SqlOrderBy;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.SqlSetOption;
import org.apache.calcite.sql.SqlWith;
import org.apache.calcite.sql.SqlWithItem;
import org.apache.commons.lang3.StringUtils;
import org.apache.pinot.common.request.DataSource;
import org.apache.pinot.common.request.Expression;
import org.apache.pinot.common.request.ExpressionType;
import org.apache.pinot.common.request.Function;
import org.apache.pinot.common.request.Identifier;
import org.apache.pinot.common.request.Literal;
import org.apache.pinot.common.request.PinotQuery;
import org.apache.pinot.spi.utils.BytesUtils;
import org.apache.pinot.spi.utils.CommonConstants;
import org.apache.pinot.sql.FilterKind;
import org.apache.pinot.sql.parsers.CalciteSqlParser;
import org.apache.pinot.sql.parsers.SqlCompilationException;
import org.apache.pinot.sql.parsers.SqlNodeAndOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RequestUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(RequestUtils.class);
  private static final JsonNode EMPTY_OBJECT_NODE = new ObjectMapper().createObjectNode();

  private RequestUtils() {
  }

  public static SqlNodeAndOptions parseQuery(String query)
      throws SqlCompilationException {
    return parseQuery(query, EMPTY_OBJECT_NODE);
  }

  public static SqlNodeAndOptions parseQuery(String query, JsonNode request)
      throws SqlCompilationException {
    long parserStartTimeNs = System.nanoTime();
    SqlNodeAndOptions sqlNodeAndOptions = CalciteSqlParser.compileToSqlNodeAndOptions(query);
    setOptions(sqlNodeAndOptions, request);
    sqlNodeAndOptions.setParseTimeNs(System.nanoTime() - parserStartTimeNs);
    return sqlNodeAndOptions;
  }

  /**
   * Sets extra options for the given query.
   */
  @VisibleForTesting
  public static void setOptions(SqlNodeAndOptions sqlNodeAndOptions, JsonNode jsonRequest) {
    Map<String, String> queryOptions = new HashMap<>();
    if (jsonRequest.has(CommonConstants.Broker.Request.DEBUG_OPTIONS)) {
      Map<String, String> debugOptions = RequestUtils.getOptionsFromJson(jsonRequest,
          CommonConstants.Broker.Request.DEBUG_OPTIONS);
      // TODO: remove debug options after releasing 0.11.0.
      if (!debugOptions.isEmpty()) {
        // NOTE: Debug options are deprecated. Put all debug options into query options for backward compatibility.
        LOGGER.debug("Debug options are set to: {}", debugOptions);
        queryOptions.putAll(debugOptions);
      }
    }
    if (jsonRequest.has(CommonConstants.Broker.Request.QUERY_OPTIONS)) {
      Map<String, String> queryOptionsFromJson = RequestUtils.getOptionsFromJson(jsonRequest,
          CommonConstants.Broker.Request.QUERY_OPTIONS);
      queryOptions.putAll(queryOptionsFromJson);
    }
    boolean enableTrace = jsonRequest.has(CommonConstants.Broker.Request.TRACE) && jsonRequest.get(
        CommonConstants.Broker.Request.TRACE).asBoolean();
    if (enableTrace) {
      queryOptions.put(CommonConstants.Broker.Request.TRACE, "true");
    }
    if (!queryOptions.isEmpty()) {
      LOGGER.debug("Query options are set to: {}", queryOptions);
    }
    // TODO: Remove the SQL query options after releasing 0.11.0
    // The query engine will break if these 2 options are missing during version upgrade.
    queryOptions.put(CommonConstants.Broker.Request.QueryOptionKey.GROUP_BY_MODE, CommonConstants.Broker.Request.SQL);
    queryOptions.put(CommonConstants.Broker.Request.QueryOptionKey.RESPONSE_FORMAT, CommonConstants.Broker.Request.SQL);
    // Setting all query options back into SqlNodeAndOptions. The above ordering matters due to priority overwrite rule
    sqlNodeAndOptions.setExtraOptions(queryOptions);
  }

  public static Expression getIdentifierExpression(String identifier) {
    Expression expression = new Expression(ExpressionType.IDENTIFIER);
    expression.setIdentifier(new Identifier(identifier));
    return expression;
  }

  public static Expression getLiteralExpression(SqlLiteral node) {
    Expression expression = new Expression(ExpressionType.LITERAL);
    Literal literal = new Literal();
    if (node instanceof SqlNumericLiteral) {
      // TODO: support different integer and floating point type.
      // Mitigate calcite NPE bug, we need to check if SqlNumericLiteral.getScale() is null before calling
      // SqlNumericLiteral.isInteger(). TODO: Undo this fix once a Calcite release that contains CALCITE-4199 is
      // available and Pinot has been upgraded to use such a release.
      SqlNumericLiteral sqlNumericLiteral = (SqlNumericLiteral) node;
      if (sqlNumericLiteral.getScale() != null && sqlNumericLiteral.isInteger()) {
        literal.setLongValue(node.bigDecimalValue().longValue());
      } else {
        literal.setDoubleValue(node.bigDecimalValue().doubleValue());
      }
    } else {
      switch (node.getTypeName()) {
        case BOOLEAN:
          literal.setBoolValue(node.booleanValue());
          break;
        case NULL:
          literal.setNullValue(true);
          break;
        default:
          literal.setStringValue(StringUtils.replace(node.toValue(), "''", "'"));
          break;
      }
    }
    expression.setLiteral(literal);
    return expression;
  }

  public static Expression createNewLiteralExpression() {
    Expression expression = new Expression(ExpressionType.LITERAL);
    Literal literal = new Literal();
    expression.setLiteral(literal);
    return expression;
  }

  public static Expression getLiteralExpression(boolean value) {
    Expression expression = createNewLiteralExpression();
    expression.getLiteral().setBoolValue(value);
    return expression;
  }

  public static Expression getLiteralExpression(long value) {
    Expression expression = createNewLiteralExpression();
    expression.getLiteral().setLongValue(value);
    return expression;
  }

  public static Expression getLiteralExpression(double value) {
    Expression expression = createNewLiteralExpression();
    expression.getLiteral().setDoubleValue(value);
    return expression;
  }

  public static Expression getLiteralExpression(String value) {
    Expression expression = createNewLiteralExpression();
    expression.getLiteral().setStringValue(value);
    return expression;
  }

  public static Expression getLiteralExpression(byte[] value) {
    Expression expression = createNewLiteralExpression();
    expression.getLiteral().setStringValue(BytesUtils.toHexString(value));
    return expression;
  }

  public static Expression getNullLiteralExpression() {
    Expression expression = createNewLiteralExpression();
    expression.getLiteral().setNullValue(true);
    return expression;
  }

  public static Expression getLiteralExpression(Object object) {
    if (object == null) {
      return getNullLiteralExpression();
    }
    if (object instanceof Integer || object instanceof Long) {
      return RequestUtils.getLiteralExpression(((Number) object).longValue());
    }
    if (object instanceof Float || object instanceof Double) {
      return RequestUtils.getLiteralExpression(((Number) object).doubleValue());
    }
    if (object instanceof byte[]) {
      return RequestUtils.getLiteralExpression((byte[]) object);
    }
    if (object instanceof Boolean) {
      return RequestUtils.getLiteralExpression(((Boolean) object).booleanValue());
    }
    return RequestUtils.getLiteralExpression(object.toString());
  }

  public static Expression getFunctionExpression(String canonicalName) {
    assert canonicalName.equalsIgnoreCase(canonicalizeFunctionNamePreservingSpecialKey(canonicalName));
    Expression expression = new Expression(ExpressionType.FUNCTION);
    Function function = new Function(canonicalName);
    expression.setFunctionCall(function);
    return expression;
  }

  /**
   * Converts the function name into its canonical form.
   */
  public static String canonicalizeFunctionName(String functionName) {
    return StringUtils.remove(functionName, '_').toLowerCase();
  }

  private static final Map<String, String> CANONICAL_NAME_TO_SPECIAL_KEY_MAP;

  static {
    CANONICAL_NAME_TO_SPECIAL_KEY_MAP = new HashMap<>();
    for (FilterKind filterKind : FilterKind.values()) {
      CANONICAL_NAME_TO_SPECIAL_KEY_MAP.put(canonicalizeFunctionName(filterKind.name()), filterKind.name());
    }
    CANONICAL_NAME_TO_SPECIAL_KEY_MAP.put("stdistance", "st_distance");
  }

  /**
   * Converts the function name into its canonical form, but preserving the special keys.
   * - Keep FilterKind.name() as is because we need to read the FilterKind via FilterKind.valueOf().
   * - Keep ST_Distance as is because we use exact match when applying geo-spatial index up to release 0.10.0.
   * TODO: Remove the ST_Distance special handling after releasing 0.11.0.
   */
  public static String canonicalizeFunctionNamePreservingSpecialKey(String functionName) {
    String canonicalName = canonicalizeFunctionName(functionName);
    return CANONICAL_NAME_TO_SPECIAL_KEY_MAP.getOrDefault(canonicalName, canonicalName);
  }

  public static String prettyPrint(Expression expression) {
    if (expression == null) {
      return "null";
    }
    if (expression.getIdentifier() != null) {
      return expression.getIdentifier().getName();
    }
    if (expression.getLiteral() != null) {
      if (expression.getLiteral().isSetLongValue()) {
        return Long.toString(expression.getLiteral().getLongValue());
      }
    }
    if (expression.getFunctionCall() != null) {
      String res = expression.getFunctionCall().getOperator() + "(";
      boolean isFirstParam = true;
      for (Expression operand : expression.getFunctionCall().getOperands()) {
        if (!isFirstParam) {
          res += ", ";
        } else {
          isFirstParam = false;
        }
        res += prettyPrint(operand);
      }
      res += ")";
      return res;
    }
    return null;
  }

  private static Set<String> getTableNames(DataSource dataSource) {
    if (dataSource.getSubquery() != null) {
      return getTableNames(dataSource.getSubquery());
    } else if (dataSource.isSetJoin()) {
      return ImmutableSet.<String>builder()
          .addAll(getTableNames(dataSource.getJoin().getLeft()))
          .addAll(getTableNames(dataSource.getJoin().getLeft())).build();
    }
    return ImmutableSet.of(dataSource.getTableName());
  }

  public static Set<String> getTableNames(PinotQuery pinotQuery) {
    return getTableNames(pinotQuery.getDataSource());
  }

  public static Map<String, String> getOptionsFromJson(JsonNode request, String optionsKey) {
    return getOptionsFromString(request.get(optionsKey).asText());
  }

  public static Map<String, String> getOptionsFromString(String optionStr) {
    return Splitter.on(';').omitEmptyStrings().trimResults().withKeyValueSeparator('=').split(optionStr);
  }

  /**
   * Returns all the table names from a given {@link SqlNode}.
   * <pre>
   * 1. FROM Clause (FromNode): The main location where the table name is specified.
   * </pre>
   * {@code
   *     SELECT * FROM table_name;
   * }
   * <pre>
   * 2. JOIN Clauses (JoinNode): Table names will be part of INNER JOIN, LEFT JOIN, RIGHT JOIN, FULL JOIN, etc.
   * </pre>
   * {@code
   *     SELECT * FROM table_name1 JOIN table_name2 ON table_name1.column_name = table_name2.column_name;
   * }
   * <pre>
   * 3. SubQueries in FROM Clause (SubQueryNode): Subqueries in the FROM clause might contain additional table names.
   * </pre>
   * {@code
   *     SELECT * FROM (SELECT * FROM table_name) WHERE column_name = value;
   * }
   * <pre>
   * 4. WITH Clause (WithNode): Common Table Expressions (CTEs) may contain table names.
   * </pre>
   * {@code
   *     WITH table_name1 AS (SELECT * FROM table_name2) SELECT * FROM table_name1;
   * }
   * <pre>
   * 5. LATERAL or APPLY Operators (LateralNode, ApplyNode): These operators allow you to reference columns of
   *    preceding tables in FROM clause sub-queries.
   * </pre>
   * {@code
   *     SELECT * FROM table_name1, LATERAL (SELECT * FROM table_name2) AS table_name2;
   * }
   * <pre>
   * 6. UNION, INTERSECT, EXCEPT Clauses (SetOperationNode): These set operations between multiple SELECT statements
   *    can also contain table names.
   * </pre>
   * {@code
   *     SELECT * FROM table_name1 UNION SELECT * FROM table_name2;
   * }
   * <pre>
   * 7. WHERE Clause (WhereNode): WHERE clause can contain table names in subqueries.
   * </pre>
   * {@code
   *     SELECT * FROM table_name WHERE column_name IN (SELECT * FROM table_name2);
   * }
   * @param sqlNode Sql Query Node
   * @return Set of table names
   */
  public static Set<String> getTableNames(SqlNode sqlNode) {
    Set<String> tableNames = new HashSet<>();
    if (sqlNode instanceof SqlSelect) {
      // Handle SqlSelect query
      SqlNode fromNode = ((SqlSelect) sqlNode).getFrom();
      if ((fromNode instanceof SqlBasicCall)
          && (((SqlBasicCall) fromNode).getOperator() instanceof SqlAsOperator)) {
        tableNames.addAll(getTableNames(((SqlBasicCall) fromNode).getOperandList().get(0)));
      } else if (fromNode instanceof SqlIdentifier) {
        tableNames.add(getTableName((SqlIdentifier) fromNode));
        tableNames.addAll(getTableNames(((SqlSelect) sqlNode).getWhere()));
      } else {
        tableNames.addAll(getTableNames(fromNode));
      }
    } else if (sqlNode instanceof SqlJoin) {
      // Handle SqlJoin query
      SqlNode left = ((SqlJoin) sqlNode).getLeft();
      SqlNode right = ((SqlJoin) sqlNode).getRight();
      if (left instanceof SqlIdentifier) {
        tableNames.add(getTableName(((SqlIdentifier) left)));
      } else {
        tableNames.addAll(getTableNames(left));
      }
      if (right instanceof SqlIdentifier) {
        tableNames.add(getTableName(((SqlIdentifier) right)));
      } else {
        tableNames.addAll(getTableNames(right));
      }
    } else if (sqlNode instanceof SqlOrderBy) {
      // Handle SqlOrderBy query
      // tableNames.addAll(getTableNames(((SqlOrderBy) sqlNode).query));
      for (SqlNode node : ((SqlOrderBy) sqlNode).getOperandList()) {
        tableNames.addAll(getTableNames(node));
      }
    } else if (sqlNode instanceof SqlBasicCall) {
      // Handle SqlBasicCall query
      if (((SqlBasicCall) sqlNode).getOperator() instanceof SqlAsOperator) {
        SqlNode firstOperand = ((SqlBasicCall) sqlNode).getOperandList().get(0);
        if (firstOperand instanceof SqlIdentifier) {
          tableNames.add(getTableName((SqlIdentifier) firstOperand));
        } else {
          tableNames.addAll(getTableNames(firstOperand));
        }
      } else {
        for (SqlNode node : ((SqlBasicCall) sqlNode).getOperandList()) {
          tableNames.addAll(getTableNames(node));
        }
      }
    } else if (sqlNode instanceof SqlWith) {
      // Handle SqlWith query
      SqlWith sqlWith = (SqlWith) sqlNode;
      List<SqlNode> withList = sqlWith.withList;
      // Table names from body, it may contains table alias from WITH clause
      tableNames.addAll(getTableNames(sqlWith.body));
      // Table alias from WITH clause, should be removed from the final results
      withList.forEach(
          sqlWithItem -> tableNames.remove(getTableName(((SqlWithItem) sqlWithItem).name)));
      // Table names from WITH clause
      withList.forEach(
          sqlWithItem -> tableNames.addAll(getTableNames(((SqlWithItem) sqlWithItem).getOperandList().get(2))));
    } else if (sqlNode instanceof SqlSetOption) {
      // Handle SqlSetOption query
      for (SqlNode node : ((SqlSetOption) sqlNode).getOperandList()) {
        tableNames.addAll(getTableNames(node));
      }
    } else if (sqlNode instanceof SqlExplain) {
      // Handle SqlExplain query
      tableNames.addAll(getTableNames(((SqlExplain) sqlNode).getExplicandum()));
    }
    return tableNames;
  }

  /**
   * Helper method to get table name from SqlIdentifier, it may in the format of dbName.tableName or tableName
   * @param sqlIdentifier SqlIdentifier
   * @return table name
   */
  public static String getTableName(SqlIdentifier sqlIdentifier) {
    return sqlIdentifier.names.get(sqlIdentifier.names.size() - 1);
  }

  public static Set<String> getTableNames(String query) {
    SqlNodeAndOptions sqlNodeAndOptions = CalciteSqlParser.compileToSqlNodeAndOptions(query);
    return getTableNames(sqlNodeAndOptions.getSqlNode());
  }
}
