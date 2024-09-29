// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: wso2/discovery/config/enforcer/pat_config.proto

package org.wso2.choreo.connect.discovery.config.enforcer;

/**
 * Protobuf type {@code wso2.discovery.config.enforcer.PATConfig}
 */
public final class PATConfig extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:wso2.discovery.config.enforcer.PATConfig)
    PATConfigOrBuilder {
private static final long serialVersionUID = 0L;
  // Use PATConfig.newBuilder() to construct.
  private PATConfig(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private PATConfig() {
  }

  @java.lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(
      UnusedPrivateParameter unused) {
    return new PATConfig();
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return this.unknownFields;
  }
  private PATConfig(
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
          case 8: {

            tokenExpiryTimeSkew_ = input.readInt32();
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
    return org.wso2.choreo.connect.discovery.config.enforcer.PATConfigProto.internal_static_wso2_discovery_config_enforcer_PATConfig_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return org.wso2.choreo.connect.discovery.config.enforcer.PATConfigProto.internal_static_wso2_discovery_config_enforcer_PATConfig_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            org.wso2.choreo.connect.discovery.config.enforcer.PATConfig.class, org.wso2.choreo.connect.discovery.config.enforcer.PATConfig.Builder.class);
  }

  public static final int TOKENEXPIRYTIMESKEW_FIELD_NUMBER = 1;
  private int tokenExpiryTimeSkew_;
  /**
   * <code>int32 tokenExpiryTimeSkew = 1;</code>
   * @return The tokenExpiryTimeSkew.
   */
  @java.lang.Override
  public int getTokenExpiryTimeSkew() {
    return tokenExpiryTimeSkew_;
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
    if (tokenExpiryTimeSkew_ != 0) {
      output.writeInt32(1, tokenExpiryTimeSkew_);
    }
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (tokenExpiryTimeSkew_ != 0) {
      size += com.google.protobuf.CodedOutputStream
        .computeInt32Size(1, tokenExpiryTimeSkew_);
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
    if (!(obj instanceof org.wso2.choreo.connect.discovery.config.enforcer.PATConfig)) {
      return super.equals(obj);
    }
    org.wso2.choreo.connect.discovery.config.enforcer.PATConfig other = (org.wso2.choreo.connect.discovery.config.enforcer.PATConfig) obj;

    if (getTokenExpiryTimeSkew()
        != other.getTokenExpiryTimeSkew()) return false;
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
    hash = (37 * hash) + TOKENEXPIRYTIMESKEW_FIELD_NUMBER;
    hash = (53 * hash) + getTokenExpiryTimeSkew();
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static org.wso2.choreo.connect.discovery.config.enforcer.PATConfig parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static org.wso2.choreo.connect.discovery.config.enforcer.PATConfig parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static org.wso2.choreo.connect.discovery.config.enforcer.PATConfig parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static org.wso2.choreo.connect.discovery.config.enforcer.PATConfig parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static org.wso2.choreo.connect.discovery.config.enforcer.PATConfig parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static org.wso2.choreo.connect.discovery.config.enforcer.PATConfig parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static org.wso2.choreo.connect.discovery.config.enforcer.PATConfig parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static org.wso2.choreo.connect.discovery.config.enforcer.PATConfig parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static org.wso2.choreo.connect.discovery.config.enforcer.PATConfig parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }
  public static org.wso2.choreo.connect.discovery.config.enforcer.PATConfig parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static org.wso2.choreo.connect.discovery.config.enforcer.PATConfig parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static org.wso2.choreo.connect.discovery.config.enforcer.PATConfig parseFrom(
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
  public static Builder newBuilder(org.wso2.choreo.connect.discovery.config.enforcer.PATConfig prototype) {
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
   * Protobuf type {@code wso2.discovery.config.enforcer.PATConfig}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:wso2.discovery.config.enforcer.PATConfig)
      org.wso2.choreo.connect.discovery.config.enforcer.PATConfigOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return org.wso2.choreo.connect.discovery.config.enforcer.PATConfigProto.internal_static_wso2_discovery_config_enforcer_PATConfig_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return org.wso2.choreo.connect.discovery.config.enforcer.PATConfigProto.internal_static_wso2_discovery_config_enforcer_PATConfig_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              org.wso2.choreo.connect.discovery.config.enforcer.PATConfig.class, org.wso2.choreo.connect.discovery.config.enforcer.PATConfig.Builder.class);
    }

    // Construct using org.wso2.choreo.connect.discovery.config.enforcer.PATConfig.newBuilder()
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
      tokenExpiryTimeSkew_ = 0;

      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return org.wso2.choreo.connect.discovery.config.enforcer.PATConfigProto.internal_static_wso2_discovery_config_enforcer_PATConfig_descriptor;
    }

    @java.lang.Override
    public org.wso2.choreo.connect.discovery.config.enforcer.PATConfig getDefaultInstanceForType() {
      return org.wso2.choreo.connect.discovery.config.enforcer.PATConfig.getDefaultInstance();
    }

    @java.lang.Override
    public org.wso2.choreo.connect.discovery.config.enforcer.PATConfig build() {
      org.wso2.choreo.connect.discovery.config.enforcer.PATConfig result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public org.wso2.choreo.connect.discovery.config.enforcer.PATConfig buildPartial() {
      org.wso2.choreo.connect.discovery.config.enforcer.PATConfig result = new org.wso2.choreo.connect.discovery.config.enforcer.PATConfig(this);
      result.tokenExpiryTimeSkew_ = tokenExpiryTimeSkew_;
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
      if (other instanceof org.wso2.choreo.connect.discovery.config.enforcer.PATConfig) {
        return mergeFrom((org.wso2.choreo.connect.discovery.config.enforcer.PATConfig)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(org.wso2.choreo.connect.discovery.config.enforcer.PATConfig other) {
      if (other == org.wso2.choreo.connect.discovery.config.enforcer.PATConfig.getDefaultInstance()) return this;
      if (other.getTokenExpiryTimeSkew() != 0) {
        setTokenExpiryTimeSkew(other.getTokenExpiryTimeSkew());
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
      org.wso2.choreo.connect.discovery.config.enforcer.PATConfig parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (org.wso2.choreo.connect.discovery.config.enforcer.PATConfig) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private int tokenExpiryTimeSkew_ ;
    /**
     * <code>int32 tokenExpiryTimeSkew = 1;</code>
     * @return The tokenExpiryTimeSkew.
     */
    @java.lang.Override
    public int getTokenExpiryTimeSkew() {
      return tokenExpiryTimeSkew_;
    }
    /**
     * <code>int32 tokenExpiryTimeSkew = 1;</code>
     * @param value The tokenExpiryTimeSkew to set.
     * @return This builder for chaining.
     */
    public Builder setTokenExpiryTimeSkew(int value) {
      
      tokenExpiryTimeSkew_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>int32 tokenExpiryTimeSkew = 1;</code>
     * @return This builder for chaining.
     */
    public Builder clearTokenExpiryTimeSkew() {
      
      tokenExpiryTimeSkew_ = 0;
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


    // @@protoc_insertion_point(builder_scope:wso2.discovery.config.enforcer.PATConfig)
  }

  // @@protoc_insertion_point(class_scope:wso2.discovery.config.enforcer.PATConfig)
  private static final org.wso2.choreo.connect.discovery.config.enforcer.PATConfig DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new org.wso2.choreo.connect.discovery.config.enforcer.PATConfig();
  }

  public static org.wso2.choreo.connect.discovery.config.enforcer.PATConfig getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<PATConfig>
      PARSER = new com.google.protobuf.AbstractParser<PATConfig>() {
    @java.lang.Override
    public PATConfig parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return new PATConfig(input, extensionRegistry);
    }
  };

  public static com.google.protobuf.Parser<PATConfig> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<PATConfig> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public org.wso2.choreo.connect.discovery.config.enforcer.PATConfig getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

