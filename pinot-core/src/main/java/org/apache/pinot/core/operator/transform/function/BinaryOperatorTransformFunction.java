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
package org.apache.pinot.core.operator.transform.function;

import com.google.common.base.Preconditions;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.pinot.common.function.TransformFunctionType;
import org.apache.pinot.common.request.context.ExpressionContext;
import org.apache.pinot.common.request.context.predicate.EqPredicate;
import org.apache.pinot.common.request.context.predicate.NotEqPredicate;
import org.apache.pinot.common.request.context.predicate.RangePredicate;
import org.apache.pinot.core.operator.ColumnContext;
import org.apache.pinot.core.operator.blocks.ValueBlock;
import org.apache.pinot.core.operator.filter.predicate.EqualsPredicateEvaluatorFactory;
import org.apache.pinot.core.operator.filter.predicate.NotEqualsPredicateEvaluatorFactory;
import org.apache.pinot.core.operator.filter.predicate.PredicateEvaluator;
import org.apache.pinot.core.operator.filter.predicate.RangePredicateEvaluatorFactory;
import org.apache.pinot.core.operator.transform.TransformResultMetadata;
import org.apache.pinot.spi.data.FieldSpec.DataType;
import org.apache.pinot.spi.utils.ByteArray;
import org.apache.pinot.spi.utils.BytesUtils;
import org.apache.pinot.spi.utils.CommonConstants;
import org.roaringbitmap.RoaringBitmap;


/**
 * <code>BinaryOperatorTransformFunction</code> abstracts common functions for binary operators (=, !=, >=, >, <=, <).
 * The results are BOOLEAN type.
 */
public abstract class BinaryOperatorTransformFunction extends BaseTransformFunction {
  private static final int EQUALS = 0;
  private static final int GREATER_THAN_OR_EQUAL = 1;
  private static final int GREATER_THAN = 2;
  private static final int LESS_THAN = 3;
  private static final int LESS_THAN_OR_EQUAL = 4;
  private static final int NOT_EQUAL = 5;

  protected final int _op;
  protected final TransformFunctionType _transformFunctionType;
  protected TransformFunction _leftTransformFunction;
  protected TransformFunction _rightTransformFunction;
  protected DataType _leftStoredType;
  protected DataType _rightStoredType;
  protected PredicateEvaluator _predicateEvaluator;
  protected boolean _isNull;
  protected boolean _isAlwaysTrue;
  protected boolean _isAlwaysFalse;

  protected BinaryOperatorTransformFunction(TransformFunctionType transformFunctionType) {
    // translate to integer in [0, 5] for guaranteed tableswitch
    switch (transformFunctionType) {
      case EQUALS:
        _op = EQUALS;
        break;
      case GREATER_THAN_OR_EQUAL:
        _op = GREATER_THAN_OR_EQUAL;
        break;
      case GREATER_THAN:
        _op = GREATER_THAN;
        break;
      case LESS_THAN:
        _op = LESS_THAN;
        break;
      case LESS_THAN_OR_EQUAL:
        _op = LESS_THAN_OR_EQUAL;
        break;
      case NOT_EQUALS:
        _op = NOT_EQUAL;
        break;
      default:
        throw new IllegalArgumentException("non-binary transform function provided: " + transformFunctionType);
    }
    _transformFunctionType = transformFunctionType;
  }

  @Override
  public String getName() {
    return _transformFunctionType.getName();
  }

  @Override
  public void init(List<TransformFunction> arguments, Map<String, ColumnContext> columnContextMap) {
    super.init(arguments, columnContextMap);
    // Check that there are exact 2 arguments
    Preconditions.checkArgument(arguments.size() == 2,
        "Exact 2 arguments are required for binary operator transform function");
    _leftTransformFunction = arguments.get(0);
    _rightTransformFunction = arguments.get(1);
    _leftStoredType = _leftTransformFunction.getResultMetadata().getDataType().getStoredType();
    _rightStoredType = _rightTransformFunction.getResultMetadata().getDataType().getStoredType();

    if (_leftTransformFunction instanceof IdentifierTransformFunction
        && _rightTransformFunction instanceof LiteralTransformFunction) {
      IdentifierTransformFunction leftTransformFunction = (IdentifierTransformFunction) _leftTransformFunction;
      if (leftTransformFunction.getDictionary() != null) {
        LiteralTransformFunction rightTransformFunction = (LiteralTransformFunction) _rightTransformFunction;
        if (rightTransformFunction.isNull()) {
          _isNull = true;
        }
        String rightLiteralStr = sanitize(rightTransformFunction.getStringLiteral(), _op, _leftStoredType);

        switch (_op) {
          case EQUALS:
            _predicateEvaluator = EqualsPredicateEvaluatorFactory.newDictionaryBasedEvaluator(
                new EqPredicate(ExpressionContext.forIdentifier(leftTransformFunction.getColumnName()),
                    rightLiteralStr), leftTransformFunction.getDictionary(), _leftStoredType);
            break;
          case NOT_EQUAL:
            _predicateEvaluator = NotEqualsPredicateEvaluatorFactory.newDictionaryBasedEvaluator(
                new NotEqPredicate(ExpressionContext.forIdentifier(leftTransformFunction.getColumnName()),
                    rightLiteralStr), leftTransformFunction.getDictionary(), _leftStoredType);
            break;
          case GREATER_THAN_OR_EQUAL:
            _predicateEvaluator = RangePredicateEvaluatorFactory.newDictionaryBasedEvaluator(
                new RangePredicate(ExpressionContext.forIdentifier(leftTransformFunction.getColumnName()),
                    RangePredicate.getGreatEqualRange(rightLiteralStr)), leftTransformFunction.getDictionary(),
                _leftStoredType);
            break;
          case GREATER_THAN:
            _predicateEvaluator = RangePredicateEvaluatorFactory.newDictionaryBasedEvaluator(
                new RangePredicate(ExpressionContext.forIdentifier(leftTransformFunction.getColumnName()),
                    RangePredicate.getGreatRange(rightLiteralStr)), leftTransformFunction.getDictionary(),
                _leftStoredType);
            break;
          case LESS_THAN:
            _predicateEvaluator = RangePredicateEvaluatorFactory.newDictionaryBasedEvaluator(
                new RangePredicate(ExpressionContext.forIdentifier(leftTransformFunction.getColumnName()),
                    RangePredicate.getLessRange(rightLiteralStr)), leftTransformFunction.getDictionary(),
                _leftStoredType);
            break;
          case LESS_THAN_OR_EQUAL:
            _predicateEvaluator = RangePredicateEvaluatorFactory.newDictionaryBasedEvaluator(
                new RangePredicate(ExpressionContext.forIdentifier(leftTransformFunction.getColumnName()),
                    RangePredicate.getLessEqualRange(rightLiteralStr)), leftTransformFunction.getDictionary(),
                _leftStoredType);
            break;
          default:
            throw new IllegalStateException("Unsupported operation for dictionary based predicate: " + _op);
        }
      }
    }

    // Data type check: left and right types should be compatible.
    if (_leftStoredType == DataType.BYTES || _rightStoredType == DataType.BYTES) {
      Preconditions.checkState(_leftStoredType == _rightStoredType, String.format(
          "Unsupported data type for comparison: [Left Transform Function [%s] result type is [%s], Right Transform "
              + "Function [%s] result type is [%s]]", _leftTransformFunction.getName(), _leftStoredType,
          _rightTransformFunction.getName(), _rightStoredType));
    }
  }

  private String sanitize(String literal, int binaryOp, DataType expectedDatType) {
    if (_isNull) {
      switch (expectedDatType) {
        case BOOLEAN:
        case INT:
          return Integer.toString(CommonConstants.NullValuePlaceHolder.INT);
        case LONG:
        case TIMESTAMP:
          return Long.toString(CommonConstants.NullValuePlaceHolder.LONG);
        case FLOAT:
          return Float.toString(CommonConstants.NullValuePlaceHolder.FLOAT);
        case DOUBLE:
          return Double.toString(CommonConstants.NullValuePlaceHolder.DOUBLE);
        case BIG_DECIMAL:
          return CommonConstants.NullValuePlaceHolder.BIG_DECIMAL.toPlainString();
        case STRING:
          return CommonConstants.NullValuePlaceHolder.STRING;
        case BYTES:
          return BytesUtils.toHexString(CommonConstants.NullValuePlaceHolder.BYTES);
        default:
          return null;
      }
    }
    if (!expectedDatType.isNumeric()) {
      return literal;
    }

    BigDecimal decimalValue = new BigDecimal(literal);
    switch (expectedDatType) {
      case INT:
        int value = decimalValue.intValue();
        if (decimalValue.compareTo(BigDecimal.valueOf(value)) == 0) {
          // If the decimal value is an integer, return the integer value directly
          return Integer.toString(value);
        }
        switch (binaryOp) {
          case EQUALS:
            // The decimal value is not an integer, so the comparison is always false
            _isAlwaysFalse = true;
            return Integer.toString(value);
          case NOT_EQUAL:
            // The decimal value is not an integer, so the comparison is always true
            _isAlwaysTrue = true;
            return Integer.toString(value);
          case GREATER_THAN_OR_EQUAL:
          case LESS_THAN:
            return Integer.toString(value + 1);
          case GREATER_THAN:
          case LESS_THAN_OR_EQUAL:
            return Integer.toString(value);
          default:
            throw new IllegalStateException("Unsupported binary operator: " + binaryOp);
        }
      case LONG:
        long longValue = decimalValue.longValue();
        if (decimalValue.compareTo(BigDecimal.valueOf(longValue)) == 0) {
          // If the decimal value is a long, return the long value directly
          return Long.toString(longValue);
        }
        switch (binaryOp) {
          case EQUALS:
            // The decimal value is not a long, so the comparison is always false
            _isAlwaysFalse = true;
            return Long.toString(longValue);
          case NOT_EQUAL:
            // The decimal value is not a long, so the comparison is always true
            _isAlwaysTrue = true;
            return Long.toString(longValue);
          case GREATER_THAN_OR_EQUAL:
          case LESS_THAN:
            return Long.toString(longValue + 1);
          case GREATER_THAN:
          case LESS_THAN_OR_EQUAL:
            return Long.toString(longValue);
          default:
            throw new IllegalStateException("Unsupported binary operator: " + binaryOp);
        }
      case FLOAT:
        float floatValue = decimalValue.floatValue();
        if (decimalValue.compareTo(BigDecimal.valueOf(floatValue)) == 0) {
          // If the decimal value is a float, return the float value directly
          return Float.toString(floatValue);
        }
        switch (binaryOp) {
          case EQUALS:
            // The decimal value is not a float, so the comparison is always false
            _isAlwaysFalse = true;
            return Float.toString(floatValue);
          case NOT_EQUAL:
            // The decimal value is not a float, so the comparison is always true
            _isAlwaysTrue = true;
            return Float.toString(floatValue);
          default:
            return Float.toString(floatValue);
        }
      default:
        return decimalValue.toString();
    }
  }

  @Override
  public TransformResultMetadata getResultMetadata() {
    return BOOLEAN_SV_NO_DICTIONARY_METADATA;
  }

  @Override
  public int[] transformToIntValuesSV(ValueBlock valueBlock) {
    fillResultArray(valueBlock);
    return _intValuesSV;
  }

  private void fillResultArray(ValueBlock valueBlock) {
    int length = valueBlock.getNumDocs();
    initIntValuesSV(length);
    if (_isAlwaysTrue) {
      Arrays.fill(_intValuesSV, 0, length, 1);
      return;
    }
    if (_isAlwaysFalse) {
      Arrays.fill(_intValuesSV, 0, length, 0);
      return;
    }
    if (_isNull) {
      // If nullBitMap exists, then use it to fill the result
      RoaringBitmap nullBitmap = _leftTransformFunction.getNullBitmap(valueBlock);
      if (nullBitmap != null) {
        if (_op == EQUALS) {
          for (int i = 0; i < length; i++) {
            _intValuesSV[i] = nullBitmap.contains(i) ? 1 : 0;
          }
        } else {
          for (int i = 0; i < length; i++) {
            _intValuesSV[i] = nullBitmap.contains(i) ? 0 : 1;
          }
        }
      }
      return;
    }
    if (_leftTransformFunction.getDictionary() != null && _predicateEvaluator != null) {
      int[] dictIds = _leftTransformFunction.transformToDictIdsSV(valueBlock);
      for (int i = 0; i < dictIds.length; i++) {
        _intValuesSV[i] = _predicateEvaluator.applySV(dictIds[i]) ? 1 : 0;
      }
    } else {
      switch (_leftStoredType) {
        case INT:
          fillResultInt(valueBlock, length);
          break;
        case LONG:
          fillResultLong(valueBlock, length);
          break;
        case FLOAT:
          fillResultFloat(valueBlock, length);
          break;
        case DOUBLE:
          fillResultDouble(valueBlock, length);
          break;
        case BIG_DECIMAL:
          fillResultBigDecimal(valueBlock, length);
          break;
        case STRING:
          fillResultString(valueBlock, length);
          break;
        case BYTES:
          fillResultBytes(valueBlock, length);
          break;
        case UNKNOWN:
          fillResultUnknown(length);
          break;
        // NOTE: Multi-value columns are not comparable, so we should not reach here
        default:
          throw illegalState();
      }
    }
  }

  private void fillResultInt(ValueBlock valueBlock, int length) {
    int[] leftIntValues = _leftTransformFunction.transformToIntValuesSV(valueBlock);
    if (_predicateEvaluator != null) {
      for (int i = 0; i < length; i++) {
        _intValuesSV[i] = _predicateEvaluator.applySV(leftIntValues[i]) ? 1 : 0;
      }
      return;
    }
    switch (_rightStoredType) {
      case INT:
        fillIntResultArray(valueBlock, leftIntValues, length);
        break;
      case LONG:
        fillLongResultArray(valueBlock, leftIntValues, length);
        break;
      case FLOAT:
        fillFloatResultArray(valueBlock, leftIntValues, length);
        break;
      case DOUBLE:
        fillDoubleResultArray(valueBlock, leftIntValues, length);
        break;
      case BIG_DECIMAL:
        fillBigDecimalResultArray(valueBlock, leftIntValues, length);
        break;
      case STRING:
        fillStringResultArray(valueBlock, leftIntValues, length);
        break;
      case UNKNOWN:
        fillResultUnknown(length);
        break;
      default:
        throw illegalState();
    }
  }

  private void fillResultLong(ValueBlock valueBlock, int length) {
    long[] leftLongValues = _leftTransformFunction.transformToLongValuesSV(valueBlock);
    if (_predicateEvaluator != null) {
      for (int i = 0; i < length; i++) {
        _intValuesSV[i] = _predicateEvaluator.applySV(leftLongValues[i]) ? 1 : 0;
      }
      return;
    }
    switch (_rightStoredType) {
      case INT:
        fillIntResultArray(valueBlock, leftLongValues, length);
        break;
      case LONG:
        fillLongResultArray(valueBlock, leftLongValues, length);
        break;
      case FLOAT:
        fillFloatResultArray(valueBlock, leftLongValues, length);
        break;
      case DOUBLE:
        fillDoubleResultArray(valueBlock, leftLongValues, length);
        break;
      case BIG_DECIMAL:
        fillBigDecimalResultArray(valueBlock, leftLongValues, length);
        break;
      case STRING:
        fillStringResultArray(valueBlock, leftLongValues, length);
        break;
      case UNKNOWN:
        fillResultUnknown(length);
        break;
      default:
        throw illegalState();
    }
  }

  private void fillResultFloat(ValueBlock valueBlock, int length) {
    float[] leftFloatValues = _leftTransformFunction.transformToFloatValuesSV(valueBlock);
    if (_predicateEvaluator != null) {
      for (int i = 0; i < length; i++) {
        _intValuesSV[i] = _predicateEvaluator.applySV(leftFloatValues[i]) ? 1 : 0;
      }
      return;
    }
    switch (_rightStoredType) {
      case INT:
        fillIntResultArray(valueBlock, leftFloatValues, length);
        break;
      case LONG:
        fillLongResultArray(valueBlock, leftFloatValues, length);
        break;
      case FLOAT:
        fillFloatResultArray(valueBlock, leftFloatValues, length);
        break;
      case DOUBLE:
        fillDoubleResultArray(valueBlock, leftFloatValues, length);
        break;
      case BIG_DECIMAL:
        fillBigDecimalResultArray(valueBlock, leftFloatValues, length);
        break;
      case STRING:
        fillStringResultArray(valueBlock, leftFloatValues, length);
        break;
      case UNKNOWN:
        fillResultUnknown(length);
        break;
      default:
        throw illegalState();
    }
  }

  private void fillResultDouble(ValueBlock valueBlock, int length) {
    double[] leftDoubleValues = _leftTransformFunction.transformToDoubleValuesSV(valueBlock);
    if (_predicateEvaluator != null) {
      for (int i = 0; i < length; i++) {
        _intValuesSV[i] = _predicateEvaluator.applySV(leftDoubleValues[i]) ? 1 : 0;
      }
      return;
    }
    switch (_rightStoredType) {
      case INT:
        fillIntResultArray(valueBlock, leftDoubleValues, length);
        break;
      case LONG:
        fillLongResultArray(valueBlock, leftDoubleValues, length);
        break;
      case FLOAT:
        fillFloatResultArray(valueBlock, leftDoubleValues, length);
        break;
      case DOUBLE:
        fillDoubleResultArray(valueBlock, leftDoubleValues, length);
        break;
      case BIG_DECIMAL:
        fillBigDecimalResultArray(valueBlock, leftDoubleValues, length);
        break;
      case STRING:
        fillStringResultArray(valueBlock, leftDoubleValues, length);
        break;
      case UNKNOWN:
        fillResultUnknown(length);
        break;
      default:
        throw illegalState();
    }
  }

  private void fillResultBigDecimal(ValueBlock valueBlock, int length) {
    BigDecimal[] leftBigDecimalValues = _leftTransformFunction.transformToBigDecimalValuesSV(valueBlock);
    if (_predicateEvaluator != null) {
      for (int i = 0; i < length; i++) {
        _intValuesSV[i] = _predicateEvaluator.applySV(leftBigDecimalValues[i]) ? 1 : 0;
      }
      return;
    }
    switch (_rightStoredType) {
      case INT:
        fillIntResultArray(valueBlock, leftBigDecimalValues, length);
        break;
      case LONG:
        fillLongResultArray(valueBlock, leftBigDecimalValues, length);
        break;
      case FLOAT:
        fillFloatResultArray(valueBlock, leftBigDecimalValues, length);
        break;
      case DOUBLE:
        fillDoubleResultArray(valueBlock, leftBigDecimalValues, length);
        break;
      case STRING:
        fillStringResultArray(valueBlock, leftBigDecimalValues, length);
        break;
      case BIG_DECIMAL:
        fillBigDecimalResultArray(valueBlock, leftBigDecimalValues, length);
        break;
      case UNKNOWN:
        fillResultUnknown(length);
        break;
      default:
        throw illegalState();
    }
  }

  private IllegalStateException illegalState() {
    throw new IllegalStateException(String.format(
        "Unsupported data type for comparison: [Left Transform Function [%s] result type is [%s], Right "
            + "Transform Function [%s] result type is [%s]]", _leftTransformFunction.getName(), _leftStoredType,
        _rightTransformFunction.getName(), _rightStoredType));
  }

  private void fillResultString(ValueBlock valueBlock, int length) {
    String[] leftStringValues = _leftTransformFunction.transformToStringValuesSV(valueBlock);
    if (_predicateEvaluator != null) {
      for (int i = 0; i < length; i++) {
        _intValuesSV[i] = _predicateEvaluator.applySV(leftStringValues[i]) ? 1 : 0;
      }
      return;
    }
    String[] rightStringValues = _rightTransformFunction.transformToStringValuesSV(valueBlock);
    for (int i = 0; i < length; i++) {
      _intValuesSV[i] = getIntResult(leftStringValues[i].compareTo(rightStringValues[i]));
    }
  }

  private void fillResultBytes(ValueBlock valueBlock, int length) {
    byte[][] leftBytesValues = _leftTransformFunction.transformToBytesValuesSV(valueBlock);
    if (_predicateEvaluator != null) {
      for (int i = 0; i < length; i++) {
        _intValuesSV[i] = _predicateEvaluator.applySV(leftBytesValues[i]) ? 1 : 0;
      }
      return;
    }
    byte[][] rightBytesValues = _rightTransformFunction.transformToBytesValuesSV(valueBlock);
    for (int i = 0; i < length; i++) {
      _intValuesSV[i] = getIntResult((ByteArray.compare(leftBytesValues[i], rightBytesValues[i])));
    }
  }

  private void fillIntResultArray(ValueBlock valueBlock, int[] leftIntValues, int length) {
    int[] rightIntValues = _rightTransformFunction.transformToIntValuesSV(valueBlock);
    for (int i = 0; i < length; i++) {
      _intValuesSV[i] = getIntResult(Integer.compare(leftIntValues[i], rightIntValues[i]));
    }
  }

  private void fillLongResultArray(ValueBlock valueBlock, int[] leftValues, int length) {
    long[] rightValues = _rightTransformFunction.transformToLongValuesSV(valueBlock);
    for (int i = 0; i < length; i++) {
      _intValuesSV[i] = getIntResult(Long.compare(leftValues[i], rightValues[i]));
    }
  }

  private void fillFloatResultArray(ValueBlock valueBlock, int[] leftValues, int length) {
    float[] rightFloatValues = _rightTransformFunction.transformToFloatValuesSV(valueBlock);
    for (int i = 0; i < length; i++) {
      _intValuesSV[i] = getIntResult(Double.compare(leftValues[i], rightFloatValues[i]));
    }
  }

  private void fillDoubleResultArray(ValueBlock valueBlock, int[] leftValues, int length) {
    double[] rightDoubleValues = _rightTransformFunction.transformToDoubleValuesSV(valueBlock);
    for (int i = 0; i < length; i++) {
      _intValuesSV[i] = getIntResult(Double.compare(leftValues[i], rightDoubleValues[i]));
    }
  }

  private void fillBigDecimalResultArray(ValueBlock valueBlock, int[] leftValues, int length) {
    BigDecimal[] rightBigDecimalValues = _rightTransformFunction.transformToBigDecimalValuesSV(valueBlock);
    for (int i = 0; i < length; i++) {
      _intValuesSV[i] = getIntResult(BigDecimal.valueOf(leftValues[i]).compareTo(rightBigDecimalValues[i]));
    }
  }

  private void fillStringResultArray(ValueBlock valueBlock, int[] leftValues, int length) {
    String[] rightStringValues = _rightTransformFunction.transformToStringValuesSV(valueBlock);
    for (int i = 0; i < length; i++) {
      try {
        _intValuesSV[i] =
            getIntResult(BigDecimal.valueOf(leftValues[i]).compareTo(new BigDecimal(rightStringValues[i])));
      } catch (NumberFormatException e) {
        _intValuesSV[i] = 0;
      }
    }
  }

  private void fillIntResultArray(ValueBlock valueBlock, long[] leftIntValues, int length) {
    int[] rightIntValues = _rightTransformFunction.transformToIntValuesSV(valueBlock);
    for (int i = 0; i < length; i++) {
      _intValuesSV[i] = getIntResult(Long.compare(leftIntValues[i], rightIntValues[i]));
    }
  }

  private void fillLongResultArray(ValueBlock valueBlock, long[] leftValues, int length) {
    long[] rightValues = _rightTransformFunction.transformToLongValuesSV(valueBlock);
    for (int i = 0; i < length; i++) {
      _intValuesSV[i] = getIntResult(Long.compare(leftValues[i], rightValues[i]));
    }
  }

  private void fillFloatResultArray(ValueBlock valueBlock, long[] leftValues, int length) {
    float[] rightFloatValues = _rightTransformFunction.transformToFloatValuesSV(valueBlock);
    for (int i = 0; i < length; i++) {
      _intValuesSV[i] = getIntResult(compare(leftValues[i], rightFloatValues[i]));
    }
  }

  private void fillDoubleResultArray(ValueBlock valueBlock, long[] leftValues, int length) {
    double[] rightDoubleValues = _rightTransformFunction.transformToDoubleValuesSV(valueBlock);
    for (int i = 0; i < length; i++) {
      _intValuesSV[i] = getIntResult(compare(leftValues[i], rightDoubleValues[i]));
    }
  }

  private void fillBigDecimalResultArray(ValueBlock valueBlock, long[] leftValues, int length) {
    BigDecimal[] rightBigDecimalValues = _rightTransformFunction.transformToBigDecimalValuesSV(valueBlock);
    for (int i = 0; i < length; i++) {
      _intValuesSV[i] = getIntResult(BigDecimal.valueOf(leftValues[i]).compareTo(rightBigDecimalValues[i]));
    }
  }

  private void fillStringResultArray(ValueBlock valueBlock, long[] leftValues, int length) {
    String[] rightStringValues = _rightTransformFunction.transformToStringValuesSV(valueBlock);
    for (int i = 0; i < length; i++) {
      try {
        _intValuesSV[i] =
            getIntResult(BigDecimal.valueOf(leftValues[i]).compareTo(new BigDecimal(rightStringValues[i])));
      } catch (NumberFormatException e) {
        _intValuesSV[i] = 0;
      }
    }
  }

  private void fillIntResultArray(ValueBlock valueBlock, float[] leftValues, int length) {
    int[] rightIntValues = _rightTransformFunction.transformToIntValuesSV(valueBlock);
    for (int i = 0; i < length; i++) {
      _intValuesSV[i] = getIntResult(Double.compare(leftValues[i], rightIntValues[i]));
    }
  }

  private void fillLongResultArray(ValueBlock valueBlock, float[] leftValues, int length) {
    long[] rightValues = _rightTransformFunction.transformToLongValuesSV(valueBlock);
    for (int i = 0; i < length; i++) {
      _intValuesSV[i] = getIntResult(compare(leftValues[i], rightValues[i]));
    }
  }

  private void fillFloatResultArray(ValueBlock valueBlock, float[] leftValues, int length) {
    float[] rightFloatValues = _rightTransformFunction.transformToFloatValuesSV(valueBlock);
    for (int i = 0; i < length; i++) {
      _intValuesSV[i] = getIntResult(Float.compare(leftValues[i], rightFloatValues[i]));
    }
  }

  private void fillDoubleResultArray(ValueBlock valueBlock, float[] leftValues, int length) {
    double[] rightDoubleValues = _rightTransformFunction.transformToDoubleValuesSV(valueBlock);
    for (int i = 0; i < length; i++) {
      _intValuesSV[i] = getIntResult(Double.compare(leftValues[i], rightDoubleValues[i]));
    }
  }

  private void fillBigDecimalResultArray(ValueBlock valueBlock, float[] leftValues, int length) {
    BigDecimal[] rightBigDecimalValues = _rightTransformFunction.transformToBigDecimalValuesSV(valueBlock);
    for (int i = 0; i < length; i++) {
      _intValuesSV[i] = getIntResult(BigDecimal.valueOf(leftValues[i]).compareTo(rightBigDecimalValues[i]));
    }
  }

  private void fillStringResultArray(ValueBlock valueBlock, float[] leftValues, int length) {
    String[] rightStringValues = _rightTransformFunction.transformToStringValuesSV(valueBlock);
    for (int i = 0; i < length; i++) {
      try {
        _intValuesSV[i] =
            getIntResult(BigDecimal.valueOf(leftValues[i]).compareTo(new BigDecimal(rightStringValues[i])));
      } catch (NumberFormatException e) {
        _intValuesSV[i] = 0;
      }
    }
  }

  private void fillIntResultArray(ValueBlock valueBlock, double[] leftValues, int length) {
    int[] rightIntValues = _rightTransformFunction.transformToIntValuesSV(valueBlock);
    for (int i = 0; i < length; i++) {
      _intValuesSV[i] = getIntResult(Double.compare(leftValues[i], rightIntValues[i]));
    }
  }

  private void fillLongResultArray(ValueBlock valueBlock, double[] leftValues, int length) {
    long[] rightValues = _rightTransformFunction.transformToLongValuesSV(valueBlock);
    for (int i = 0; i < length; i++) {
      _intValuesSV[i] = getIntResult(compare(leftValues[i], rightValues[i]));
    }
  }

  private void fillFloatResultArray(ValueBlock valueBlock, double[] leftValues, int length) {
    float[] rightFloatValues = _rightTransformFunction.transformToFloatValuesSV(valueBlock);
    for (int i = 0; i < length; i++) {
      _intValuesSV[i] = getIntResult(Double.compare(leftValues[i], rightFloatValues[i]));
    }
  }

  private void fillDoubleResultArray(ValueBlock valueBlock, double[] leftValues, int length) {
    double[] rightDoubleValues = _rightTransformFunction.transformToDoubleValuesSV(valueBlock);
    for (int i = 0; i < length; i++) {
      _intValuesSV[i] = getIntResult(Double.compare(leftValues[i], rightDoubleValues[i]));
    }
  }

  private void fillBigDecimalResultArray(ValueBlock valueBlock, double[] leftValues, int length) {
    BigDecimal[] rightBigDecimalValues = _rightTransformFunction.transformToBigDecimalValuesSV(valueBlock);
    for (int i = 0; i < length; i++) {
      _intValuesSV[i] = getIntResult(BigDecimal.valueOf(leftValues[i]).compareTo(rightBigDecimalValues[i]));
    }
  }

  private void fillStringResultArray(ValueBlock valueBlock, double[] leftValues, int length) {
    String[] rightStringValues = _rightTransformFunction.transformToStringValuesSV(valueBlock);
    for (int i = 0; i < length; i++) {
      try {
        _intValuesSV[i] =
            getIntResult(BigDecimal.valueOf(leftValues[i]).compareTo(new BigDecimal(rightStringValues[i])));
      } catch (NumberFormatException e) {
        _intValuesSV[i] = 0;
      }
    }
  }

  private void fillIntResultArray(ValueBlock valueBlock, BigDecimal[] leftValues, int length) {
    int[] rightIntValues = _rightTransformFunction.transformToIntValuesSV(valueBlock);
    for (int i = 0; i < length; i++) {
      _intValuesSV[i] = getIntResult(leftValues[i].compareTo(BigDecimal.valueOf(rightIntValues[i])));
    }
  }

  private void fillLongResultArray(ValueBlock valueBlock, BigDecimal[] leftValues, int length) {
    long[] rightLongValues = _rightTransformFunction.transformToLongValuesSV(valueBlock);
    for (int i = 0; i < length; i++) {
      _intValuesSV[i] = getIntResult(leftValues[i].compareTo(BigDecimal.valueOf(rightLongValues[i])));
    }
  }

  private void fillFloatResultArray(ValueBlock valueBlock, BigDecimal[] leftValues, int length) {
    float[] rightFloatValues = _rightTransformFunction.transformToFloatValuesSV(valueBlock);
    for (int i = 0; i < length; i++) {
      _intValuesSV[i] = getIntResult(leftValues[i].compareTo(BigDecimal.valueOf(rightFloatValues[i])));
    }
  }

  private void fillDoubleResultArray(ValueBlock valueBlock, BigDecimal[] leftValues, int length) {
    double[] rightDoubleValues = _rightTransformFunction.transformToDoubleValuesSV(valueBlock);
    for (int i = 0; i < length; i++) {
      _intValuesSV[i] = getIntResult(leftValues[i].compareTo(BigDecimal.valueOf(rightDoubleValues[i])));
    }
  }

  private void fillBigDecimalResultArray(ValueBlock valueBlock, BigDecimal[] leftValues, int length) {
    BigDecimal[] rightBigDecimalValues = _rightTransformFunction.transformToBigDecimalValuesSV(valueBlock);
    for (int i = 0; i < length; i++) {
      _intValuesSV[i] = getIntResult(leftValues[i].compareTo(rightBigDecimalValues[i]));
    }
  }

  private void fillStringResultArray(ValueBlock valueBlock, BigDecimal[] leftValues, int length) {
    String[] rightStringValues = _rightTransformFunction.transformToStringValuesSV(valueBlock);
    for (int i = 0; i < length; i++) {
      _intValuesSV[i] = getIntResult(leftValues[i].compareTo(new BigDecimal(rightStringValues[i])));
    }
  }

  private int compare(long left, double right) {
    if (Math.abs(left) <= 1L << 53) {
      return Double.compare(left, right);
    } else {
      return BigDecimal.valueOf(left).compareTo(BigDecimal.valueOf(right));
    }
  }

  private int compare(double left, long right) {
    if (Math.abs(right) <= 1L << 53) {
      return Double.compare(left, right);
    } else {
      return BigDecimal.valueOf(left).compareTo(BigDecimal.valueOf(right));
    }
  }

  private int getIntResult(int comparisonResult) {
    return getBinaryFuncResult(comparisonResult) ? 1 : 0;
  }

  private boolean getBinaryFuncResult(int comparisonResult) {
    switch (_op) {
      case EQUALS:
        return comparisonResult == 0;
      case GREATER_THAN_OR_EQUAL:
        return comparisonResult >= 0;
      case GREATER_THAN:
        return comparisonResult > 0;
      case LESS_THAN:
        return comparisonResult < 0;
      case LESS_THAN_OR_EQUAL:
        return comparisonResult <= 0;
      case NOT_EQUAL:
        return comparisonResult != 0;
      default:
        throw new IllegalStateException();
    }
  }
}
