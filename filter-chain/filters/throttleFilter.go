package filters

import (
	"context"
	"fmt"
	_ "github.com/cactus/go-statsd-client/statsd"
	ext_authz "github.com/envoyproxy/go-control-plane/envoy/service/auth/v2"
	envoy_type "github.com/envoyproxy/go-control-plane/envoy/type"
	"github.com/gogo/googleapis/google/rpc"
	log "github.com/sirupsen/logrus"
	"github.com/wso2/product-microgateway/constants"
	"github.com/wso2/product-microgateway/dtos"
	"google.golang.org/genproto/googleapis/rpc/status"
	_ "log"
	"reflect"
	"strconv"
)

var enabledGlobalTMEventPublishing bool = false

func ThrottleFilter(ctx context.Context, req *ext_authz.CheckRequest) (*ext_authz.CheckResponse, error) {
	ctx = updateContext(ctx)
	deployedPolicies := getDeployedPolicies()
	resp := doThrottleFilterRequest(ctx, deployedPolicies)
	fmt.Println(resp)
	return resp, nil
}

type policyDetails struct {
	count       int64
	unitTime    int64
	timeUnit    string
	stopOnQuota bool
}

func getDeployedPolicies() map[string]policyDetails {
	deployedPolicies := map[string]policyDetails{
		"app_50PerMin": {
			count:       5,
			unitTime:    5,
			timeUnit:    "min",
			stopOnQuota: true,
		},
		"app_20PerMin": {
			count:       5,
			unitTime:    5,
			timeUnit:    "min",
			stopOnQuota: true,
		},
		"res_10PerMin": {
			count:       5,
			unitTime:    5,
			timeUnit:    "min",
			stopOnQuota: true,
		},
		"sub_10MinSubPolicy": {
			count:       5,
			unitTime:    5,
			timeUnit:    "min",
			stopOnQuota: true,
		},
		"app_10MinAppPolicy": {
			count:       5,
			unitTime:    5,
			timeUnit:    "min",
			stopOnQuota: true,
		},
	}
	return deployedPolicies
}

type Values struct {
	m map[string]string
}

func (v Values) Get(key string) string {
	return v.m[key]
}

func (v Values) Set(key string, value string) {
	v.m[key] = value
}

func updateContext(ctx context.Context) context.Context {
	v := Values{map[string]string{
		"AUTHENTICATION_CONTEXT":  "authenticated",
		"IS_SECURED":              "false",
		"authenticated":           "true",
		"username":                "admin",
		"applicationTier":         "10MinAppPolicy",
		"tier":                    "10MinSubPolicy",
		"apiTier":                 "10PerMin",
		"applicationId":           "899",
		"remoteAddress":           "remoteAddress",
		"apiVersion":              "1.0.0",
		"resourceLevelPolicyName": "10PerMin",
		"basePath":                "/petstore/v1",
		"resourceName":            "get87b5e48c92b648e3bba230381d89cfef",
		"resourceTier":            "3PerMin",
	}}
	ctx = context.WithValue(context.Background(), "ApiDetails", v)
	return ctx
}

func getContextDetails(ctx context.Context, key string) string {
	value := ctx.Value("ApiDetails").(Values).Get(key)
	return value
}

func doThrottleFilterRequest(ctx context.Context, deployedPolicies map[string]policyDetails) *ext_authz.CheckResponse {
	log.Println(constants.KEY_THROTTLE_FILTER + " Processing the request in ThrottleFilter")
	//Throttled decisions
	var isThrottled bool = false
	var stopOnQuota bool
	isSecured, _ := strconv.ParseBool(getContextDetails(ctx, constants.IS_SECURED))
	var apiVersion string = getContextDetails(ctx, constants.API_VERSION)
	var resourceLevelPolicyName string = getContextDetails(ctx, constants.RESOURCE_LEVEL_POLICY_NAME)
	var apiLevelPolicy string = getContextDetails(ctx, constants.API_TIER)
	resp := &ext_authz.CheckResponse{}
	if getContextDetails(ctx, constants.AUTHENTICATION_CONTEXT) != "" {
		log.Println(constants.KEY_THROTTLE_FILTER + " Context contains Authentication Context")
		if apiLevelThrottled, resp := checkAPILevelThrottled(ctx, apiLevelPolicy, apiVersion, deployedPolicies);
			!apiLevelThrottled {
			return resp
		}
		if resourceLevelThrottled, resp := checkResourceLevelThrottled(ctx, resourceLevelPolicyName, apiVersion,
			deployedPolicies); !resourceLevelThrottled {
			return resp
		}
		log.Println(constants.KEY_THROTTLE_FILTER + " Checking subscription level throttle policy " +
			getContextDetails(ctx, constants.TIER) + " exist.")
		tier := getContextDetails(ctx, constants.TIER)
		if tier != constants.UNLIMITED_TIER && !isPolicyExist(deployedPolicies,
			tier, constants.SUB_LEVEL_PREFIX) {
			log.Println(constants.KEY_THROTTLE_FILTER + " Subscription level throttle policy " +
				tier + " does not exist.")
			resp = &ext_authz.CheckResponse{
				Status: &status.Status{Code: int32(rpc.INTERNAL)},
				HttpResponse: &ext_authz.CheckResponse_DeniedResponse{
					DeniedResponse: &ext_authz.DeniedHttpResponse{
						Status: &envoy_type.HttpStatus{
							Code: envoy_type.StatusCode_InternalServerError,
						},
						Body: "Internal server error occured.",
					},
				},
			}
			return resp
		}
		log.Println(constants.KEY_THROTTLE_FILTER + "Checking subscription level throttling-out.")
		isThrottled, stopOnQuota = isSubscriptionLevelThrottled(ctx, deployedPolicies, apiVersion)
		log.Println(constants.KEY_THROTTLE_FILTER + "Subscription level throttling result:: isThrottled: " +
			strconv.FormatBool(isThrottled) + " , stopOnQuota: " + strconv.FormatBool(stopOnQuota))
		if isThrottled {
			if stopOnQuota {
				log.Debugf(constants.KEY_THROTTLE_FILTER + "Sending throttled out responses.")
				resp = &ext_authz.CheckResponse{
					Status: &status.Status{Code: int32(rpc.RESOURCE_EXHAUSTED)},
					HttpResponse: &ext_authz.CheckResponse_DeniedResponse{
						DeniedResponse: &ext_authz.DeniedHttpResponse{
							Status: &envoy_type.HttpStatus{
								Code: envoy_type.StatusCode_TooManyRequests,
							},
							Body: "Subscription level throttled out",
						},
					},
				}
				return resp
			} else {
				// set properties in order to publish into analytics for billing
				log.Debugf(constants.KEY_THROTTLE_FILTER + " Proceeding(1st) since stopOnQuota is set to false.")
			}
		}
		applicationTier := getContextDetails(ctx, constants.APPLICATION_TIER)
		log.Println(constants.KEY_THROTTLE_FILTER + " Checking application level throttle policy " +
			applicationTier + " exist.")
		if applicationTier != constants.UNLIMITED_TIER &&
			!isPolicyExist(deployedPolicies, applicationTier, constants.APP_LEVEL_PREFIX) {
			log.Println(constants.KEY_THROTTLE_FILTER + " Application level throttle policy " +
				applicationTier + " does not exist.")
			resp = &ext_authz.CheckResponse{
				Status: &status.Status{Code: int32(rpc.INTERNAL)},
				HttpResponse: &ext_authz.CheckResponse_DeniedResponse{
					DeniedResponse: &ext_authz.DeniedHttpResponse{
						Status: &envoy_type.HttpStatus{
							Code: envoy_type.StatusCode_InternalServerError,
						},
						Body: "Internal server error occured.",
					},
				},
			}
			return resp
		}
		log.Println(constants.KEY_THROTTLE_FILTER + " Checking application level throttling-out.")
		if isApplicationLevelThrottled(ctx, deployedPolicies) {
			log.Println(constants.KEY_THROTTLE_FILTER + " Application level throttled out. Sending throttled " +
				"out response.")
			resp = &ext_authz.CheckResponse{
				Status: &status.Status{Code: int32(rpc.RESOURCE_EXHAUSTED)},
				HttpResponse: &ext_authz.CheckResponse_DeniedResponse{
					DeniedResponse: &ext_authz.DeniedHttpResponse{
						Status: &envoy_type.HttpStatus{
							Code: envoy_type.StatusCode_TooManyRequests,
						},
						Body: "Application level throttled out",
					},
				},
			}
			return resp
		} else {
			log.Println(constants.KEY_THROTTLE_FILTER + " Application level throttled out: false")
		}
	} else if !isSecured {
		if apiLevelThrottled, resp := checkAPILevelThrottled(ctx, apiLevelPolicy, apiVersion, deployedPolicies);
			!apiLevelThrottled {
			return resp
		}
		if resourceLevelThrottled, resp := checkResourceLevelThrottled(ctx, resourceLevelPolicyName, apiVersion,
			deployedPolicies); !resourceLevelThrottled {
			return resp
		}
	} else {
		log.Debugf("Unknown error.")
		resp = &ext_authz.CheckResponse{
			Status: &status.Status{Code: int32(rpc.INTERNAL)},
			HttpResponse: &ext_authz.CheckResponse_DeniedResponse{
				DeniedResponse: &ext_authz.DeniedHttpResponse{
					Status: &envoy_type.HttpStatus{
						Code: envoy_type.StatusCode_InternalServerError,
					},
					Body: "Internal server error occured.",
				},
			},
		}
		return resp
	}
	//Publish throttle event to another worker flow to publish to internal policies or traffic manager
	var throttleEvent dtos.RequestStreamDTO = generateThrottleEvent(ctx, deployedPolicies)
	publishEvent(throttleEvent)
	resp = &ext_authz.CheckResponse{
		Status: &status.Status{Code: int32(rpc.OK)},
		HttpResponse: &ext_authz.CheckResponse_OkResponse{
			OkResponse: &ext_authz.OkHttpResponse{

			},
		},
	}
	return resp
}

func checkAPILevelThrottled(ctx context.Context, apiLevelPolicy string, apiVersion string,
	deployedPolicies map[string]policyDetails) (bool, *ext_authz.CheckResponse) {
	resp := &ext_authz.CheckResponse{}
	log.Println("Checking api level throttle policy " + apiLevelPolicy + " exist.")
	if apiLevelPolicy != constants.UNLIMITED_TIER && !isPolicyExist(deployedPolicies, apiLevelPolicy,
		constants.RESOURCE_LEVEL_PREFIX) {
		log.Println(constants.KEY_THROTTLE_FILTER + ", API level throttle policy " + apiLevelPolicy +
			"does not exist.")
		resp = &ext_authz.CheckResponse{
			Status: &status.Status{Code: int32(rpc.INTERNAL)},
			HttpResponse: &ext_authz.CheckResponse_DeniedResponse{
				DeniedResponse: &ext_authz.DeniedHttpResponse{
					Status: &envoy_type.HttpStatus{
						Code: envoy_type.StatusCode_InternalServerError,
					},
					Body: "Internal server error occured.",
				},
			},
		}
		return false, resp
	}
	log.Println(constants.KEY_THROTTLE_FILTER + ", Checking API level throttling-out.")
	if isAPILevelThrottled(ctx, apiVersion) {
		log.Println(constants.KEY_THROTTLE_FILTER + "API level throttled out. Sending throttled out response.")
		resp = &ext_authz.CheckResponse{
			Status: &status.Status{Code: int32(rpc.RESOURCE_EXHAUSTED)},
			HttpResponse: &ext_authz.CheckResponse_DeniedResponse{
				DeniedResponse: &ext_authz.DeniedHttpResponse{
					Status: &envoy_type.HttpStatus{
						Code: envoy_type.StatusCode_TooManyRequests,
					},
					Body: "API level throttled out",
				},
			},
		}
		return false, resp
	} else {
		log.Println(constants.KEY_THROTTLE_FILTER + "API level throttled out: false")
	}
	resp = &ext_authz.CheckResponse{
		Status: &status.Status{Code: int32(rpc.OK)},
		HttpResponse: &ext_authz.CheckResponse_OkResponse{
			OkResponse: &ext_authz.OkHttpResponse{

			},
		},
	}
	return true, resp
}

func checkResourceLevelThrottled(ctx context.Context, resourceLevelPolicyName string, apiVersion string,
	deployedPolicies map[string]policyDetails) (bool, *ext_authz.CheckResponse) {
	resp := &ext_authz.CheckResponse{}
	if (reflect.TypeOf(resourceLevelPolicyName).String()) == "string" {
		log.Println(constants.KEY_THROTTLE_FILTER + " Resource level throttle policy : " + resourceLevelPolicyName)
		if len(resourceLevelPolicyName) > 0 && resourceLevelPolicyName != constants.UNLIMITED_TIER &&
			!isPolicyExist(deployedPolicies, resourceLevelPolicyName, constants.RESOURCE_LEVEL_PREFIX) {
			log.Println(constants.KEY_THROTTLE_FILTER + " Resource level throttle policy " +
				resourceLevelPolicyName + " does not exist.")
			resp = &ext_authz.CheckResponse{
				Status: &status.Status{Code: int32(rpc.INTERNAL)},
				HttpResponse: &ext_authz.CheckResponse_DeniedResponse{
					DeniedResponse: &ext_authz.DeniedHttpResponse{
						Status: &envoy_type.HttpStatus{
							Code: envoy_type.StatusCode_InternalServerError,
						},
						Body: "Internal server error occured.",
					},
				},
			}
			return false, resp
		}
	}
	log.Println(constants.KEY_THROTTLE_FILTER + " Checking resource level throttling-out.")
	if isResourceLevelThrottled(ctx, resourceLevelPolicyName, deployedPolicies, apiVersion) {
		log.Println(constants.KEY_THROTTLE_FILTER + " Resource level throttled out. Sending throttled out response.")
		resp = &ext_authz.CheckResponse{
			Status: &status.Status{Code: int32(rpc.RESOURCE_EXHAUSTED)},
			HttpResponse: &ext_authz.CheckResponse_DeniedResponse{
				DeniedResponse: &ext_authz.DeniedHttpResponse{
					Status: &envoy_type.HttpStatus{
						Code: envoy_type.StatusCode_TooManyRequests,
					},
					Body: "Resource level throttled out",
				},
			},
		}
		return false, resp
	} else {
		log.Println(constants.KEY_THROTTLE_FILTER + "Resource level throttled out: false")
	}
	resp = &ext_authz.CheckResponse{
		Status: &status.Status{Code: int32(rpc.OK)},
		HttpResponse: &ext_authz.CheckResponse_OkResponse{
			OkResponse: &ext_authz.OkHttpResponse{

			},
		},
	}
	return true, resp
}

func isAPILevelThrottled(ctx context.Context, apiVersion string) bool {
	var apiThrottleKey string = getContextDetails(ctx, constants.BASE_PATH)
	if (reflect.TypeOf(apiVersion).String()) == "string" {
		apiThrottleKey += ":" + apiVersion
	}
	if enabledGlobalTMEventPublishing {
		apiThrottleKey += "_default"
	}
	if !enabledGlobalTMEventPublishing {
		return isApiLevelThrottled(apiThrottleKey)
	}
	return true
}

func isResourceLevelThrottled(ctx context.Context, policy string, deployedPolicies map[string]policyDetails,
	apiVersion string) bool {
	if (reflect.TypeOf(policy).String()) == "string" {
		if policy == constants.UNLIMITED_TIER {
			return false
		}
		var resourceLevelThrottleKey string = getContextDetails(ctx, constants.RESOURCE_NAME)
		if (reflect.TypeOf(apiVersion).String()) == "string" {
			resourceLevelThrottleKey += ":" + apiVersion
		}
		if enabledGlobalTMEventPublishing {
			resourceLevelThrottleKey += "_default"
		}
		log.Println(constants.KEY_THROTTLE_FILTER + " Resource level throttle key : " + resourceLevelThrottleKey)
		if !enabledGlobalTMEventPublishing {
			return isResourceThrottled(resourceLevelThrottleKey)
		}
	}
	return false
}

func isSubscriptionLevelThrottled(ctx context.Context,
	deployedPolicies map[string]policyDetails, apiVersion string) (bool, bool) {
	tier := getContextDetails(ctx, constants.TIER)
	if tier == constants.UNLIMITED_TIER {
		return false, false
	}
	var subscriptionLevelThrottleKey string = getContextDetails(ctx, constants.APPLICATION_ID) + ":" +
		getContextDetails(ctx, constants.BASE_PATH)
	if (reflect.TypeOf(apiVersion).String()) == "string" {
		subscriptionLevelThrottleKey += ":" + apiVersion
	}
	log.Println(constants.KEY_THROTTLE_FILTER + " Subscription level throttle key : " + subscriptionLevelThrottleKey)
	if !enabledGlobalTMEventPublishing {
		stopOnQuota := deployedPolicies[constants.SUB_LEVEL_PREFIX+tier].stopOnQuota
		var isThrottled bool = isSubsLevelThrottled(subscriptionLevelThrottleKey)
		return isThrottled, stopOnQuota

	}
	return false, false
}

func isApplicationLevelThrottled(ctx context.Context, deployedPolicies map[string]policyDetails) bool {
	if getContextDetails(ctx, constants.APPLICATION_TIER) == constants.UNLIMITED_TIER {
		return false
	}
	var applicationLevelThrottleKey string = getContextDetails(ctx, constants.APPLICATION_ID) + ":" +
		getContextDetails(ctx, constants.USERNAME)
	log.Println(constants.KEY_THROTTLE_FILTER + " Application level throttle key : " + applicationLevelThrottleKey)
	if !enabledGlobalTMEventPublishing {
		return isAppLevelThrottled(applicationLevelThrottleKey)
	}
	return false
}

func isPolicyExist(deployedPolicies map[string]policyDetails, policyName string, prefix string) bool {
	if _, found := deployedPolicies[prefix+policyName]; found {
		return true
	} else {
		return false
	}
}

func generateThrottleEvent(ctx context.Context, deployedPolicies map[string]policyDetails) dtos.RequestStreamDTO {
	requestStreamDTO := dtos.RequestStreamDTO{}
	if !enabledGlobalTMEventPublishing {
		requestStreamDTO = generateLocalThrottleEvent(ctx, deployedPolicies)
	}
	log.Println(constants.KEY_THROTTLE_FILTER + " Resource key : " + requestStreamDTO.ResourceKey +
		" Subscription key : " + requestStreamDTO.SubscriptionKey + " App key : " + requestStreamDTO.AppKey +
		" API key : " + requestStreamDTO.ApiKey + " Resource Tier : " + requestStreamDTO.ResourceTier +
		" Subscription Tier : " + requestStreamDTO.SubscriptionTier + " App Tier : " + requestStreamDTO.AppTier +
		" API Tier : " + requestStreamDTO.ApiTier)
	return requestStreamDTO
}

func generateLocalThrottleEvent(ctx context.Context,
	deployedPolicies map[string]policyDetails) dtos.RequestStreamDTO {
	requestStreamDTO := dtos.RequestStreamDTO{}
	requestStreamDTO = setCommonThrottleData(ctx, deployedPolicies)
	appTier := getContextDetails(ctx, constants.APPLICATION_TIER)
	appPolicyDetails := getPolicyDetails(deployedPolicies, appTier, constants.APP_LEVEL_PREFIX)
	requestStreamDTO.AppTierCount = appPolicyDetails.count
	requestStreamDTO.AppTierUnitTime = appPolicyDetails.unitTime
	requestStreamDTO.AppTierTimeUnit = appPolicyDetails.timeUnit
	subscriptionTier := getContextDetails(ctx, constants.TIER)
	subPolicyDetails := getPolicyDetails(deployedPolicies, subscriptionTier, constants.SUB_LEVEL_PREFIX)
	requestStreamDTO.SubscriptionTierCount = subPolicyDetails.count
	requestStreamDTO.SubscriptionTierUnitTime = subPolicyDetails.unitTime
	requestStreamDTO.SubscriptionTierTimeUnit = subPolicyDetails.timeUnit
	requestStreamDTO.StopOnQuota = subPolicyDetails.stopOnQuota
	resourcePolicyDetails := getPolicyDetails(deployedPolicies, requestStreamDTO.ResourceTier,
		constants.RESOURCE_LEVEL_PREFIX)
	requestStreamDTO.ResourceTierCount = resourcePolicyDetails.count
	requestStreamDTO.ResourceTierUnitTime = resourcePolicyDetails.unitTime
	requestStreamDTO.ResourceTierTimeUnit = resourcePolicyDetails.timeUnit
	apiPolicyDetails := getPolicyDetails(deployedPolicies, requestStreamDTO.ApiTier, constants.RESOURCE_LEVEL_PREFIX)
	requestStreamDTO.ApiTierCount = apiPolicyDetails.count
	requestStreamDTO.ApiTierUnitTime = apiPolicyDetails.unitTime
	requestStreamDTO.ApiTierTimeUnit = apiPolicyDetails.timeUnit
	requestStreamDTO = setThrottleKeysWithVersion(ctx, requestStreamDTO)
	return requestStreamDTO
}

func getPolicyDetails(deployedPolicies map[string]policyDetails, policyName string, prefix string) policyDetails {
	if policyName == constants.UNLIMITED_TIER || len(policyName) == 0 {
		return policyDetails{count: -1, unitTime: -1, timeUnit: "min", stopOnQuota: true}
	}
	return deployedPolicies[prefix+policyName]
}

func setThrottleKeysWithVersion(ctx context.Context, requestStreamDTO dtos.RequestStreamDTO) dtos.RequestStreamDTO {
	apiVersion := getContextDetails(ctx, constants.API_VERSION)
	if (reflect.TypeOf(apiVersion).String()) == "string" {
		requestStreamDTO.ApiVersion = apiVersion
		requestStreamDTO.ApiKey += ":" + apiVersion
		requestStreamDTO.SubscriptionKey += ":" + apiVersion
		requestStreamDTO.ResourceKey += ":" + apiVersion
	}
	return requestStreamDTO
}

func setCommonThrottleData(ctx context.Context, deployedPolicies map[string]policyDetails) dtos.RequestStreamDTO {
	requestStreamDTO := dtos.RequestStreamDTO{ResetTimestamp: 0, RemainingQuota: 0, IsThrottled: false,
		StopOnQuota: true, ResourceTierCount: -1, ResourceTierUnitTime: -1, AppTierCount: -1, AppTierUnitTime: -1,
		ApiTierCount: -1, ApiTierUnitTime: -1, SubscriptionTierCount: -1, SubscriptionTierUnitTime: -1}
	requestStreamDTO.AppTier = getContextDetails(ctx, constants.APPLICATION_TIER)
	requestStreamDTO.ApiTier = getContextDetails(ctx, constants.API_TIER)
	requestStreamDTO.SubscriptionTier = getContextDetails(ctx, constants.TIER)
	requestStreamDTO.ApiKey = getContextDetails(ctx, constants.BASE_PATH)

	if requestStreamDTO.ApiTier != constants.UNLIMITED_TIER && requestStreamDTO.ApiTier != "" {
		requestStreamDTO.ResourceTier = requestStreamDTO.ApiTier
		requestStreamDTO.ResourceKey = requestStreamDTO.ApiKey
	} else {
		var resourceKey string = getContextDetails(ctx, constants.RESOURCE_NAME)
		requestStreamDTO.ResourceTier = getContextDetails(ctx, constants.RESOURCE_TIER)
		requestStreamDTO.ResourceKey = resourceKey
	}
	requestStreamDTO.AppKey = getContextDetails(ctx, constants.APPLICATION_ID) + ":" +
		getContextDetails(ctx, constants.USERNAME)
	requestStreamDTO.SubscriptionKey = getContextDetails(ctx, constants.APPLICATION_ID) + ":" +
		getContextDetails(ctx, constants.BASE_PATH)
	return requestStreamDTO
}

func publishEvent(throttleEvent dtos.RequestStreamDTO) {
	log.Println(constants.KEY_THROTTLE_FILTER + " Checking application sending throttle event to another worker.")
	PublishNonThrottleEvent(throttleEvent)
}
