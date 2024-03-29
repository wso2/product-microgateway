// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: wso2/discovery/api/api.proto

package org.wso2.choreo.connect.discovery.api;

public final class ApiProto {
  private ApiProto() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_wso2_discovery_api_Api_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_wso2_discovery_api_Api_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\034wso2/discovery/api/api.proto\022\022wso2.dis" +
      "covery.api\032)wso2/discovery/api/endpoint_" +
      "cluster.proto\032!wso2/discovery/api/Resour" +
      "ce.proto\032*wso2/discovery/api/endpoint_se" +
      "curity.proto\032(wso2/discovery/api/securit" +
      "y_scheme.proto\032$wso2/discovery/api/Certi" +
      "ficate.proto\032 wso2/discovery/api/graphql" +
      ".proto\"\306\006\n\003Api\022\n\n\002id\030\001 \001(\t\022\r\n\005title\030\002 \001(" +
      "\t\022\017\n\007version\030\003 \001(\t\022\017\n\007apiType\030\004 \001(\t\022\023\n\013d" +
      "escription\030\005 \001(\t\022@\n\023productionEndpoints\030" +
      "\006 \001(\0132#.wso2.discovery.api.EndpointClust" +
      "er\022=\n\020sandboxEndpoints\030\007 \001(\0132#.wso2.disc" +
      "overy.api.EndpointCluster\022/\n\tresources\030\010" +
      " \003(\0132\034.wso2.discovery.api.Resource\022\020\n\010ba" +
      "sePath\030\t \001(\t\022\014\n\004tier\030\n \001(\t\022\031\n\021apiLifeCyc" +
      "leState\030\013 \001(\t\022:\n\016securityScheme\030\014 \003(\0132\"." +
      "wso2.discovery.api.SecurityScheme\0222\n\010sec" +
      "urity\030\r \003(\0132 .wso2.discovery.api.Securit" +
      "yList\022>\n\020endpointSecurity\030\016 \001(\0132$.wso2.d" +
      "iscovery.api.EndpointSecurity\022\033\n\023authori" +
      "zationHeader\030\017 \001(\t\022\027\n\017disableSecurity\030\020 " +
      "\001(\010\022\r\n\005vhost\030\021 \001(\t\022\026\n\016organizationId\030\022 \001" +
      "(\t\022\023\n\013isMockedApi\030\023 \001(\010\022;\n\022clientCertifi" +
      "cates\030\024 \003(\0132\037.wso2.discovery.api.Certifi" +
      "cate\022\021\n\tmutualSSL\030\025 \001(\t\022\033\n\023applicationSe" +
      "curity\030\026 \001(\010\022\025\n\rgraphQLSchema\030\027 \001(\t\022D\n\025g" +
      "raphqlComplexityInfo\030\030 \003(\0132%.wso2.discov" +
      "ery.api.GraphqlComplexity\022\024\n\014endpointTyp" +
      "e\030\031 \001(\tBr\n%org.wso2.choreo.connect.disco" +
      "very.apiB\010ApiProtoP\001Z=github.com/envoypr" +
      "oxy/go-control-plane/wso2/discovery/api;" +
      "apib\006proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
          org.wso2.choreo.connect.discovery.api.EndpointClusterProto.getDescriptor(),
          org.wso2.choreo.connect.discovery.api.ResourceProto.getDescriptor(),
          org.wso2.choreo.connect.discovery.api.EndpointSecurityProto.getDescriptor(),
          org.wso2.choreo.connect.discovery.api.SecuritySchemeProto.getDescriptor(),
          org.wso2.choreo.connect.discovery.api.CertificateProto.getDescriptor(),
          org.wso2.choreo.connect.discovery.api.GraphQLProto.getDescriptor(),
        });
    internal_static_wso2_discovery_api_Api_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_wso2_discovery_api_Api_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_wso2_discovery_api_Api_descriptor,
        new java.lang.String[] { "Id", "Title", "Version", "ApiType", "Description", "ProductionEndpoints", "SandboxEndpoints", "Resources", "BasePath", "Tier", "ApiLifeCycleState", "SecurityScheme", "Security", "EndpointSecurity", "AuthorizationHeader", "DisableSecurity", "Vhost", "OrganizationId", "IsMockedApi", "ClientCertificates", "MutualSSL", "ApplicationSecurity", "GraphQLSchema", "GraphqlComplexityInfo", "EndpointType", });
    org.wso2.choreo.connect.discovery.api.EndpointClusterProto.getDescriptor();
    org.wso2.choreo.connect.discovery.api.ResourceProto.getDescriptor();
    org.wso2.choreo.connect.discovery.api.EndpointSecurityProto.getDescriptor();
    org.wso2.choreo.connect.discovery.api.SecuritySchemeProto.getDescriptor();
    org.wso2.choreo.connect.discovery.api.CertificateProto.getDescriptor();
    org.wso2.choreo.connect.discovery.api.GraphQLProto.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
