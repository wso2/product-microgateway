module github.com/wso2/product-microgateway/adapter

go 1.18

// TODO: (renuka) Remove this replace once, https://github.com/envoyproxy/go-control-plane/pull/598 is merged
replace github.com/envoyproxy/go-control-plane => github.com/renuka-fernando/go-control-plane v0.10.4-0.20221206120017-43c2f5b75212

require (
	github.com/Azure/azure-sdk-for-go/sdk/messaging/azservicebus v1.1.4
	github.com/envoyproxy/go-control-plane v0.10.0
	github.com/fsnotify/fsnotify v1.4.9
	github.com/getkin/kin-openapi v0.8.0
	github.com/ghodss/yaml v1.0.0
	github.com/go-openapi/errors v0.19.8
	github.com/go-openapi/loads v0.19.5
	github.com/go-openapi/runtime v0.19.22
	github.com/go-openapi/spec v0.19.8
	github.com/go-openapi/strfmt v0.21.1
	github.com/go-openapi/swag v0.19.9
	github.com/go-openapi/validate v0.19.11
	github.com/golang/protobuf v1.5.2
	github.com/google/uuid v1.1.2
	github.com/grpc-ecosystem/go-grpc-middleware v1.3.0
	github.com/jessevdk/go-flags v1.4.0
	github.com/lestrrat-go/jwx v1.1.3
	github.com/mitchellh/mapstructure v1.3.3
	github.com/pelletier/go-toml v1.8.1
	github.com/sirupsen/logrus v1.7.0
	github.com/streadway/amqp v1.0.0
	github.com/stretchr/testify v1.7.1
	golang.org/x/net v0.4.0
	google.golang.org/genproto v0.0.0-20220329172620-7be39ac1afc7
	google.golang.org/grpc v1.45.0
	google.golang.org/protobuf v1.28.0
	gopkg.in/natefinch/lumberjack.v2 v2.0.0
	gopkg.in/yaml.v2 v2.4.0
)

require (
	github.com/Azure/azure-sdk-for-go/sdk/azcore v1.0.0 // indirect
	github.com/Azure/azure-sdk-for-go/sdk/internal v1.1.2 // indirect
	github.com/PuerkitoBio/purell v1.1.1 // indirect
	github.com/PuerkitoBio/urlesc v0.0.0-20170810143723-de5bf2ad4578 // indirect
	github.com/asaskevich/govalidator v0.0.0-20200907205600-7a23bdc65eef // indirect
	github.com/census-instrumentation/opencensus-proto v0.3.0 // indirect
	github.com/cncf/xds/go v0.0.0-20220314180256-7f1daf1720fc // indirect
	github.com/davecgh/go-spew v1.1.1 // indirect
	github.com/decred/dcrd/dcrec/secp256k1/v3 v3.0.0 // indirect
	github.com/docker/go-units v0.4.0 // indirect
	github.com/envoyproxy/protoc-gen-validate v0.6.7 // indirect
	github.com/go-openapi/analysis v0.19.10 // indirect
	github.com/go-openapi/jsonpointer v0.19.3 // indirect
	github.com/go-openapi/jsonreference v0.19.3 // indirect
	github.com/go-stack/stack v1.8.0 // indirect
	github.com/goccy/go-json v0.4.7 // indirect
	github.com/lestrrat-go/backoff/v2 v2.0.7 // indirect
	github.com/lestrrat-go/httpcc v1.0.0 // indirect
	github.com/lestrrat-go/iter v1.0.0 // indirect
	github.com/lestrrat-go/option v1.0.0 // indirect
	github.com/mailru/easyjson v0.7.1 // indirect
	github.com/oklog/ulid v1.3.1 // indirect
	github.com/pkg/errors v0.9.1 // indirect
	github.com/pmezard/go-difflib v1.0.0 // indirect
	go.mongodb.org/mongo-driver v1.7.5 // indirect
	golang.org/x/crypto v0.0.0-20220511200225-c6db032c6c88 // indirect
	golang.org/x/sys v0.3.0 // indirect
	golang.org/x/text v0.5.0 // indirect
	gopkg.in/yaml.v3 v3.0.0-20210107192922-496545a6307b // indirect
)
