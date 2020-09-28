package filters

import (
	"context"
	"crypto/rsa"
	"errors"
	"github.com/dgrijalva/jwt-go"
	ext_authz "github.com/envoyproxy/go-control-plane/envoy/service/auth/v2"
	envoy_type "github.com/envoyproxy/go-control-plane/envoy/type"
	"github.com/gogo/googleapis/google/rpc"
	"github.com/patrickmn/go-cache"
	log "github.com/sirupsen/logrus"
	"google.golang.org/genproto/googleapis/rpc/status"
	"io/ioutil"
	"os"
	"strconv"
	"strings"
	"sync"
	"time"
)



type TokenData struct {
	meta_clientType        string
	applicationConsumerKey string
	applicationName        string
	applicationId          string
	applicationOwner       string
	apiCreator             string
	apiCreatorTenantDomain string
	apiTier                string
	username               string
	userTenantDomain       string
	throttledOut           bool
	serviceTime            int64
	authorized             bool
}

var (
	Unknown = "__unknown__"
	once sync.Once
	once_1 sync.Once
	caCert []byte
	err error
	key *rsa.PublicKey
	jwtCache = cache.New(5*time.Minute, 10*time.Minute)
	UnauthorizedError = errors.New("Invalid access token")
	jwtToken string
	isCacheEnabled bool
)



// handle JWT token
func HandleJWT(validateSubscription bool, publicCert []byte, token string) (bool, TokenData, error) {

	accessToken := token

	tokenContent := strings.Split(accessToken, ".")
	var tokenData TokenData

	if len(tokenContent) != 3 {
		log.Errorf("Invalid JWT token received, token must have 3 parts")
		return false, tokenData, UnauthorizedError
	}

	signedContent := tokenContent[0] + "." + tokenContent[1]
	err := validateSignature(publicCert, signedContent, tokenContent[2])
	if err != nil {
		log.Errorf("Error in validating the signature: %v", err)
		return false, tokenData, UnauthorizedError
	}

	return true, tokenData, nil
}

// validate the signature
func validateSignature(publicCert []byte, signedContent string, signature string) error {

	once_1.Do(func() {
		key, err = jwt.ParseRSAPublicKeyFromPEM(publicCert)
		log.Info("read public key once")
	})

	if err != nil {
		log.Errorf("Error in parsing the public key: %v", err)
		return err
	}

	return jwt.SigningMethodRS256.Verify(signedContent, signature, key)
}

//reading the secret
func ReadFile(fileName string) ([]byte, error) {

	secretValue, err := ioutil.ReadFile(fileName)
	if err != nil {
		log.Warnf("Error in reading the file %v: error - %v", fileName, err)
	}

	return secretValue, err
}


func ValidateToken(ctx context.Context, req *ext_authz.CheckRequest) (*ext_authz.CheckResponse, error) {

	once.Do(func() {
		caCert,_ = ReadFile("./artifacts/server.pem")
		log.Info("read server.pem file once")

		/* env variable can define as following
		ENVOY_GW_CACHE_ENABLE = true
		ENVOY_GW_CACHE_ENABLE = false
		*/
		cacheEnvVar := os.Getenv("ENVOY_GW_CACHE_ENABLE")
		log.Info("env variable",cacheEnvVar )

		if cacheEnvVar != "" {
			isCacheEnabled, err = strconv.ParseBool(cacheEnvVar)


			if err != nil {
				log.Error("Error reading cache env variable, err")
				isCacheEnabled = false
			}

		} else {
			isCacheEnabled = false
		}
	})

	auth := false
	jwtToken := ""
	requestAttributes := req.Attributes.Request.Http.Headers

	for k := range requestAttributes {
		if k == "authorization" {
			jwtToken =  requestAttributes["authorization"]
		}
	}


	if isCacheEnabled {
		log.Info("cache is enabled")
		tokenvalidity, found := jwtCache.Get(jwtToken)

		if found {
			auth = tokenvalidity.(bool)
			log.Info("token found in cache")

		} else {
			auth, _, _ = HandleJWT(false, caCert,jwtToken )
			jwtCache.Set(jwtToken , auth, cache.DefaultExpiration)
		}
	} else {
		auth, _, _ = HandleJWT(false, caCert,jwtToken )
	}

	resp := &ext_authz.CheckResponse{}
	if auth {
		resp = &ext_authz.CheckResponse{
			Status: &status.Status{Code: int32(rpc.OK)},
			HttpResponse: &ext_authz.CheckResponse_OkResponse{
				OkResponse: &ext_authz.OkHttpResponse{

				},
			},
		}

	} else {
		resp = &ext_authz.CheckResponse{
			Status: &status.Status{Code: int32(rpc.UNAUTHENTICATED)},
			HttpResponse: &ext_authz.CheckResponse_DeniedResponse{
				DeniedResponse: &ext_authz.DeniedHttpResponse{
					Status:  &envoy_type.HttpStatus{
						Code: envoy_type.StatusCode_Unauthorized,
					},
					Body: "Error occurred while authenticating.",

				},
			},
		}
	}

	return resp, nil
}
