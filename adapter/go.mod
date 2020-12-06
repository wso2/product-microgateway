module github.com/wso2/micro-gw

go 1.15

replace github.com/envoyproxy/go-control-plane => ../../../envoy/go-control-plane

require (
	github.com/BurntSushi/toml v0.3.1
	github.com/envoyproxy/go-control-plane v0.9.4
	github.com/fsnotify/fsnotify v1.4.9
	github.com/getkin/kin-openapi v0.8.0
	github.com/ghodss/yaml v1.0.0
	github.com/go-openapi/errors v0.19.7
	github.com/go-openapi/loads v0.19.5
	github.com/go-openapi/runtime v0.19.22
	github.com/go-openapi/spec v0.19.8
	github.com/go-openapi/strfmt v0.19.6
	github.com/go-openapi/swag v0.19.9
	github.com/go-openapi/validate v0.19.11
	github.com/golang/protobuf v1.4.3
	github.com/google/uuid v1.1.2
	github.com/gorilla/mux v1.7.4
	github.com/jessevdk/go-flags v1.4.0
	github.com/pavel-v-chernykh/keystore-go/v3 v3.0.4
	github.com/sirupsen/logrus v1.7.0
	github.com/stretchr/testify v1.6.1
	golang.org/x/lint v0.0.0-20200302205851-738671d3881b // indirect
	golang.org/x/net v0.0.0-20201201195509-5d6afe98e0b7
	golang.org/x/text v0.3.4 // indirect
	golang.org/x/tools v0.0.0-20201116002733-ac45abd4c88c // indirect
	google.golang.org/appengine v1.6.1
	google.golang.org/genproto v0.0.0-20201201144952-b05cb90ed32e
	google.golang.org/grpc v1.33.2
	google.golang.org/protobuf v1.25.0
	gopkg.in/natefinch/lumberjack.v2 v2.0.0
)
