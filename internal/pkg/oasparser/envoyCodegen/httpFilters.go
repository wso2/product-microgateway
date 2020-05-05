package envoyCodegen

import (
	core "github.com/envoyproxy/go-control-plane/envoy/api/v2/core"
	envoyconfigfilterhttpextauthzv2 "github.com/envoyproxy/go-control-plane/envoy/config/filter/http/ext_authz/v2"
	hcm "github.com/envoyproxy/go-control-plane/envoy/config/filter/network/http_connection_manager/v2"
	"github.com/envoyproxy/go-control-plane/pkg/wellknown"
	"github.com/golang/protobuf/ptypes"
)

func GetHttpFilters() []*hcm.HttpFilter {
	//extAauth := GetExtAauthzHttpFilter()
	router := GetRouterHttpFilter()

	httpFilters := []*hcm.HttpFilter{
		//&extAauth,
		&router,
	}

	return httpFilters
}

func GetRouterHttpFilter() hcm.HttpFilter {
	return hcm.HttpFilter{Name: wellknown.Router}
}

func GetExtAauthzHttpFilter() hcm.HttpFilter {
	extAuthzConfig := &envoyconfigfilterhttpextauthzv2.ExtAuthz{
		WithRequestBody: &envoyconfigfilterhttpextauthzv2.BufferSettings{
			MaxRequestBytes:     1024,
			AllowPartialMessage: false,
		},
		Services: &envoyconfigfilterhttpextauthzv2.ExtAuthz_GrpcService{
			GrpcService: &core.GrpcService{
				TargetSpecifier: &core.GrpcService_EnvoyGrpc_{
					EnvoyGrpc: &core.GrpcService_EnvoyGrpc{
						ClusterName: "ext-authz",
					},
				},
			},
		},
	}

	ext, err2 := ptypes.MarshalAny(extAuthzConfig)
	if err2 != nil {
		panic(err2)
	}
	//fmt.Println(ext)

	extAuthzFilter := hcm.HttpFilter{
		Name: "envoy.filters.http.ext_authz",
		ConfigType: &hcm.HttpFilter_TypedConfig{
			TypedConfig: ext,
		},
	}

	return extAuthzFilter
}
