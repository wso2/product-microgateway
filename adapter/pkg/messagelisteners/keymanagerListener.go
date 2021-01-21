package messagelisteners

import (
	"encoding/base64"
	"encoding/json"
	"strings"

	"github.com/streadway/amqp"
	logger "github.com/wso2/micro-gw/loggers"
)

const keyManagerConfig = "key_manager_configuration"

// handleKMEvent
func handleKMConfiguration(deliveries <-chan amqp.Delivery, done chan error) {

	for d := range deliveries {
		var notification EventKeyManagerNotification
		var keyManagerEvent KeyManagerEvent
		// var eventType string
		json.Unmarshal([]byte(string(d.Body)), &notification)
		logger.LoggerJMS.Infof("\n[%v]", d.DeliveryTag)

		var decodedByte, _ = base64.StdEncoding.DecodeString(notification.Event.PayloadData.Value)

		if strings.EqualFold(keyManagerConfig, notification.Event.PayloadData.EventType) {
			if decodedByte != nil {
				json.Unmarshal([]byte(string(decodedByte)), &keyManagerEvent)
				logger.LoggerJMS.Infof("EventType: %s, Action: %s ",
					notification.Event.PayloadData.EventType, notification.Event.PayloadData.Action)
			}
			// eventType = notification.Event.PayloadData.EventType
		}
		d.Ack(false)
	}
	logger.LoggerJMS.Info("handle: deliveries channel closed")
	done <- nil
}

// EventKeyManagerNotification for struct
type EventKeyManagerNotification struct {
	Event struct {
		PayloadData struct {
			EventType string `json:"event_type"`
			Action    string `json:"action"`
			Type      string `json:"type"`
			Enabled   bool   `json:"enabled"`
			Value     string `json:"value"`
		} `json:"payloadData"`
	} `json:"event"`
}

// KeyManagerEvent for struct
type KeyManagerEvent struct {
	ServerURL                  string   `json:"ServerURL"`
	ValidationEnable           bool     `json:"validation_enable"`
	ClaimMappings              []Claim  `json:"Claim"`
	GrantTypes                 []string `json:"grant_types"`
	EncryptPersistedTokens     bool     `json:"OAuthConfigurations.EncryptPersistedTokens"`
	EnableOauthAppCreation     bool     `json:"enable_oauth_app_creation"`
	ValidityPeriod             string   `json:"VALIDITY_PERIOD"`
	CertificateValue           string   `json:"certificate_value"`
	EnableTokenGeneration      bool     `json:"enable_token_generation"`
	Issuer                     string   `json:"issuer"`
	EnableMapOauthConsumerApps bool     `json:"enable_map_oauth_consumer_apps"`
	EnableTokenHash            bool     `json:"enable_token_hash"`
	SelfValidateJwt            bool     `json:"self_validate_jwt"`
	RevokeEndpoint             string   `json:"revoke_endpoint"`
	EnableTokenEncryption      bool     `json:"enable_token_encryption"`
	RevokeURL                  string   `json:"RevokeURL"`
	TokenURL                   string   `json:"TokenURL"`
	TokenFormatString          string   `json:"token_format_string"`
	CertificateType            string   `json:"certificate_type"`
	TokenEndpoint              string   `json:"token_endpoint"`
}

// Claim for struct
type Claim struct {
	remoteClaim string
	localClaim  string
}
