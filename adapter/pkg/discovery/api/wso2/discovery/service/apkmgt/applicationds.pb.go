// Code generated by protoc-gen-go. DO NOT EDIT.
// versions:
// 	protoc-gen-go v1.25.0-devel
// 	protoc        v3.13.0
// source: wso2/discovery/service/apkmgt/applicationds.proto

package apkmgt

import (
	context "context"
	v3 "github.com/envoyproxy/go-control-plane/envoy/service/discovery/v3"
	grpc "google.golang.org/grpc"
	codes "google.golang.org/grpc/codes"
	status "google.golang.org/grpc/status"
	protoreflect "google.golang.org/protobuf/reflect/protoreflect"
	protoimpl "google.golang.org/protobuf/runtime/protoimpl"
	reflect "reflect"
)

const (
	// Verify that this generated code is sufficiently up-to-date.
	_ = protoimpl.EnforceVersion(20 - protoimpl.MinVersion)
	// Verify that runtime/protoimpl is sufficiently up-to-date.
	_ = protoimpl.EnforceVersion(protoimpl.MaxVersion - 20)
)

var File_wso2_discovery_service_apkmgt_applicationds_proto protoreflect.FileDescriptor

var file_wso2_discovery_service_apkmgt_applicationds_proto_rawDesc = []byte{
	0x0a, 0x31, 0x77, 0x73, 0x6f, 0x32, 0x2f, 0x64, 0x69, 0x73, 0x63, 0x6f, 0x76, 0x65, 0x72, 0x79,
	0x2f, 0x73, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65, 0x2f, 0x61, 0x70, 0x6b, 0x6d, 0x67, 0x74, 0x2f,
	0x61, 0x70, 0x70, 0x6c, 0x69, 0x63, 0x61, 0x74, 0x69, 0x6f, 0x6e, 0x64, 0x73, 0x2e, 0x70, 0x72,
	0x6f, 0x74, 0x6f, 0x12, 0x18, 0x64, 0x69, 0x73, 0x63, 0x6f, 0x76, 0x65, 0x72, 0x79, 0x2e, 0x73,
	0x65, 0x72, 0x76, 0x69, 0x63, 0x65, 0x2e, 0x61, 0x70, 0x6b, 0x6d, 0x67, 0x74, 0x1a, 0x2a, 0x65,
	0x6e, 0x76, 0x6f, 0x79, 0x2f, 0x73, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65, 0x2f, 0x64, 0x69, 0x73,
	0x63, 0x6f, 0x76, 0x65, 0x72, 0x79, 0x2f, 0x76, 0x33, 0x2f, 0x64, 0x69, 0x73, 0x63, 0x6f, 0x76,
	0x65, 0x72, 0x79, 0x2e, 0x70, 0x72, 0x6f, 0x74, 0x6f, 0x32, 0x97, 0x01, 0x0a, 0x16, 0x41, 0x50,
	0x4b, 0x4d, 0x67, 0x74, 0x44, 0x69, 0x73, 0x63, 0x6f, 0x76, 0x65, 0x72, 0x79, 0x53, 0x65, 0x72,
	0x76, 0x69, 0x63, 0x65, 0x12, 0x7d, 0x0a, 0x18, 0x53, 0x74, 0x72, 0x65, 0x61, 0x6d, 0x41, 0x50,
	0x4b, 0x4d, 0x67, 0x74, 0x41, 0x70, 0x70, 0x6c, 0x69, 0x63, 0x61, 0x74, 0x69, 0x6f, 0x6e, 0x73,
	0x12, 0x2c, 0x2e, 0x65, 0x6e, 0x76, 0x6f, 0x79, 0x2e, 0x73, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65,
	0x2e, 0x64, 0x69, 0x73, 0x63, 0x6f, 0x76, 0x65, 0x72, 0x79, 0x2e, 0x76, 0x33, 0x2e, 0x44, 0x69,
	0x73, 0x63, 0x6f, 0x76, 0x65, 0x72, 0x79, 0x52, 0x65, 0x71, 0x75, 0x65, 0x73, 0x74, 0x1a, 0x2d,
	0x2e, 0x65, 0x6e, 0x76, 0x6f, 0x79, 0x2e, 0x73, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65, 0x2e, 0x64,
	0x69, 0x73, 0x63, 0x6f, 0x76, 0x65, 0x72, 0x79, 0x2e, 0x76, 0x33, 0x2e, 0x44, 0x69, 0x73, 0x63,
	0x6f, 0x76, 0x65, 0x72, 0x79, 0x52, 0x65, 0x73, 0x70, 0x6f, 0x6e, 0x73, 0x65, 0x22, 0x00, 0x28,
	0x01, 0x30, 0x01, 0x42, 0x91, 0x01, 0x0a, 0x30, 0x6f, 0x72, 0x67, 0x2e, 0x77, 0x73, 0x6f, 0x32,
	0x2e, 0x63, 0x68, 0x6f, 0x72, 0x65, 0x6f, 0x2e, 0x63, 0x6f, 0x6e, 0x6e, 0x65, 0x63, 0x74, 0x2e,
	0x64, 0x69, 0x73, 0x63, 0x6f, 0x76, 0x65, 0x72, 0x79, 0x2e, 0x73, 0x65, 0x72, 0x76, 0x69, 0x63,
	0x65, 0x2e, 0x61, 0x70, 0x6b, 0x6d, 0x67, 0x74, 0x42, 0x12, 0x41, 0x70, 0x70, 0x6c, 0x69, 0x63,
	0x61, 0x74, 0x69, 0x6f, 0x6e, 0x44, 0x73, 0x50, 0x72, 0x6f, 0x74, 0x6f, 0x50, 0x00, 0x5a, 0x44,
	0x67, 0x69, 0x74, 0x68, 0x75, 0x62, 0x2e, 0x63, 0x6f, 0x6d, 0x2f, 0x65, 0x6e, 0x76, 0x6f, 0x79,
	0x70, 0x72, 0x6f, 0x78, 0x79, 0x2f, 0x67, 0x6f, 0x2d, 0x63, 0x6f, 0x6e, 0x74, 0x72, 0x6f, 0x6c,
	0x2d, 0x70, 0x6c, 0x61, 0x6e, 0x65, 0x2f, 0x77, 0x73, 0x6f, 0x32, 0x2f, 0x64, 0x69, 0x73, 0x63,
	0x6f, 0x76, 0x65, 0x72, 0x79, 0x2f, 0x73, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65, 0x2f, 0x61, 0x70,
	0x6b, 0x6d, 0x67, 0x74, 0x88, 0x01, 0x01, 0x62, 0x06, 0x70, 0x72, 0x6f, 0x74, 0x6f, 0x33,
}

var file_wso2_discovery_service_apkmgt_applicationds_proto_goTypes = []interface{}{
	(*v3.DiscoveryRequest)(nil),  // 0: envoy.service.discovery.v3.DiscoveryRequest
	(*v3.DiscoveryResponse)(nil), // 1: envoy.service.discovery.v3.DiscoveryResponse
}
var file_wso2_discovery_service_apkmgt_applicationds_proto_depIdxs = []int32{
	0, // 0: discovery.service.apkmgt.APKMgtDiscoveryService.StreamAPKMgtApplications:input_type -> envoy.service.discovery.v3.DiscoveryRequest
	1, // 1: discovery.service.apkmgt.APKMgtDiscoveryService.StreamAPKMgtApplications:output_type -> envoy.service.discovery.v3.DiscoveryResponse
	1, // [1:2] is the sub-list for method output_type
	0, // [0:1] is the sub-list for method input_type
	0, // [0:0] is the sub-list for extension type_name
	0, // [0:0] is the sub-list for extension extendee
	0, // [0:0] is the sub-list for field type_name
}

func init() { file_wso2_discovery_service_apkmgt_applicationds_proto_init() }
func file_wso2_discovery_service_apkmgt_applicationds_proto_init() {
	if File_wso2_discovery_service_apkmgt_applicationds_proto != nil {
		return
	}
	type x struct{}
	out := protoimpl.TypeBuilder{
		File: protoimpl.DescBuilder{
			GoPackagePath: reflect.TypeOf(x{}).PkgPath(),
			RawDescriptor: file_wso2_discovery_service_apkmgt_applicationds_proto_rawDesc,
			NumEnums:      0,
			NumMessages:   0,
			NumExtensions: 0,
			NumServices:   1,
		},
		GoTypes:           file_wso2_discovery_service_apkmgt_applicationds_proto_goTypes,
		DependencyIndexes: file_wso2_discovery_service_apkmgt_applicationds_proto_depIdxs,
	}.Build()
	File_wso2_discovery_service_apkmgt_applicationds_proto = out.File
	file_wso2_discovery_service_apkmgt_applicationds_proto_rawDesc = nil
	file_wso2_discovery_service_apkmgt_applicationds_proto_goTypes = nil
	file_wso2_discovery_service_apkmgt_applicationds_proto_depIdxs = nil
}

// Reference imports to suppress errors if they are not otherwise used.
var _ context.Context
var _ grpc.ClientConnInterface

// This is a compile-time assertion to ensure that this generated file
// is compatible with the grpc package it is being compiled against.
const _ = grpc.SupportPackageIsVersion6

// APKMgtDiscoveryServiceClient is the client API for APKMgtDiscoveryService service.
//
// For semantics around ctx use and closing/ending streaming RPCs, please refer to https://godoc.org/google.golang.org/grpc#ClientConn.NewStream.
type APKMgtDiscoveryServiceClient interface {
	StreamAPKMgtApplications(ctx context.Context, opts ...grpc.CallOption) (APKMgtDiscoveryService_StreamAPKMgtApplicationsClient, error)
}

type aPKMgtDiscoveryServiceClient struct {
	cc grpc.ClientConnInterface
}

func NewAPKMgtDiscoveryServiceClient(cc grpc.ClientConnInterface) APKMgtDiscoveryServiceClient {
	return &aPKMgtDiscoveryServiceClient{cc}
}

func (c *aPKMgtDiscoveryServiceClient) StreamAPKMgtApplications(ctx context.Context, opts ...grpc.CallOption) (APKMgtDiscoveryService_StreamAPKMgtApplicationsClient, error) {
	stream, err := c.cc.NewStream(ctx, &_APKMgtDiscoveryService_serviceDesc.Streams[0], "/discovery.service.apkmgt.APKMgtDiscoveryService/StreamAPKMgtApplications", opts...)
	if err != nil {
		return nil, err
	}
	x := &aPKMgtDiscoveryServiceStreamAPKMgtApplicationsClient{stream}
	return x, nil
}

type APKMgtDiscoveryService_StreamAPKMgtApplicationsClient interface {
	Send(*v3.DiscoveryRequest) error
	Recv() (*v3.DiscoveryResponse, error)
	grpc.ClientStream
}

type aPKMgtDiscoveryServiceStreamAPKMgtApplicationsClient struct {
	grpc.ClientStream
}

func (x *aPKMgtDiscoveryServiceStreamAPKMgtApplicationsClient) Send(m *v3.DiscoveryRequest) error {
	return x.ClientStream.SendMsg(m)
}

func (x *aPKMgtDiscoveryServiceStreamAPKMgtApplicationsClient) Recv() (*v3.DiscoveryResponse, error) {
	m := new(v3.DiscoveryResponse)
	if err := x.ClientStream.RecvMsg(m); err != nil {
		return nil, err
	}
	return m, nil
}

// APKMgtDiscoveryServiceServer is the server API for APKMgtDiscoveryService service.
type APKMgtDiscoveryServiceServer interface {
	StreamAPKMgtApplications(APKMgtDiscoveryService_StreamAPKMgtApplicationsServer) error
}

// UnimplementedAPKMgtDiscoveryServiceServer can be embedded to have forward compatible implementations.
type UnimplementedAPKMgtDiscoveryServiceServer struct {
}

func (*UnimplementedAPKMgtDiscoveryServiceServer) StreamAPKMgtApplications(APKMgtDiscoveryService_StreamAPKMgtApplicationsServer) error {
	return status.Errorf(codes.Unimplemented, "method StreamAPKMgtApplications not implemented")
}

func RegisterAPKMgtDiscoveryServiceServer(s *grpc.Server, srv APKMgtDiscoveryServiceServer) {
	s.RegisterService(&_APKMgtDiscoveryService_serviceDesc, srv)
}

func _APKMgtDiscoveryService_StreamAPKMgtApplications_Handler(srv interface{}, stream grpc.ServerStream) error {
	return srv.(APKMgtDiscoveryServiceServer).StreamAPKMgtApplications(&aPKMgtDiscoveryServiceStreamAPKMgtApplicationsServer{stream})
}

type APKMgtDiscoveryService_StreamAPKMgtApplicationsServer interface {
	Send(*v3.DiscoveryResponse) error
	Recv() (*v3.DiscoveryRequest, error)
	grpc.ServerStream
}

type aPKMgtDiscoveryServiceStreamAPKMgtApplicationsServer struct {
	grpc.ServerStream
}

func (x *aPKMgtDiscoveryServiceStreamAPKMgtApplicationsServer) Send(m *v3.DiscoveryResponse) error {
	return x.ServerStream.SendMsg(m)
}

func (x *aPKMgtDiscoveryServiceStreamAPKMgtApplicationsServer) Recv() (*v3.DiscoveryRequest, error) {
	m := new(v3.DiscoveryRequest)
	if err := x.ServerStream.RecvMsg(m); err != nil {
		return nil, err
	}
	return m, nil
}

var _APKMgtDiscoveryService_serviceDesc = grpc.ServiceDesc{
	ServiceName: "discovery.service.apkmgt.APKMgtDiscoveryService",
	HandlerType: (*APKMgtDiscoveryServiceServer)(nil),
	Methods:     []grpc.MethodDesc{},
	Streams: []grpc.StreamDesc{
		{
			StreamName:    "StreamAPKMgtApplications",
			Handler:       _APKMgtDiscoveryService_StreamAPKMgtApplications_Handler,
			ServerStreams: true,
			ClientStreams: true,
		},
	},
	Metadata: "wso2/discovery/service/apkmgt/applicationds.proto",
}
