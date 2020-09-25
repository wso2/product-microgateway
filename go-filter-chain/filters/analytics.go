package filters

import (
	"context"
	"github.com/cactus/go-statsd-client/statsd"
	ext_authz "github.com/envoyproxy/go-control-plane/envoy/service/auth/v2"
	"github.com/gogo/googleapis/google/rpc"
	"google.golang.org/genproto/googleapis/rpc/status"
	"log"
)
func PublishMetrics(ctx context.Context, req *ext_authz.CheckRequest) (*ext_authz.CheckResponse, error){


	config := &statsd.ClientConfig{
		Address: "127.0.0.1:8125",
		Prefix: "test-client",
	}

	client, err := statsd.NewClientWithConfig(config)

	// and handle any initialization errors
	if err != nil {
		log.Fatal(err)
	}

	// make sure to clean up
	defer client.Close()

	// Send a stat
	err = client.Inc("stat1", 42, 1.0)
	// handle any errors
	if err != nil {
		log.Printf("Error sending metric: %+v", err)
	}

	resp := &ext_authz.CheckResponse{}
	resp = &ext_authz.CheckResponse{
		Status: &status.Status{Code: int32(rpc.OK)},
		HttpResponse: &ext_authz.CheckResponse_OkResponse{
			OkResponse: &ext_authz.OkHttpResponse{

			},
		},
	}
	return resp, nil
}
