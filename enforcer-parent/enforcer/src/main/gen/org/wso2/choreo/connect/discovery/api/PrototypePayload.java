// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: wso2/discovery/api/prototype_config.proto

package org.wso2.choreo.connect.discovery.api;

/**
 * <pre>
 * PrototypePayload holds prototype payload configs in prototype implementations
 * </pre>
 *
 * Protobuf type {@code wso2.discovery.api.PrototypePayload}
 */
public final class PrototypePayload extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:wso2.discovery.api.PrototypePayload)
    PrototypePayloadOrBuilder {
private static final long serialVersionUID = 0L;
  // Use PrototypePayload.newBuilder() to construct.
  private PrototypePayload(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private PrototypePayload() {
    applicationJSON_ = "";
    applicationXML_ = "";
  }

  @java.lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(
      UnusedPrivateParameter unused) {
    return new PrototypePayload();
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return this.unknownFields;
  }
  private PrototypePayload(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    this();
    if (extensionRegistry == null) {
      throw new java.lang.NullPointerException();
    }
    com.google.protobuf.UnknownFieldSet.Builder unknownFields =
        com.google.protobuf.UnknownFieldSet.newBuilder();
    try {
      boolean done = false;
      while (!done) {
        int tag = input.readTag();
        switch (tag) {
          case 0:
            done = true;
            break;
          case 10: {
            java.lang.String s = input.readStringRequireUtf8();

            applicationJSON_ = s;
            break;
          }
          case 18: {
            java.lang.String s = input.readStringRequireUtf8();

            applicationXML_ = s;
            break;
          }
          default: {
            if (!parseUnknownField(
                input, unknownFields, extensionRegistry, tag)) {
              done = true;
            }
            break;
          }
        }
      }
    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
      throw e.setUnfinishedMessage(this);
    } catch (java.io.IOException e) {
      throw new com.google.protobuf.InvalidProtocolBufferException(
          e).setUnfinishedMessage(this);
    } finally {
      this.unknownFields = unknownFields.build();
      makeExtensionsImmutable();
    }
  }
  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return org.wso2.choreo.connect.discovery.api.SecurityConfigProto.internal_static_wso2_discovery_api_PrototypePayload_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return org.wso2.choreo.connect.discovery.api.SecurityConfigProto.internal_static_wso2_discovery_api_PrototypePayload_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            org.wso2.choreo.connect.discovery.api.PrototypePayload.class, org.wso2.choreo.connect.discovery.api.PrototypePayload.Builder.class);
  }

  public static final int APPLICATIONJSON_FIELD_NUMBER = 1;
  private volatile java.lang.Object applicationJSON_;
  /**
   * <code>string applicationJSON = 1;</code>
   * @return The applicationJSON.
   */
  @java.lang.Override
  public java.lang.String getApplicationJSON() {
    java.lang.Object ref = applicationJSON_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = 
          (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      applicationJSON_ = s;
      return s;
    }
  }
  /**
   * <code>string applicationJSON = 1;</code>
   * @return The bytes for applicationJSON.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString
      getApplicationJSONBytes() {
    java.lang.Object ref = applicationJSON_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = 
          com.google.protobuf.ByteString.copyFromUtf8(
              (java.lang.String) ref);
      applicationJSON_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int APPLICATIONXML_FIELD_NUMBER = 2;
  private volatile java.lang.Object applicationXML_;
  /**
   * <code>string applicationXML = 2;</code>
   * @return The applicationXML.
   */
  @java.lang.Override
  public java.lang.String getApplicationXML() {
    java.lang.Object ref = applicationXML_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = 
          (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      applicationXML_ = s;
      return s;
    }
  }
  /**
   * <code>string applicationXML = 2;</code>
   * @return The bytes for applicationXML.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString
      getApplicationXMLBytes() {
    java.lang.Object ref = applicationXML_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = 
          com.google.protobuf.ByteString.copyFromUtf8(
              (java.lang.String) ref);
      applicationXML_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  private byte memoizedIsInitialized = -1;
  @java.lang.Override
  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized == 1) return true;
    if (isInitialized == 0) return false;

    memoizedIsInitialized = 1;
    return true;
  }

  @java.lang.Override
  public void writeTo(com.google.protobuf.CodedOutputStream output)
                      throws java.io.IOException {
    if (!getApplicationJSONBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 1, applicationJSON_);
    }
    if (!getApplicationXMLBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 2, applicationXML_);
    }
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (!getApplicationJSONBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, applicationJSON_);
    }
    if (!getApplicationXMLBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, applicationXML_);
    }
    size += unknownFields.getSerializedSize();
    memoizedSize = size;
    return size;
  }

  @java.lang.Override
  public boolean equals(final java.lang.Object obj) {
    if (obj == this) {
     return true;
    }
    if (!(obj instanceof org.wso2.choreo.connect.discovery.api.PrototypePayload)) {
      return super.equals(obj);
    }
    org.wso2.choreo.connect.discovery.api.PrototypePayload other = (org.wso2.choreo.connect.discovery.api.PrototypePayload) obj;

    if (!getApplicationJSON()
        .equals(other.getApplicationJSON())) return false;
    if (!getApplicationXML()
        .equals(other.getApplicationXML())) return false;
    if (!unknownFields.equals(other.unknownFields)) return false;
    return true;
  }

  @java.lang.Override
  public int hashCode() {
    if (memoizedHashCode != 0) {
      return memoizedHashCode;
    }
    int hash = 41;
    hash = (19 * hash) + getDescriptor().hashCode();
    hash = (37 * hash) + APPLICATIONJSON_FIELD_NUMBER;
    hash = (53 * hash) + getApplicationJSON().hashCode();
    hash = (37 * hash) + APPLICATIONXML_FIELD_NUMBER;
    hash = (53 * hash) + getApplicationXML().hashCode();
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static org.wso2.choreo.connect.discovery.api.PrototypePayload parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static org.wso2.choreo.connect.discovery.api.PrototypePayload parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static org.wso2.choreo.connect.discovery.api.PrototypePayload parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static org.wso2.choreo.connect.discovery.api.PrototypePayload parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static org.wso2.choreo.connect.discovery.api.PrototypePayload parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static org.wso2.choreo.connect.discovery.api.PrototypePayload parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static org.wso2.choreo.connect.discovery.api.PrototypePayload parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static org.wso2.choreo.connect.discovery.api.PrototypePayload parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static org.wso2.choreo.connect.discovery.api.PrototypePayload parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }
  public static org.wso2.choreo.connect.discovery.api.PrototypePayload parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static org.wso2.choreo.connect.discovery.api.PrototypePayload parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static org.wso2.choreo.connect.discovery.api.PrototypePayload parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }

  @java.lang.Override
  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }
  public static Builder newBuilder(org.wso2.choreo.connect.discovery.api.PrototypePayload prototype) {
    return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
  }
  @java.lang.Override
  public Builder toBuilder() {
    return this == DEFAULT_INSTANCE
        ? new Builder() : new Builder().mergeFrom(this);
  }

  @java.lang.Override
  protected Builder newBuilderForType(
      com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
    Builder builder = new Builder(parent);
    return builder;
  }
  /**
   * <pre>
   * PrototypePayload holds prototype payload configs in prototype implementations
   * </pre>
   *
   * Protobuf type {@code wso2.discovery.api.PrototypePayload}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:wso2.discovery.api.PrototypePayload)
      org.wso2.choreo.connect.discovery.api.PrototypePayloadOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return org.wso2.choreo.connect.discovery.api.SecurityConfigProto.internal_static_wso2_discovery_api_PrototypePayload_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return org.wso2.choreo.connect.discovery.api.SecurityConfigProto.internal_static_wso2_discovery_api_PrototypePayload_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              org.wso2.choreo.connect.discovery.api.PrototypePayload.class, org.wso2.choreo.connect.discovery.api.PrototypePayload.Builder.class);
    }

    // Construct using org.wso2.choreo.connect.discovery.api.PrototypePayload.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }

    private Builder(
        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);
      maybeForceBuilderInitialization();
    }
    private void maybeForceBuilderInitialization() {
      if (com.google.protobuf.GeneratedMessageV3
              .alwaysUseFieldBuilders) {
      }
    }
    @java.lang.Override
    public Builder clear() {
      super.clear();
      applicationJSON_ = "";

      applicationXML_ = "";

      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return org.wso2.choreo.connect.discovery.api.SecurityConfigProto.internal_static_wso2_discovery_api_PrototypePayload_descriptor;
    }

    @java.lang.Override
    public org.wso2.choreo.connect.discovery.api.PrototypePayload getDefaultInstanceForType() {
      return org.wso2.choreo.connect.discovery.api.PrototypePayload.getDefaultInstance();
    }

    @java.lang.Override
    public org.wso2.choreo.connect.discovery.api.PrototypePayload build() {
      org.wso2.choreo.connect.discovery.api.PrototypePayload result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public org.wso2.choreo.connect.discovery.api.PrototypePayload buildPartial() {
      org.wso2.choreo.connect.discovery.api.PrototypePayload result = new org.wso2.choreo.connect.discovery.api.PrototypePayload(this);
      result.applicationJSON_ = applicationJSON_;
      result.applicationXML_ = applicationXML_;
      onBuilt();
      return result;
    }

    @java.lang.Override
    public Builder clone() {
      return super.clone();
    }
    @java.lang.Override
    public Builder setField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return super.setField(field, value);
    }
    @java.lang.Override
    public Builder clearField(
        com.google.protobuf.Descriptors.FieldDescriptor field) {
      return super.clearField(field);
    }
    @java.lang.Override
    public Builder clearOneof(
        com.google.protobuf.Descriptors.OneofDescriptor oneof) {
      return super.clearOneof(oneof);
    }
    @java.lang.Override
    public Builder setRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        int index, java.lang.Object value) {
      return super.setRepeatedField(field, index, value);
    }
    @java.lang.Override
    public Builder addRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return super.addRepeatedField(field, value);
    }
    @java.lang.Override
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof org.wso2.choreo.connect.discovery.api.PrototypePayload) {
        return mergeFrom((org.wso2.choreo.connect.discovery.api.PrototypePayload)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(org.wso2.choreo.connect.discovery.api.PrototypePayload other) {
      if (other == org.wso2.choreo.connect.discovery.api.PrototypePayload.getDefaultInstance()) return this;
      if (!other.getApplicationJSON().isEmpty()) {
        applicationJSON_ = other.applicationJSON_;
        onChanged();
      }
      if (!other.getApplicationXML().isEmpty()) {
        applicationXML_ = other.applicationXML_;
        onChanged();
      }
      this.mergeUnknownFields(other.unknownFields);
      onChanged();
      return this;
    }

    @java.lang.Override
    public final boolean isInitialized() {
      return true;
    }

    @java.lang.Override
    public Builder mergeFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      org.wso2.choreo.connect.discovery.api.PrototypePayload parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (org.wso2.choreo.connect.discovery.api.PrototypePayload) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private java.lang.Object applicationJSON_ = "";
    /**
     * <code>string applicationJSON = 1;</code>
     * @return The applicationJSON.
     */
    public java.lang.String getApplicationJSON() {
      java.lang.Object ref = applicationJSON_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        applicationJSON_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <code>string applicationJSON = 1;</code>
     * @return The bytes for applicationJSON.
     */
    public com.google.protobuf.ByteString
        getApplicationJSONBytes() {
      java.lang.Object ref = applicationJSON_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        applicationJSON_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <code>string applicationJSON = 1;</code>
     * @param value The applicationJSON to set.
     * @return This builder for chaining.
     */
    public Builder setApplicationJSON(
        java.lang.String value) {
      if (value == null) {
    throw new NullPointerException();
  }
  
      applicationJSON_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>string applicationJSON = 1;</code>
     * @return This builder for chaining.
     */
    public Builder clearApplicationJSON() {
      
      applicationJSON_ = getDefaultInstance().getApplicationJSON();
      onChanged();
      return this;
    }
    /**
     * <code>string applicationJSON = 1;</code>
     * @param value The bytes for applicationJSON to set.
     * @return This builder for chaining.
     */
    public Builder setApplicationJSONBytes(
        com.google.protobuf.ByteString value) {
      if (value == null) {
    throw new NullPointerException();
  }
  checkByteStringIsUtf8(value);
      
      applicationJSON_ = value;
      onChanged();
      return this;
    }

    private java.lang.Object applicationXML_ = "";
    /**
     * <code>string applicationXML = 2;</code>
     * @return The applicationXML.
     */
    public java.lang.String getApplicationXML() {
      java.lang.Object ref = applicationXML_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        applicationXML_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <code>string applicationXML = 2;</code>
     * @return The bytes for applicationXML.
     */
    public com.google.protobuf.ByteString
        getApplicationXMLBytes() {
      java.lang.Object ref = applicationXML_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        applicationXML_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <code>string applicationXML = 2;</code>
     * @param value The applicationXML to set.
     * @return This builder for chaining.
     */
    public Builder setApplicationXML(
        java.lang.String value) {
      if (value == null) {
    throw new NullPointerException();
  }
  
      applicationXML_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>string applicationXML = 2;</code>
     * @return This builder for chaining.
     */
    public Builder clearApplicationXML() {
      
      applicationXML_ = getDefaultInstance().getApplicationXML();
      onChanged();
      return this;
    }
    /**
     * <code>string applicationXML = 2;</code>
     * @param value The bytes for applicationXML to set.
     * @return This builder for chaining.
     */
    public Builder setApplicationXMLBytes(
        com.google.protobuf.ByteString value) {
      if (value == null) {
    throw new NullPointerException();
  }
  checkByteStringIsUtf8(value);
      
      applicationXML_ = value;
      onChanged();
      return this;
    }
    @java.lang.Override
    public final Builder setUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFields(unknownFields);
    }

    @java.lang.Override
    public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }


    // @@protoc_insertion_point(builder_scope:wso2.discovery.api.PrototypePayload)
  }

  // @@protoc_insertion_point(class_scope:wso2.discovery.api.PrototypePayload)
  private static final org.wso2.choreo.connect.discovery.api.PrototypePayload DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new org.wso2.choreo.connect.discovery.api.PrototypePayload();
  }

  public static org.wso2.choreo.connect.discovery.api.PrototypePayload getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<PrototypePayload>
      PARSER = new com.google.protobuf.AbstractParser<PrototypePayload>() {
    @java.lang.Override
    public PrototypePayload parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return new PrototypePayload(input, extensionRegistry);
    }
  };

  public static com.google.protobuf.Parser<PrototypePayload> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<PrototypePayload> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public org.wso2.choreo.connect.discovery.api.PrototypePayload getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

