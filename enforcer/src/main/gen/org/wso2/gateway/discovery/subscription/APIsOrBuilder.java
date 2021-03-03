// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: wso2/discovery/subscription/api.proto

package org.wso2.gateway.discovery.subscription;

public interface APIsOrBuilder extends
    // @@protoc_insertion_point(interface_extends:wso2.discovery.subscription.APIs)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>string apiId = 1;</code>
   * @return The apiId.
   */
  java.lang.String getApiId();
  /**
   * <code>string apiId = 1;</code>
   * @return The bytes for apiId.
   */
  com.google.protobuf.ByteString
      getApiIdBytes();

  /**
   * <code>string name = 2;</code>
   * @return The name.
   */
  java.lang.String getName();
  /**
   * <code>string name = 2;</code>
   * @return The bytes for name.
   */
  com.google.protobuf.ByteString
      getNameBytes();

  /**
   * <code>string provider = 3;</code>
   * @return The provider.
   */
  java.lang.String getProvider();
  /**
   * <code>string provider = 3;</code>
   * @return The bytes for provider.
   */
  com.google.protobuf.ByteString
      getProviderBytes();

  /**
   * <code>string version = 4;</code>
   * @return The version.
   */
  java.lang.String getVersion();
  /**
   * <code>string version = 4;</code>
   * @return The bytes for version.
   */
  com.google.protobuf.ByteString
      getVersionBytes();

  /**
   * <code>string context = 5;</code>
   * @return The context.
   */
  java.lang.String getContext();
  /**
   * <code>string context = 5;</code>
   * @return The bytes for context.
   */
  com.google.protobuf.ByteString
      getContextBytes();

  /**
   * <code>string policy = 6;</code>
   * @return The policy.
   */
  java.lang.String getPolicy();
  /**
   * <code>string policy = 6;</code>
   * @return The bytes for policy.
   */
  com.google.protobuf.ByteString
      getPolicyBytes();

  /**
   * <code>string apiType = 7;</code>
   * @return The apiType.
   */
  java.lang.String getApiType();
  /**
   * <code>string apiType = 7;</code>
   * @return The bytes for apiType.
   */
  com.google.protobuf.ByteString
      getApiTypeBytes();

  /**
   * <code>bool isDefaultVersion = 8;</code>
   * @return The isDefaultVersion.
   */
  boolean getIsDefaultVersion();

  /**
   * <code>.wso2.discovery.subscription.URLMapping urlMappings = 9;</code>
   * @return Whether the urlMappings field is set.
   */
  boolean hasUrlMappings();
  /**
   * <code>.wso2.discovery.subscription.URLMapping urlMappings = 9;</code>
   * @return The urlMappings.
   */
  org.wso2.gateway.discovery.subscription.URLMapping getUrlMappings();
  /**
   * <code>.wso2.discovery.subscription.URLMapping urlMappings = 9;</code>
   */
  org.wso2.gateway.discovery.subscription.URLMappingOrBuilder getUrlMappingsOrBuilder();

  /**
   * <code>string uuid = 10;</code>
   * @return The uuid.
   */
  java.lang.String getUuid();
  /**
   * <code>string uuid = 10;</code>
   * @return The bytes for uuid.
   */
  com.google.protobuf.ByteString
      getUuidBytes();
}
