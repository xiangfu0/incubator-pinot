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
/**
 * Autogenerated by Thrift Compiler (0.13.0)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package org.apache.pinot.common.request;

@SuppressWarnings({"cast", "rawtypes", "serial", "unchecked", "unused"})
/**
 * AUTO GENERATED: DO NOT EDIT
 * Filter Query is nested but thrift stable version does not support yet (The support is there in top of the trunk but no released jars. Two concerns : stability and onus of maintaining a stable point. Also, its pretty difficult to compile thrift in Linkedin software development environment which is not geared towards c++ dev. Hence, the )
 *
 */
@javax.annotation.Generated(value = "Autogenerated by Thrift Compiler (0.13.0)", date = "2021-09-28")
public class FilterQueryMap implements org.apache.thrift.TBase<FilterQueryMap, FilterQueryMap._Fields>, java.io.Serializable, Cloneable, Comparable<FilterQueryMap> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("FilterQueryMap");

  private static final org.apache.thrift.protocol.TField FILTER_QUERY_MAP_FIELD_DESC = new org.apache.thrift.protocol.TField("filterQueryMap", org.apache.thrift.protocol.TType.MAP, (short)1);

  private static final org.apache.thrift.scheme.SchemeFactory STANDARD_SCHEME_FACTORY = new FilterQueryMapStandardSchemeFactory();
  private static final org.apache.thrift.scheme.SchemeFactory TUPLE_SCHEME_FACTORY = new FilterQueryMapTupleSchemeFactory();

  private @org.apache.thrift.annotation.Nullable java.util.Map<java.lang.Integer,FilterQuery> filterQueryMap; // optional

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    FILTER_QUERY_MAP((short)1, "filterQueryMap");

    private static final java.util.Map<java.lang.String, _Fields> byName = new java.util.HashMap<java.lang.String, _Fields>();

    static {
      for (_Fields field : java.util.EnumSet.allOf(_Fields.class)) {
        byName.put(field.getFieldName(), field);
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, or null if its not found.
     */
    @org.apache.thrift.annotation.Nullable
    public static _Fields findByThriftId(int fieldId) {
      switch(fieldId) {
        case 1: // FILTER_QUERY_MAP
          return FILTER_QUERY_MAP;
        default:
          return null;
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, throwing an exception
     * if it is not found.
     */
    public static _Fields findByThriftIdOrThrow(int fieldId) {
      _Fields fields = findByThriftId(fieldId);
      if (fields == null) throw new java.lang.IllegalArgumentException("Field " + fieldId + " doesn't exist!");
      return fields;
    }

    /**
     * Find the _Fields constant that matches name, or null if its not found.
     */
    @org.apache.thrift.annotation.Nullable
    public static _Fields findByName(java.lang.String name) {
      return byName.get(name);
    }

    private final short _thriftId;
    private final java.lang.String _fieldName;

    _Fields(short thriftId, java.lang.String fieldName) {
      _thriftId = thriftId;
      _fieldName = fieldName;
    }

    public short getThriftFieldId() {
      return _thriftId;
    }

    public java.lang.String getFieldName() {
      return _fieldName;
    }
  }

  // isset id assignments
  private static final _Fields optionals[] = {_Fields.FILTER_QUERY_MAP};
  public static final java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new java.util.EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.FILTER_QUERY_MAP, new org.apache.thrift.meta_data.FieldMetaData("filterQueryMap", org.apache.thrift.TFieldRequirementType.OPTIONAL,
        new org.apache.thrift.meta_data.MapMetaData(org.apache.thrift.protocol.TType.MAP,
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32),
            new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, FilterQuery.class))));
    metaDataMap = java.util.Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(FilterQueryMap.class, metaDataMap);
  }

  public FilterQueryMap() {
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public FilterQueryMap(FilterQueryMap other) {
    if (other.isSetFilterQueryMap()) {
      java.util.Map<java.lang.Integer,FilterQuery> __this__filterQueryMap = new java.util.HashMap<java.lang.Integer,FilterQuery>(other.filterQueryMap.size());
      for (java.util.Map.Entry<java.lang.Integer, FilterQuery> other_element : other.filterQueryMap.entrySet()) {

        java.lang.Integer other_element_key = other_element.getKey();
        FilterQuery other_element_value = other_element.getValue();

        java.lang.Integer __this__filterQueryMap_copy_key = other_element_key;

        FilterQuery __this__filterQueryMap_copy_value = new FilterQuery(other_element_value);

        __this__filterQueryMap.put(__this__filterQueryMap_copy_key, __this__filterQueryMap_copy_value);
      }
      this.filterQueryMap = __this__filterQueryMap;
    }
  }

  public FilterQueryMap deepCopy() {
    return new FilterQueryMap(this);
  }

  @Override
  public void clear() {
    this.filterQueryMap = null;
  }

  public int getFilterQueryMapSize() {
    return (this.filterQueryMap == null) ? 0 : this.filterQueryMap.size();
  }

  public void putToFilterQueryMap(int key, FilterQuery val) {
    if (this.filterQueryMap == null) {
      this.filterQueryMap = new java.util.HashMap<java.lang.Integer,FilterQuery>();
    }
    this.filterQueryMap.put(key, val);
  }

  @org.apache.thrift.annotation.Nullable
  public java.util.Map<java.lang.Integer,FilterQuery> getFilterQueryMap() {
    return this.filterQueryMap;
  }

  public void setFilterQueryMap(@org.apache.thrift.annotation.Nullable java.util.Map<java.lang.Integer,FilterQuery> filterQueryMap) {
    this.filterQueryMap = filterQueryMap;
  }

  public void unsetFilterQueryMap() {
    this.filterQueryMap = null;
  }

  /** Returns true if field filterQueryMap is set (has been assigned a value) and false otherwise */
  public boolean isSetFilterQueryMap() {
    return this.filterQueryMap != null;
  }

  public void setFilterQueryMapIsSet(boolean value) {
    if (!value) {
      this.filterQueryMap = null;
    }
  }

  public void setFieldValue(_Fields field, @org.apache.thrift.annotation.Nullable java.lang.Object value) {
    switch (field) {
    case FILTER_QUERY_MAP:
      if (value == null) {
        unsetFilterQueryMap();
      } else {
        setFilterQueryMap((java.util.Map<java.lang.Integer,FilterQuery>)value);
      }
      break;
    }
  }

  @org.apache.thrift.annotation.Nullable
  public java.lang.Object getFieldValue(_Fields field) {
    switch (field) {
    case FILTER_QUERY_MAP:
      return getFilterQueryMap();
    }
    throw new java.lang.IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new java.lang.IllegalArgumentException();
    }

    switch (field) {
    case FILTER_QUERY_MAP:
      return isSetFilterQueryMap();
    }
    throw new java.lang.IllegalStateException();
  }

  @Override
  public boolean equals(java.lang.Object that) {
    if (that == null)
      return false;
    if (that instanceof FilterQueryMap)
      return this.equals((FilterQueryMap)that);
    return false;
  }

  public boolean equals(FilterQueryMap that) {
    if (that == null)
      return false;
    if (this == that)
      return true;

    boolean this_present_filterQueryMap = true && this.isSetFilterQueryMap();
    boolean that_present_filterQueryMap = true && that.isSetFilterQueryMap();
    if (this_present_filterQueryMap || that_present_filterQueryMap) {
      if (!(this_present_filterQueryMap && that_present_filterQueryMap))
        return false;
      if (!this.filterQueryMap.equals(that.filterQueryMap))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int hashCode = 1;

    hashCode = hashCode * 8191 + ((isSetFilterQueryMap()) ? 131071 : 524287);
    if (isSetFilterQueryMap())
      hashCode = hashCode * 8191 + filterQueryMap.hashCode();

    return hashCode;
  }

  @Override
  public int compareTo(FilterQueryMap other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = java.lang.Boolean.valueOf(isSetFilterQueryMap()).compareTo(other.isSetFilterQueryMap());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetFilterQueryMap()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.filterQueryMap, other.filterQueryMap);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    return 0;
  }

  @org.apache.thrift.annotation.Nullable
  public _Fields fieldForId(int fieldId) {
    return _Fields.findByThriftId(fieldId);
  }

  public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
    scheme(iprot).read(iprot, this);
  }

  public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
    scheme(oprot).write(oprot, this);
  }

  @Override
  public java.lang.String toString() {
    java.lang.StringBuilder sb = new java.lang.StringBuilder("FilterQueryMap(");
    boolean first = true;

    if (isSetFilterQueryMap()) {
      sb.append("filterQueryMap:");
      if (this.filterQueryMap == null) {
        sb.append("null");
      } else {
        sb.append(this.filterQueryMap);
      }
      first = false;
    }
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    // check for sub-struct validity
  }

  private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    try {
      write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, java.lang.ClassNotFoundException {
    try {
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class FilterQueryMapStandardSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
    public FilterQueryMapStandardScheme getScheme() {
      return new FilterQueryMapStandardScheme();
    }
  }

  private static class FilterQueryMapStandardScheme extends org.apache.thrift.scheme.StandardScheme<FilterQueryMap> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, FilterQueryMap struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) {
          break;
        }
        switch (schemeField.id) {
          case 1: // FILTER_QUERY_MAP
            if (schemeField.type == org.apache.thrift.protocol.TType.MAP) {
              {
                org.apache.thrift.protocol.TMap _map16 = iprot.readMapBegin();
                struct.filterQueryMap = new java.util.HashMap<java.lang.Integer,FilterQuery>(2*_map16.size);
                int _key17;
                @org.apache.thrift.annotation.Nullable FilterQuery _val18;
                for (int _i19 = 0; _i19 < _map16.size; ++_i19)
                {
                  _key17 = iprot.readI32();
                  _val18 = new FilterQuery();
                  _val18.read(iprot);
                  struct.filterQueryMap.put(_key17, _val18);
                }
                iprot.readMapEnd();
              }
              struct.setFilterQueryMapIsSet(true);
            } else {
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          default:
            org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
        }
        iprot.readFieldEnd();
      }
      iprot.readStructEnd();
      struct.validate();
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot, FilterQueryMap struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.filterQueryMap != null) {
        if (struct.isSetFilterQueryMap()) {
          oprot.writeFieldBegin(FILTER_QUERY_MAP_FIELD_DESC);
          {
            oprot.writeMapBegin(new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.I32, org.apache.thrift.protocol.TType.STRUCT, struct.filterQueryMap.size()));
            for (java.util.Map.Entry<java.lang.Integer, FilterQuery> _iter20 : struct.filterQueryMap.entrySet())
            {
              oprot.writeI32(_iter20.getKey());
              _iter20.getValue().write(oprot);
            }
            oprot.writeMapEnd();
          }
          oprot.writeFieldEnd();
        }
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }
  }

  private static class FilterQueryMapTupleSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
    public FilterQueryMapTupleScheme getScheme() {
      return new FilterQueryMapTupleScheme();
    }
  }

  private static class FilterQueryMapTupleScheme extends org.apache.thrift.scheme.TupleScheme<FilterQueryMap> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, FilterQueryMap struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TTupleProtocol oprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
      java.util.BitSet optionals = new java.util.BitSet();
      if (struct.isSetFilterQueryMap()) {
        optionals.set(0);
      }
      oprot.writeBitSet(optionals, 1);
      if (struct.isSetFilterQueryMap()) {
        {
          oprot.writeI32(struct.filterQueryMap.size());
          for (java.util.Map.Entry<java.lang.Integer, FilterQuery> _iter21 : struct.filterQueryMap.entrySet())
          {
            oprot.writeI32(_iter21.getKey());
            _iter21.getValue().write(oprot);
          }
        }
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, FilterQueryMap struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TTupleProtocol iprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
      java.util.BitSet incoming = iprot.readBitSet(1);
      if (incoming.get(0)) {
        {
          org.apache.thrift.protocol.TMap _map22 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.I32, org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
          struct.filterQueryMap = new java.util.HashMap<java.lang.Integer,FilterQuery>(2*_map22.size);
          int _key23;
          @org.apache.thrift.annotation.Nullable FilterQuery _val24;
          for (int _i25 = 0; _i25 < _map22.size; ++_i25)
          {
            _key23 = iprot.readI32();
            _val24 = new FilterQuery();
            _val24.read(iprot);
            struct.filterQueryMap.put(_key23, _val24);
          }
        }
        struct.setFilterQueryMapIsSet(true);
      }
    }
  }

  private static <S extends org.apache.thrift.scheme.IScheme> S scheme(org.apache.thrift.protocol.TProtocol proto) {
    return (org.apache.thrift.scheme.StandardScheme.class.equals(proto.getScheme()) ? STANDARD_SCHEME_FACTORY : TUPLE_SCHEME_FACTORY).getScheme();
  }
}
