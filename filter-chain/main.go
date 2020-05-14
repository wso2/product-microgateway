package main

import (
	"context"
	"fmt"
	"log"
	"net"
	"os"
	"os/signal"

	ext_authz "github.com/envoyproxy/go-control-plane/envoy/service/auth/v2"
	"github.com/golang/protobuf/jsonpb"
	"github.com/wso2/product-microgateway/controller"
	"google.golang.org/grpc"
	"google.golang.org/grpc/reflection"
)

type server struct {
	mode string
}

func main() {
	c := make(chan os.Signal)
	signal.Notify(c, os.Interrupt)

	go listen(":8081", &server{mode: "GATEWAY"})

	<-c
}

func listen(address string, serverType *server) {
	lis, err := net.Listen("tcp", address)
	if err != nil {
		log.Fatalf("failed to listen: %v", err)
	}
	s := grpc.NewServer()
	ext_authz.RegisterAuthorizationServer(s, serverType)
	reflection.Register(s)
	fmt.Printf("Starting %q reciver on %q\n", serverType.mode, address)
	if err := s.Serve(lis); err != nil {
		log.Fatalf("failed to serve: %v", err)
	}
}

func (s *server) Check(ctx context.Context, req *ext_authz.CheckRequest) (*ext_authz.CheckResponse, error) {

	fmt.Printf("======================================== %-24s ========================================\n", fmt.Sprintf("%s Start", s.mode))
	defer fmt.Printf("======================================== %-24s ========================================\n\n", fmt.Sprintf("%s End", s.mode))

	m := jsonpb.Marshaler{Indent: "  "}
	js, err := m.MarshalToString(req)

	if err != nil {
		fmt.Println(err)
	} else {
		fmt.Println(js)
	}
	return controller.ExecuteFilters(ctx, req)

}
