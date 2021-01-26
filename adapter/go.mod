module github.com/wso2/micro-gw

go 1.15

replace github.com/envoyproxy/go-control-plane => github.com/praminda/go-control-plane v0.9.8-0.20210119081951-da1f9251f109

require (
	github.com/envoyproxy/go-control-plane v0.9.8
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
	github.com/jessevdk/go-flags v1.4.0
	github.com/pavel-v-chernykh/keystore-go/v3 v3.0.4
	github.com/pelletier/go-toml v1.8.1
	github.com/sirupsen/logrus v1.7.0
	github.com/stretchr/testify v1.6.1
	golang.org/x/lint v0.0.0-20201208152925-83fdc39ff7b5 // indirect
	golang.org/x/net v0.0.0-20201201195509-5d6afe98e0b7
	golang.org/x/tools v0.0.0-20210107193943-4ed967dd8eff // indirect
	google.golang.org/appengine v1.6.1 // indirect
	google.golang.org/genproto v0.0.0-20201201144952-b05cb90ed32e // indirect
	google.golang.org/grpc v1.33.2
	google.golang.org/protobuf v1.25.0
	gopkg.in/natefinch/lumberjack.v2 v2.0.0
)
