/**
 * Autogenerated by Thrift Compiler (0.9.2)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package org.apache.pinot.common.request;


import java.util.Map;
import java.util.HashMap;
import org.apache.thrift.TEnum;

/**
 * AUTO GENERATED: DO NOT EDIT
 * Filter Operator
 * 
 */
public enum FilterOperator implements org.apache.thrift.TEnum {
  AND(0),
  OR(1),
  EQUALITY(2),
  NOT(3),
  RANGE(4),
  REGEXP_LIKE(5),
  NOT_IN(6),
  IN(7),
  TEXT_MATCH(8);

  private final int value;

  private FilterOperator(int value) {
    this.value = value;
  }

  /**
   * Get the integer value of this enum value, as defined in the Thrift IDL.
   */
  public int getValue() {
    return value;
  }

  /**
   * Find a the enum type by its integer value, as defined in the Thrift IDL.
   * @return null if the value is not found.
   */
  public static FilterOperator findByValue(int value) { 
    switch (value) {
      case 0:
        return AND;
      case 1:
        return OR;
      case 2:
        return EQUALITY;
      case 3:
        return NOT;
      case 4:
        return RANGE;
      case 5:
        return REGEXP_LIKE;
      case 6:
        return NOT_IN;
      case 7:
        return IN;
      case 8:
        return TEXT_MATCH;
      default:
        return null;
    }
  }
}
