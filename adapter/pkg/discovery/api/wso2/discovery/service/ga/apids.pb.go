// Code generated by protoc-gen-go. DO NOT EDIT.
// versions:
// 	protoc-gen-go v1.25.0-devel
// 	protoc        v3.13.0
// source: wso2/discovery/service/ga/apids.proto

package ga

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

var File_wso2_discovery_service_ga_apids_proto protoreflect.FileDescriptor

var file_wso2_discovery_service_ga_apids_proto_rawDesc = []byte{
	0x0a, 0x25, 0x77, 0x73, 0x6f, 0x32, 0x2f, 0x64, 0x69, 0x73, 0x63, 0x6f, 0x76, 0x65, 0x72, 0x79,
	0x2f, 0x73, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65, 0x2f, 0x67, 0x61, 0x2f, 0x61, 0x70, 0x69, 0x64,
	0x73, 0x2e, 0x70, 0x72, 0x6f, 0x74, 0x6f, 0x12, 0x14, 0x64, 0x69, 0x73, 0x63, 0x6f, 0x76, 0x65,
	0x72, 0x79, 0x2e, 0x73, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65, 0x2e, 0x67, 0x61, 0x1a, 0x2a, 0x65,
	0x6e, 0x76, 0x6f, 0x79, 0x2f, 0x73, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65, 0x2f, 0x64, 0x69, 0x73,
	0x63, 0x6f, 0x76, 0x65, 0x72, 0x79, 0x2f, 0x76, 0x33, 0x2f, 0x64, 0x69, 0x73, 0x63, 0x6f, 0x76,
	0x65, 0x72, 0x79, 0x2e, 0x70, 0x72, 0x6f, 0x74, 0x6f, 0x32, 0xf8, 0x01, 0x0a, 0x15, 0x41, 0x70,
	0x69, 0x47, 0x41, 0x44, 0x69, 0x73, 0x63, 0x6f, 0x76, 0x65, 0x72, 0x79, 0x53, 0x65, 0x72, 0x76,
	0x69, 0x63, 0x65, 0x12, 0x71, 0x0a, 0x0c, 0x53, 0x74, 0x72, 0x65, 0x61, 0x6d, 0x47, 0x41, 0x41,
	0x70, 0x69, 0x73, 0x12, 0x2c, 0x2e, 0x65, 0x6e, 0x76, 0x6f, 0x79, 0x2e, 0x73, 0x65, 0x72, 0x76,
	0x69, 0x63, 0x65, 0x2e, 0x64, 0x69, 0x73, 0x63, 0x6f, 0x76, 0x65, 0x72, 0x79, 0x2e, 0x76, 0x33,
	0x2e, 0x44, 0x69, 0x73, 0x63, 0x6f, 0x76, 0x65, 0x72, 0x79, 0x52, 0x65, 0x71, 0x75, 0x65, 0x73,
	0x74, 0x1a, 0x2d, 0x2e, 0x65, 0x6e, 0x76, 0x6f, 0x79, 0x2e, 0x73, 0x65, 0x72, 0x76, 0x69, 0x63,
	0x65, 0x2e, 0x64, 0x69, 0x73, 0x63, 0x6f, 0x76, 0x65, 0x72, 0x79, 0x2e, 0x76, 0x33, 0x2e, 0x44,
	0x69, 0x73, 0x63, 0x6f, 0x76, 0x65, 0x72, 0x79, 0x52, 0x65, 0x73, 0x70, 0x6f, 0x6e, 0x73, 0x65,
	0x22, 0x00, 0x28, 0x01, 0x30, 0x01, 0x12, 0x6c, 0x0a, 0x0b, 0x46, 0x65, 0x74, 0x63, 0x68, 0x47,
	0x41, 0x41, 0x70, 0x69, 0x73, 0x12, 0x2c, 0x2e, 0x65, 0x6e, 0x76, 0x6f, 0x79, 0x2e, 0x73, 0x65,
	0x72, 0x76, 0x69, 0x63, 0x65, 0x2e, 0x64, 0x69, 0x73, 0x63, 0x6f, 0x76, 0x65, 0x72, 0x79, 0x2e,
	0x76, 0x33, 0x2e, 0x44, 0x69, 0x73, 0x63, 0x6f, 0x76, 0x65, 0x72, 0x79, 0x52, 0x65, 0x71, 0x75,
	0x65, 0x73, 0x74, 0x1a, 0x2d, 0x2e, 0x65, 0x6e, 0x76, 0x6f, 0x79, 0x2e, 0x73, 0x65, 0x72, 0x76,
	0x69, 0x63, 0x65, 0x2e, 0x64, 0x69, 0x73, 0x63, 0x6f, 0x76, 0x65, 0x72, 0x79, 0x2e, 0x76, 0x33,
	0x2e, 0x44, 0x69, 0x73, 0x63, 0x6f, 0x76, 0x65, 0x72, 0x79, 0x52, 0x65, 0x73, 0x70, 0x6f, 0x6e,
	0x73, 0x65, 0x22, 0x00, 0x42, 0x81, 0x01, 0x0a, 0x2c, 0x6f, 0x72, 0x67, 0x2e, 0x77, 0x73, 0x6f,
	0x32, 0x2e, 0x63, 0x68, 0x6f, 0x72, 0x65, 0x6f, 0x2e, 0x63, 0x6f, 0x6e, 0x6e, 0x65, 0x63, 0x74,
	0x2e, 0x64, 0x69, 0x73, 0x63, 0x6f, 0x76, 0x65, 0x72, 0x79, 0x2e, 0x73, 0x65, 0x72, 0x76, 0x69,
	0x63, 0x65, 0x2e, 0x67, 0x61, 0x42, 0x0a, 0x41, 0x50, 0x49, 0x44, 0x73, 0x50, 0x72, 0x6f, 0x74,
	0x6f, 0x50, 0x00, 0x5a, 0x40, 0x67, 0x69, 0x74, 0x68, 0x75, 0x62, 0x2e, 0x63, 0x6f, 0x6d, 0x2f,
	0x65, 0x6e, 0x76, 0x6f, 0x79, 0x70, 0x72, 0x6f, 0x78, 0x79, 0x2f, 0x67, 0x6f, 0x2d, 0x63, 0x6f,
	0x6e, 0x74, 0x72, 0x6f, 0x6c, 0x2d, 0x70, 0x6c, 0x61, 0x6e, 0x65, 0x2f, 0x77, 0x73, 0x6f, 0x32,
	0x2f, 0x64, 0x69, 0x73, 0x63, 0x6f, 0x76, 0x65, 0x72, 0x79, 0x2f, 0x73, 0x65, 0x72, 0x76, 0x69,
	0x63, 0x65, 0x2f, 0x67, 0x61, 0x88, 0x01, 0x01, 0x62, 0x06, 0x70, 0x72, 0x6f, 0x74, 0x6f, 0x33,
}

var file_wso2_discovery_service_ga_apids_proto_goTypes = []interface{}{
	(*v3.DiscoveryRequest)(nil),  // 0: envoy.service.discovery.v3.DiscoveryRequest
	(*v3.DiscoveryResponse)(nil), // 1: envoy.service.discovery.v3.DiscoveryResponse
}
var file_wso2_discovery_service_ga_apids_proto_depIdxs = []int32{
	0, // 0: discovery.service.ga.ApiGADiscoveryService.StreamGAApis:input_type -> envoy.service.discovery.v3.DiscoveryRequest
	0, // 1: discovery.service.ga.ApiGADiscoveryService.FetchGAApis:input_type -> envoy.service.discovery.v3.DiscoveryRequest
	1, // 2: discovery.service.ga.ApiGADiscoveryService.StreamGAApis:output_type -> envoy.service.discovery.v3.DiscoveryResponse
	1, // 3: discovery.service.ga.ApiGADiscoveryService.FetchGAApis:output_type -> envoy.service.discovery.v3.DiscoveryResponse
	2, // [2:4] is the sub-list for method output_type
	0, // [0:2] is the sub-list for method input_type
	0, // [0:0] is the sub-list for extension type_name
	0, // [0:0] is the sub-list for extension extendee
	0, // [0:0] is the sub-list for field type_name
}

func init() { file_wso2_discovery_service_ga_apids_proto_init() }
func file_wso2_discovery_service_ga_apids_proto_init() {
	if File_wso2_discovery_service_ga_apids_proto != nil {
		return
	}
	type x struct{}
	out := protoimpl.TypeBuilder{
		File: protoimpl.DescBuilder{
			GoPackagePath: reflect.TypeOf(x{}).PkgPath(),
			RawDescriptor: file_wso2_discovery_service_ga_apids_proto_rawDesc,
			NumEnums:      0,
			NumMessages:   0,
			NumExtensions: 0,
			NumServices:   1,
		},
		GoTypes:           file_wso2_discovery_service_ga_apids_proto_goTypes,
		DependencyIndexes: file_wso2_discovery_service_ga_apids_proto_depIdxs,
	}.Build()
	File_wso2_discovery_service_ga_apids_proto = out.File
	file_wso2_discovery_service_ga_apids_proto_rawDesc = nil
	file_wso2_discovery_service_ga_apids_proto_goTypes = nil
	file_wso2_discovery_service_ga_apids_proto_depIdxs = nil
}

// Reference imports to suppress errors if they are not otherwise used.
var _ context.Context
var _ grpc.ClientConnInterface

// This is a compile-time assertion to ensure that this generated file
// is compatible with the grpc package it is being compiled against.
const _ = grpc.SupportPackageIsVersion6

// ApiGADiscoveryServiceClient is the client API for ApiGADiscoveryService service.
//
// For semantics around ctx use and closing/ending streaming RPCs, please refer to https://godoc.org/google.golang.org/grpc#ClientConn.NewStream.
type ApiGADiscoveryServiceClient interface {
	StreamGAApis(ctx context.Context, opts ...grpc.CallOption) (ApiGADiscoveryService_StreamGAApisClient, error)
	FetchGAApis(ctx context.Context, in *v3.DiscoveryRequest, opts ...grpc.CallOption) (*v3.DiscoveryResponse, error)
}

type apiGADiscoveryServiceClient struct {
	cc grpc.ClientConnInterface
}

func NewApiGADiscoveryServiceClient(cc grpc.ClientConnInterface) ApiGADiscoveryServiceClient {
	return &apiGADiscoveryServiceClient{cc}
}

func (c *apiGADiscoveryServiceClient) StreamGAApis(ctx context.Context, opts ...grpc.CallOption) (ApiGADiscoveryService_StreamGAApisClient, error) {
	stream, err := c.cc.NewStream(ctx, &_ApiGADiscoveryService_serviceDesc.Streams[0], "/discovery.service.ga.ApiGADiscoveryService/StreamGAApis", opts...)
	if err != nil {
		return nil, err
	}
	x := &apiGADiscoveryServiceStreamGAApisClient{stream}
	return x, nil
}

type ApiGADiscoveryService_StreamGAApisClient interface {
	Send(*v3.DiscoveryRequest) error
	Recv() (*v3.DiscoveryResponse, error)
	grpc.ClientStream
}

type apiGADiscoveryServiceStreamGAApisClient struct {
	grpc.ClientStream
}

func (x *apiGADiscoveryServiceStreamGAApisClient) Send(m *v3.DiscoveryRequest) error {
	return x.ClientStream.SendMsg(m)
}

func (x *apiGADiscoveryServiceStreamGAApisClient) Recv() (*v3.DiscoveryResponse, error) {
	m := new(v3.DiscoveryResponse)
	if err := x.ClientStream.RecvMsg(m); err != nil {
		return nil, err
	}
	return m, nil
}

func (c *apiGADiscoveryServiceClient) FetchGAApis(ctx context.Context, in *v3.DiscoveryRequest, opts ...grpc.CallOption) (*v3.DiscoveryResponse, error) {
	out := new(v3.DiscoveryResponse)
	err := c.cc.Invoke(ctx, "/discovery.service.ga.ApiGADiscoveryService/FetchGAApis", in, out, opts...)
	if err != nil {
		return nil, err
	}
	return out, nil
}

// ApiGADiscoveryServiceServer is the server API for ApiGADiscoveryService service.
type ApiGADiscoveryServiceServer interface {
	StreamGAApis(ApiGADiscoveryService_StreamGAApisServer) error
	FetchGAApis(context.Context, *v3.DiscoveryRequest) (*v3.DiscoveryResponse, error)
}

// UnimplementedApiGADiscoveryServiceServer can be embedded to have forward compatible implementations.
type UnimplementedApiGADiscoveryServiceServer struct {
}

func (*UnimplementedApiGADiscoveryServiceServer) StreamGAApis(ApiGADiscoveryService_StreamGAApisServer) error {
	return status.Errorf(codes.Unimplemented, "method StreamGAApis not implemented")
}
func (*UnimplementedApiGADiscoveryServiceServer) FetchGAApis(context.Context, *v3.DiscoveryRequest) (*v3.DiscoveryResponse, error) {
	return nil, status.Errorf(codes.Unimplemented, "method FetchGAApis not implemented")
}

func RegisterApiGADiscoveryServiceServer(s *grpc.Server, srv ApiGADiscoveryServiceServer) {
	s.RegisterService(&_ApiGADiscoveryService_serviceDesc, srv)
}

func _ApiGADiscoveryService_StreamGAApis_Handler(srv interface{}, stream grpc.ServerStream) error {
	return srv.(ApiGADiscoveryServiceServer).StreamGAApis(&apiGADiscoveryServiceStreamGAApisServer{stream})
}

type ApiGADiscoveryService_StreamGAApisServer interface {
	Send(*v3.DiscoveryResponse) error
	Recv() (*v3.DiscoveryRequest, error)
	grpc.ServerStream
}

type apiGADiscoveryServiceStreamGAApisServer struct {
	grpc.ServerStream
}

func (x *apiGADiscoveryServiceStreamGAApisServer) Send(m *v3.DiscoveryResponse) error {
	return x.ServerStream.SendMsg(m)
}

func (x *apiGADiscoveryServiceStreamGAApisServer) Recv() (*v3.DiscoveryRequest, error) {
	m := new(v3.DiscoveryRequest)
	if err := x.ServerStream.RecvMsg(m); err != nil {
		return nil, err
	}
	return m, nil
}

func _ApiGADiscoveryService_FetchGAApis_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(v3.DiscoveryRequest)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(ApiGADiscoveryServiceServer).FetchGAApis(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: "/discovery.service.ga.ApiGADiscoveryService/FetchGAApis",
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(ApiGADiscoveryServiceServer).FetchGAApis(ctx, req.(*v3.DiscoveryRequest))
	}
	return interceptor(ctx, in, info, handler)
}

var _ApiGADiscoveryService_serviceDesc = grpc.ServiceDesc{
	ServiceName: "discovery.service.ga.ApiGADiscoveryService",
	HandlerType: (*ApiGADiscoveryServiceServer)(nil),
	Methods: []grpc.MethodDesc{
		{
			MethodName: "FetchGAApis",
			Handler:    _ApiGADiscoveryService_FetchGAApis_Handler,
		},
	},
	Streams: []grpc.StreamDesc{
		{
			StreamName:    "StreamGAApis",
			Handler:       _ApiGADiscoveryService_StreamGAApis_Handler,
			ServerStreams: true,
			ClientStreams: true,
		},
	},
	Metadata: "wso2/discovery/service/ga/apids.proto",
}
