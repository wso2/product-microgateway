package messagelisteners

import (
	"encoding/base64"
	"encoding/json"
	"strings"

	"github.com/streadway/amqp"
	logger "github.com/wso2/micro-gw/loggers"
)

// handleNotification to process
func handleNotification(deliveries <-chan amqp.Delivery, done chan error) {

	for d := range deliveries {
		var notification EventNotification
		var eventType string
		json.Unmarshal([]byte(string(d.Body)), &notification)
		logger.LoggerJMS.Infof("[%v]", d.DeliveryTag)
		// fmt.Printf("EventType: %s, event: %s", notification.Event.PayloadData.EventType, notification.Event.PayloadData.Event)

		var decodedByte, _ = base64.StdEncoding.DecodeString(notification.Event.PayloadData.Event)
		logger.LoggerJMS.Infof("\n\n[%s]", decodedByte)
		eventType = notification.Event.PayloadData.EventType
		if strings.Contains(eventType, "API") {
			handleAPIEvents(decodedByte, eventType)
		} else if strings.Contains(eventType, "APPLICATION") {
			handleApplicationEvents(decodedByte, eventType)
		} else if strings.Contains(eventType, "SUBSCRIPTIONS") {
			handleSubscriptionEvents(decodedByte, eventType)
		} else if strings.Contains(eventType, "SCOPE") {
			handleScopeEvents(decodedByte, eventType)
		} else {
			handlePolicyEvents(decodedByte, eventType)
		}
		d.Ack(false)
	}
	logger.LoggerJMS.Infof("handle: deliveries channel closed")
	done <- nil
}

// handleAPIRelatedEvents to process
func handleAPIEvents(data []byte, eventType string) {
	var apiEvent APIEvent
	json.Unmarshal([]byte(string(data)), &apiEvent)
	logger.LoggerJMS.Infof("EventType: %s, api: %v", apiEvent.Type, apiEvent.APIID)
	// REMOVE_API_FROM_GATEWAY, DEPLOY_API_IN_GATEWAY --> check the feasibility of sending one event
	// EventType.API_DELETE, EventType.API_UPDATE, EventType.API_LIFECYCLE_CHANGE, EventType.API_DELETE
}

// handleApplicationRelatedEvents to process
func handleApplicationEvents(data []byte, eventType string) {
	if strings.EqualFold("APPLICATION_REGISTRATION_CREATE", eventType) ||
		strings.EqualFold("REMOVE_APPLICATION_KEYMAPPING", eventType) {
		var applicationRegistrationEvent ApplicationRegistrationEvent
		json.Unmarshal([]byte(string(data)), &applicationRegistrationEvent)
		logger.LoggerJMS.Infof("EventType: %s for application %v, consumerKey %s", eventType,
			applicationRegistrationEvent.ApplicationID, applicationRegistrationEvent.ConsumerKey)
		// EventType.APPLICATION_REGISTRATION_CREATE, EventType.REMOVE_APPLICATION_KEYMAPPING
	} else {
		var applicationEvent ApplicationEvent
		json.Unmarshal([]byte(string(data)), &applicationEvent)
		logger.LoggerJMS.Infof("EventType: %s for application: %s", applicationEvent.Type, applicationEvent.ApplicationName)
		// EventType.APPLICATION_CREATE, EventType.APPLICATION_UPDATE, EventType.APPLICATION_REGISTRATION_CREATE,
		// EventType.APPLICATION_DELETE, EventType.REMOVE_APPLICATION_KEYMAPPING
	}
}

// handleSubscriptionRelatedEvents to process
func handleSubscriptionEvents(data []byte, eventType string) {
	var subscriptionEvent SubscriptionEvent
	json.Unmarshal([]byte(string(data)), &subscriptionEvent)
	logger.LoggerJMS.Infof("EventType: %s for subscription: %v. application %v with api %v", subscriptionEvent.Type,
		subscriptionEvent.SubscriptionID, subscriptionEvent.ApplicationID, subscriptionEvent.APIID)
	// SUBSCRIPTIONS_CREATE, SUBSCRIPTIONS_UPDATE, SUBSCRIPTIONS_DELETE
}

// handleScopeRelatedEvents to process
func handleScopeEvents(data []byte, eventType string) {
	var scopeEvent ScopeEvent
	json.Unmarshal([]byte(string(data)), &scopeEvent)
	logger.LoggerJMS.Infof("EventType: %s for scope: %s", scopeEvent.Type, scopeEvent.DisplayName)
	// EventType.SCOPE_CREATE, EventType.SCOPE_UPDATE, EventType.SCOPE_DELETE
}

// handlePolicyRelatedEvents to process
func handlePolicyEvents(data []byte, eventType string) {
	var policyEvent PolicyInfo
	json.Unmarshal([]byte(string(data)), &policyEvent)
	if strings.EqualFold(eventType, "POLICY_CREATE") {
		logger.LoggerJMS.Infof("Policy: %s for policy type: %s", policyEvent.PolicyName, policyEvent.PolicyType)
	} else if strings.EqualFold(eventType, "POLICY_UPDATE") {
		logger.LoggerJMS.Infof("Policy: %s for policy type: %s", policyEvent.PolicyName, policyEvent.PolicyType)
	} else if strings.EqualFold(eventType, "POLICY_DELETE") {
		logger.LoggerJMS.Infof("Policy: %s for policy type: %s", policyEvent.PolicyName, policyEvent.PolicyType)
	}

	if strings.EqualFold("API", policyEvent.PolicyType) {
		var apiPolicyEvent APIPolicyEvent
		json.Unmarshal([]byte(string(data)), &apiPolicyEvent)
		logger.LoggerJMS.Infof("Policy: %s for policy type: %s , AddedConditionGroups: %s , DeletedConditionGroups: %s",
			apiPolicyEvent.PolicyName, apiPolicyEvent.PolicyType, apiPolicyEvent.AddedConditionGroupIds,
			apiPolicyEvent.DeletedConditionGroupIds)
	} else if strings.EqualFold("APPLICATION", policyEvent.PolicyType) {
		logger.LoggerJMS.Infof("Policy: %s for policy type: %s", policyEvent.PolicyName, policyEvent.PolicyType)
	} else if strings.EqualFold("SUBSCRIPTION", policyEvent.PolicyType) {
		var subscriptionPolicyEvent SubscriptionPolicyEvent
		json.Unmarshal([]byte(string(data)), &subscriptionPolicyEvent)
		logger.LoggerJMS.Infof("Policy: %s for policy type: %s , rateLimitCount : %v, QuotaType: %s ", subscriptionPolicyEvent.PolicyName,
			subscriptionPolicyEvent.PolicyType, subscriptionPolicyEvent.RateLimitCount, subscriptionPolicyEvent.QuotaType)
	}
	// EventType.POLICY_CREATE, EventType.POLICY_UPDATE, EventType.POLICY_DELETE, API, APPLICATION, SUBSCRIPTION
}

// EventNotification for struct event notifications
type EventNotification struct {
	Event struct {
		PayloadData struct {
			EventType string  `json:"eventType"`
			Timstamp  float64 `json:"timstamp"`
			Event     string  `json:"event"`
		} `json:"payloadData"`
	} `json:"event"`
}

// Event for struct abstract event
type Event struct {
	EventID      string  `json:"eventId"`
	TimeStamp    float64 `json:"timeStamp"`
	Type         string  `json:"type"`
	TenantID     string  `json:"tenantId"`
	TenantDomain string  `json:"tenantDomain"`
}

// APIEvent for struct API events
type APIEvent struct {
	APIID         string   `json:"apiId"`
	GatewayLabels []string `json:"gatewayLabels"`
	APIVersion    string   `json:"apiVersion"`
	APIContext    string   `json:"apiContext"`
	APIName       string   `json:"apiName"`
	APIProvider   string   `json:"apiProvider"`
	APIStatus     string   `json:"apiStatus"`
	APIType       string   `json:"apiType"`
	Event
}

// ApplicationRegistrationEvent for struct application registration events
type ApplicationRegistrationEvent struct {
	ApplicationID int    `json:"applicationId"`
	ConsumerKey   string `json:"consumerKey"`
	KeyType       string `json:"keyType"`
	KeyManager    string `json:"keyManager"`
	Event
}

// ApplicationEvent for struct application events
type ApplicationEvent struct {
	UUID              string   `json:"uuid"`
	ApplicationID     int      `json:"applicationId"`
	ApplicationName   string   `json:"applicationName"`
	TokenType         string   `json:"tokenType"`
	ApplicationPolicy string   `json:"applicationPolicy"`
	Attributes        []string `json:"attributes"`
	Subscriber        string   `json:"subscriber"`
	GroupID           string   `json:"groupId"`
	Event
}

// SubscriptionEvent for struct subscription events
type SubscriptionEvent struct {
	SubscriptionID    int    `json:"subscriptionId"`
	APIID             int    `json:"apiId"`
	ApplicationID     int    `json:"applicationId"`
	PolicyID          string `json:"policyId"`
	SubscriptionState string `json:"subscriptionState"`
	Event
}

// ScopeEvent for struct scope events
type ScopeEvent struct {
	Name            string `json:"name"`
	DisplayName     string `json:"displayName"`
	ApplicationName string `json:"description"`
	Event
}

// PolicyInfo for struct policy Info events
type PolicyInfo struct {
	PolicyID   string `json:"policyId"`
	PolicyName string `json:"policyName"`
	QuotaType  string `json:"quotaType"`
	PolicyType string `json:"policyType"`
	Event
}

// APIPolicyEvent for struct API policy events
type APIPolicyEvent struct {
	PolicyInfo
	AddedConditionGroupIds   string `json:"addedConditionGroupIds"`
	DeletedConditionGroupIds string `json:"deletedConditionGroupIds"`
}

// SubscriptionPolicyEvent for struct subscriptionPolicy events
type SubscriptionPolicyEvent struct {
	PolicyInfo
	RateLimitCount       int    `json:"rateLimitCount"`
	RateLimitTimeUnit    string `json:"rateLimitTimeUnit"`
	StopOnQuotaReach     bool   `json:"stopOnQuotaReach"`
	GraphQLMaxComplexity int    `json:"graphQLMaxComplexity"`
	GraphQLMaxDepth      int    `json:"graphQLMaxDepth"`
}
