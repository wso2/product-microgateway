package messagelisteners

import (
	"encoding/json"
	"fmt"
	"log"
	"strings"
	"time"

	"github.com/streadway/amqp"
)

// ProcessCacheInvalidationEvent to process
func ProcessCacheInvalidationEvent() {
	c, err := NewConsumer("keymanager-queue", "keymanager")

	if err != nil {
		log.Fatalf("%s", err)
	}

	if lifetime > 0 {
		log.Printf("running for %s", lifetime)
		time.Sleep(lifetime)
	} else {
		log.Printf("running forever")
		select {}
	}

	log.Printf("shutting down")

	if err := c.Shutdown(); err != nil {
		log.Fatalf("error during shutdown: %s", err)
	}
}

// handleCacheInvalidation to process
func handleCacheInvalidation(deliveries <-chan amqp.Delivery, done chan error) {

	for d := range deliveries {
		var notification CacheInvalidationEvent
		var eventType string
		json.Unmarshal([]byte(string(d.Body)), &notification)
		fmt.Printf("\n\n[%v]", d.DeliveryTag)
		// fmt.Printf("EventType: %s, event: %s", notification.Event.PayloadData.EventType, notification.Event.PayloadData.Event)

		eventType = notification.Event.PayloadData.Type
		if strings.EqualFold(eventType, "resourceCache") {
			var resourceCache ResourceCache
			json.Unmarshal([]byte(string(notification.Event.PayloadData.Value)), &resourceCache)
			fmt.Printf("EventType: %s, API context: %s , API Version: %s",
				notification.Event.PayloadData.Type, resourceCache.APIContext,
				resourceCache.APIVersion)
		}
		d.Ack(false)
	}
	log.Printf("handle: deliveries channel closed")
	done <- nil
}

// CacheInvalidationEvent for struct
type CacheInvalidationEvent struct {
	Event struct {
		PayloadData struct {
			Type  string `json:"type"`
			Value string `json:"value"`
		} `json:"payloadData"`
	} `json:"event"`
}

// ResourceCache for construct
type ResourceCache struct {
	APIVersion string `json:"apiVersion"`
	Resources  []Resource
	APIContext string `json:"apiContext"`
}

// Resource for construct
type Resource struct {
	ResourceURLContext string `json:"resourceURLContext"`
	HTTPVERB           string `json:"httpVerb"`
}
