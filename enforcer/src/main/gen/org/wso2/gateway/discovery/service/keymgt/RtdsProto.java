// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: wso2/discovery/service/keymgt/rtds.proto

package org.wso2.gateway.discovery.service.keymgt;

public final class RtdsProto {
  private RtdsProto() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n(wso2/discovery/service/keymgt/rtds.pro" +
      "to\022\035wso2.discovery.service.keymgt\032*envoy" +
      "/service/discovery/v3/discovery.proto2\377\001" +
      "\n\034RevokedTokenDiscoveryService\022q\n\014Stream" +
      "Tokens\022,.envoy.service.discovery.v3.Disc" +
      "overyRequest\032-.envoy.service.discovery.v" +
      "3.DiscoveryResponse\"\000(\0010\001\022l\n\013FetchTokens" +
      "\022,.envoy.service.discovery.v3.DiscoveryR" +
      "equest\032-.envoy.service.discovery.v3.Disc" +
      "overyResponse\"\000B\201\001\n)org.wso2.gateway.dis" +
      "covery.service.keymgtB\tRtdsProtoP\001ZDgith" +
      "ub.com/envoyproxy/go-control-plane/wso2/" +
      "discovery/service/keymgt\210\001\001b\006proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
          io.envoyproxy.envoy.service.discovery.v3.DiscoveryProto.getDescriptor(),
        });
    io.envoyproxy.envoy.service.discovery.v3.DiscoveryProto.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
