package controller

import (
	"context"
	"strings"

	"github.com/wso2/product-microgateway/filters"

	ext_authz "github.com/envoyproxy/go-control-plane/envoy/service/auth/v2"
	"github.com/gogo/googleapis/google/rpc"
	log "github.com/sirupsen/logrus"
)

// ExecuteFilters method execute the filter chain
// authoriztion, throttling and stat
// Input parameters
// - context of the request
// - ext_authz.CheckRequest coming from downstream (envoy)
// Output parameters
// - ext_authz.Check response with the status of the filter chain execution results
// - error if an error occurred during the execution of fitlers
func ExecuteFilters(ctx context.Context, req *ext_authz.CheckRequest) (*ext_authz.CheckResponse, error) {

	apis, err := readApis()
	basemap, err := ResolveBasepaths(apis)
	if err != nil {
		log.Error("Error occurred", err)
	}
	//Path of the request
	path := req.Attributes.Request.Http.Path
	var apiname string
	var basepath string
	// Identifying the API which the basepath maps to
	for k, v := range basemap {
		if strings.Contains(path, v) {
			apiname = k
			basepath = v
		}
	}
	// Set the API name, version and swagger to the context
	ctx = context.WithValue(ctx, "apiname", apiname)
	ctx = context.WithValue(ctx, "context", basepath)

	api, err := GetAPI(apiname, apis)
	ctx = context.WithValue(ctx, "API", api)

	//Validate the token by calling token filter
	resp, err := filters.ValidateToken(ctx, req)

	//Return if the authentication failed
	if resp.Status.Code != int32(rpc.OK) {
		return resp, nil
	}

	//Continue to next filter
	// Publish metrics
	resp, err = filters.PublishMetrics(ctx, req)
	//todo: handle failure
	return resp, err

}
