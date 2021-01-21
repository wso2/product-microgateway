package messagelisteners

import (
	"encoding/json"

	"github.com/streadway/amqp"
	logger "github.com/wso2/micro-gw/loggers"
)

func handleTokenRevocation(deliveries <-chan amqp.Delivery, done chan error) {

	for d := range deliveries {
		var notification EventTokenRevocationNotification
		// var eventType string
		json.Unmarshal([]byte(string(d.Body)), &notification)
		logger.LoggerJMS.Printf("\n\n[%v]", d.DeliveryTag)
		logger.LoggerJMS.Printf("RevokedToken: %s, Token Type: %s", notification.Event.PayloadData.RevokedToken,
			notification.Event.PayloadData.Type)

		d.Ack(false)
	}
	logger.LoggerJMS.Infof("handle: deliveries channel closed")
	done <- nil
}

// EventTokenRevocationNotification for struct
type EventTokenRevocationNotification struct {
	Event struct {
		PayloadData struct {
			EventID      string  `json:"eventId"`
			RevokedToken string  `json:"revokedToken"`
			TTL          string  `json:"ttl"`
			ExpiryTime   float32 `json:"expiryTime"`
			Type         string  `json:"type"`
			TenantID     string  `json:"tenantId"`
		} `json:"payloadData"`
	} `json:"event"`
}
