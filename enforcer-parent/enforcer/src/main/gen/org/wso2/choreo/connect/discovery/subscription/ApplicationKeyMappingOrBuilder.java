// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: wso2/discovery/subscription/application_key_mapping.proto

package org.wso2.choreo.connect.discovery.subscription;

public interface ApplicationKeyMappingOrBuilder extends
    // @@protoc_insertion_point(interface_extends:wso2.discovery.subscription.ApplicationKeyMapping)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>string consumerKey = 1;</code>
   * @return The consumerKey.
   */
  java.lang.String getConsumerKey();
  /**
   * <code>string consumerKey = 1;</code>
   * @return The bytes for consumerKey.
   */
  com.google.protobuf.ByteString
      getConsumerKeyBytes();

  /**
   * <code>string keyType = 2;</code>
   * @return The keyType.
   */
  java.lang.String getKeyType();
  /**
   * <code>string keyType = 2;</code>
   * @return The bytes for keyType.
   */
  com.google.protobuf.ByteString
      getKeyTypeBytes();

  /**
   * <code>string keyManager = 3;</code>
   * @return The keyManager.
   */
  java.lang.String getKeyManager();
  /**
   * <code>string keyManager = 3;</code>
   * @return The bytes for keyManager.
   */
  com.google.protobuf.ByteString
      getKeyManagerBytes();

  /**
   * <code>int32 applicationId = 4;</code>
   * @return The applicationId.
   */
  int getApplicationId();

  /**
   * <code>int32 tenantId = 5;</code>
   * @return The tenantId.
   */
  int getTenantId();

  /**
   * <code>string tenantDomain = 6;</code>
   * @return The tenantDomain.
   */
  java.lang.String getTenantDomain();
  /**
   * <code>string tenantDomain = 6;</code>
   * @return The bytes for tenantDomain.
   */
  com.google.protobuf.ByteString
      getTenantDomainBytes();

  /**
   * <code>int64 timestamp = 7;</code>
   * @return The timestamp.
   */
  long getTimestamp();

  /**
   * <code>string applicationUUID = 8;</code>
   * @return The applicationUUID.
   */
  java.lang.String getApplicationUUID();
  /**
   * <code>string applicationUUID = 8;</code>
   * @return The bytes for applicationUUID.
   */
  com.google.protobuf.ByteString
      getApplicationUUIDBytes();

  /**
   * <code>string envId = 9;</code>
   * @return The envId.
   */
  java.lang.String getEnvId();
  /**
   * <code>string envId = 9;</code>
   * @return The bytes for envId.
   */
  com.google.protobuf.ByteString
      getEnvIdBytes();
}
