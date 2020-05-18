package filters

// JWT token will be validated before proceeding to the gateway

import (
	"context"
	"encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	"github.com/dgrijalva/jwt-go"
	envoy_type "github.com/envoyproxy/go-control-plane/envoy/type"
	"github.com/gogo/googleapis/google/rpc"
	log "github.com/sirupsen/logrus"
	"io/ioutil"
	"strconv"
	"strings"
	"time"
	ext_authz "github.com/envoyproxy/go-control-plane/envoy/service/auth/v2"
	"google.golang.org/genproto/googleapis/rpc/status"

)

//JWT represents the JWT token found in the decrypted "Authorization" header.

type JWT struct {
	Aud         string `json:"aud"`
	Sub         string `json:"sub"`
	Application struct {
		Owner string      `json:"owner"`
		Tier  string      `json:"tier"`
		Name  string      `json:"name"`
		ID    int         `json:"id"`
		UUID  interface{} `json:"uuid"`
	} `json:"application"`
	Scope    string `json:"scope"`
	Iss      string `json:"iss"`
	TierInfo struct {
	} `json:"tierInfo"`
	Keytype        string        `json:"keytype"`
	SubscribedAPIs []struct {
		Name                   string `json:"name"`
		Context                string `json:"context"`
		Version                string `json:"version"`
		Publisher              string `json:"publisher"`
		SubscriptionTier       string `json:"subscriptionTier"`
		SubscriberTenantDomain string `json:"subscriberTenantDomain"`
	} `json:"subscribedAPIs"`
	ConsumerKey    string        `json:"consumerKey"`
	Exp            int64         `json:"exp"`
	Iat            int           `json:"iat"`
	Jti            string        `json:"jti"`
}

const (
	StdPadding rune = '=' // Standard padding character
	NoPadding  rune = -1  // No padding
)


type Subscription struct {
	name                   string
	context                string
	version                string
	publisher              string
	subscriptionTier       string
	subscriberTenantDomain string
}

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

var Unknown = "__unknown__"


var UnauthorizedError = errors.New("Invalid access token")


// handle JWT token
func HandleJWT(validateSubscription bool, publicCert []byte, requestAttributes map[string]string) (bool, TokenData, error) {

	accessToken := requestAttributes["authorization"]
	//apiName := requestAttributes["api-name"]
	//apiVersion := requestAttributes["api-version"]
	//requestScope := requestAttributes["request-scope"]

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

	jwtData, err := decodePayload(tokenContent[1])
	if jwtData == nil {
		log.Errorf("Error in decoding the payload: %v", err)
		return false, tokenData, UnauthorizedError
	}

	if isTokenExpired(jwtData) {
		return false, tokenData, UnauthorizedError
	}

	//if !isRequestScopeValid(jwtData, requestScope) {
	//	return false, tokenData, UnauthorizedError
	//}

	if validateSubscription {

		subscription := getSubscription(jwtData, "", "")

		if (Subscription{}) == subscription {
			return false, tokenData, errors.New("Resource forbidden")
		}

		return true, getTokenDataForJWT(jwtData, "", ""), nil
	}

	return true, tokenData, nil
}

// validate the signature
func validateSignature(publicCert []byte, signedContent string, signature string) error {

	key, err := jwt.ParseRSAPublicKeyFromPEM(publicCert)

	if err != nil {
		log.Errorf("Error in parsing the public key: %v", err)
		return err
	}

	return jwt.SigningMethodRS256.Verify(signedContent, signature, key)
}

// decode the payload
func decodePayload(payload string) (*JWT, error) {

	data, err := base64.StdEncoding.WithPadding(NoPadding).DecodeString(payload)

	jwtData := JWT{}
	err = json.Unmarshal(data, &jwtData)
	if err != nil {
		log.Errorf("Error in unmarshalling payload: %v", err)
		return nil, err
	}

	return &jwtData, nil
}

// check whether the token has expired
func isTokenExpired(jwtData *JWT) bool {

	nowTime := time.Now().Unix()
	expireTime := int64(jwtData.Exp)

	if expireTime < nowTime {
		log.Warnf("Token is expired!")
		return true
	}

	return false
}

// do resource scope validation
func isRequestScopeValid(jwtData *JWT, requestScope string) bool {

	if len(requestScope) > 0 {

		tokenScopes := strings.Split(jwtData.Scope, " ")

		for _, tokenScope := range tokenScopes {
			if requestScope == tokenScope {
				return true
			}

		}
		log.Warnf("No matching scopes found!")
		return false
	}

	log.Infof("No scopes defined")
	return true
}

// get the subscription
func getSubscription(jwtData *JWT, apiName string, apiVersion string) Subscription {

	var sub Subscription
	for _, api := range jwtData.SubscribedAPIs {

		if (strings.ToLower(apiName) == strings.ToLower(api.Name)) && apiVersion == api.Version {
			sub.name = apiName
			sub.version = apiVersion
			sub.context = api.Context
			sub.publisher = api.Publisher
			sub.subscriptionTier = api.SubscriptionTier
			sub.subscriberTenantDomain = api.SubscriberTenantDomain
			return sub
		}
	}

	log.Warnf("Subscription is not valid for API - %v %v", apiName, apiVersion)
	return sub
}

// get token data for JWT
func getTokenDataForJWT(jwtData *JWT, apiName string, apiVersion string) TokenData {

	var token TokenData

	token.authorized = true
	token.meta_clientType = jwtData.Keytype
	token.applicationConsumerKey = jwtData.ConsumerKey
	token.applicationName = jwtData.Application.Name
	token.applicationId = strconv.Itoa(jwtData.Application.ID)
	token.applicationOwner = jwtData.Application.Owner

	subscription := getSubscription(jwtData, apiName, apiVersion)

	if &subscription == nil {
		token.apiCreator = Unknown
		token.apiCreatorTenantDomain = Unknown
		token.apiTier = Unknown
		token.userTenantDomain = Unknown
	} else {
		token.apiCreator = subscription.publisher
		token.apiCreatorTenantDomain = subscription.subscriberTenantDomain
		token.apiTier = subscription.subscriptionTier
		token.userTenantDomain = subscription.subscriberTenantDomain
	}

	token.username = jwtData.Sub
	token.throttledOut = false

	return token
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

	caCert,_ := ReadFile("./filter-chain/artifacts/security/server.pem")

	auth := false
	for k := range req.Attributes.Request.Http.Headers {
		if k == "authorization" {
			//h = true
			//header := req.Attributes.Request.Http.Headers["authorization"]
			auth, _, _ = HandleJWT(false, caCert,req.Attributes.Request.Http.Headers )
			fmt.Println("JWT header detected" + k)
			break;
		}
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