// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: wso2/discovery/config/enforcer/config.proto

package org.wso2.choreo.connect.discovery.config.enforcer;

public final class ConfigProto {
  private ConfigProto() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_wso2_discovery_config_enforcer_Config_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_wso2_discovery_config_enforcer_Config_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n+wso2/discovery/config/enforcer/config." +
      "proto\022\036wso2.discovery.config.enforcer\032)w" +
      "so2/discovery/config/enforcer/cert.proto" +
      "\032,wso2/discovery/config/enforcer/service" +
      ".proto\0322wso2/discovery/config/enforcer/j" +
      "wt_generator.proto\032/wso2/discovery/confi" +
      "g/enforcer/jwt_issuer.proto\032/wso2/discov" +
      "ery/config/enforcer/throttling.proto\032*ws" +
      "o2/discovery/config/enforcer/cache.proto" +
      "\032.wso2/discovery/config/enforcer/analyti" +
      "cs.proto\032-wso2/discovery/config/enforcer" +
      "/security.proto\"\264\004\n\006Config\022:\n\010security\030\001" +
      " \001(\0132(.wso2.discovery.config.enforcer.Se" +
      "curity\022;\n\010keystore\030\002 \001(\0132).wso2.discover" +
      "y.config.enforcer.CertStore\022=\n\ntruststor" +
      "e\030\003 \001(\0132).wso2.discovery.config.enforcer" +
      ".CertStore\022<\n\013authService\030\004 \001(\0132\'.wso2.d" +
      "iscovery.config.enforcer.Service\022B\n\014jwtG" +
      "enerator\030\005 \001(\0132,.wso2.discovery.config.e" +
      "nforcer.JWTGenerator\022>\n\nthrottling\030\006 \001(\013" +
      "2*.wso2.discovery.config.enforcer.Thrott" +
      "ling\0224\n\005cache\030\007 \001(\0132%.wso2.discovery.con" +
      "fig.enforcer.Cache\022<\n\tjwtIssuer\030\010 \001(\0132)." +
      "wso2.discovery.config.enforcer.JWTIssuer" +
      "\022<\n\tanalytics\030\t \001(\0132).wso2.discovery.con" +
      "fig.enforcer.AnalyticsB\222\001\n1org.wso2.chor" +
      "eo.connect.discovery.config.enforcerB\013Co" +
      "nfigProtoP\001ZNgithub.com/envoyproxy/go-co" +
      "ntrol-plane/wso2/discovery/config/enforc" +
      "er;enforcerb\006proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
          org.wso2.choreo.connect.discovery.config.enforcer.CertStoreProto.getDescriptor(),
          org.wso2.choreo.connect.discovery.config.enforcer.ServiceProto.getDescriptor(),
          org.wso2.choreo.connect.discovery.config.enforcer.JWTGeneratorProto.getDescriptor(),
          org.wso2.choreo.connect.discovery.config.enforcer.JWTIssuerProto.getDescriptor(),
          org.wso2.choreo.connect.discovery.config.enforcer.ThrottlingProto.getDescriptor(),
          org.wso2.choreo.connect.discovery.config.enforcer.CacheProto.getDescriptor(),
          org.wso2.choreo.connect.discovery.config.enforcer.AnalyticsProto.getDescriptor(),
          org.wso2.choreo.connect.discovery.config.enforcer.SecurityProto.getDescriptor(),
        });
    internal_static_wso2_discovery_config_enforcer_Config_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_wso2_discovery_config_enforcer_Config_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_wso2_discovery_config_enforcer_Config_descriptor,
        new java.lang.String[] { "Security", "Keystore", "Truststore", "AuthService", "JwtGenerator", "Throttling", "Cache", "JwtIssuer", "Analytics", });
    org.wso2.choreo.connect.discovery.config.enforcer.CertStoreProto.getDescriptor();
    org.wso2.choreo.connect.discovery.config.enforcer.ServiceProto.getDescriptor();
    org.wso2.choreo.connect.discovery.config.enforcer.JWTGeneratorProto.getDescriptor();
    org.wso2.choreo.connect.discovery.config.enforcer.JWTIssuerProto.getDescriptor();
    org.wso2.choreo.connect.discovery.config.enforcer.ThrottlingProto.getDescriptor();
    org.wso2.choreo.connect.discovery.config.enforcer.CacheProto.getDescriptor();
    org.wso2.choreo.connect.discovery.config.enforcer.AnalyticsProto.getDescriptor();
    org.wso2.choreo.connect.discovery.config.enforcer.SecurityProto.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
