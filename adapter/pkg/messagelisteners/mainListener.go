package messagelisteners

import (
	"fmt"
	"strings"
	"time"

	"github.com/streadway/amqp"
	"github.com/wso2/micro-gw/config"
	logger "github.com/wso2/micro-gw/loggers"
)

var (
	mgwConfig *config.Config
	lifetime  = 0 * time.Second
	amqpURI   string
)

const exchange = "amq.topic"
const exchangeType = "topic"
const consumerTag = "jms-consumer"

// ProcessEvents for struct
func ProcessEvents(config *config.Config) {
	mgwConfig = config
	amqpURI = mgwConfig.Enforcer.EventHub.JmsConnectionParameters.EventListeningEndpoints
	bindingKeys := []string{"notification", "keymanager", "tokenRevocation", "cacheInvalidation"}

	for i, key := range bindingKeys {
		logger.LoggerJMS.Infof("shutting down index %v key %s ", i, key)
		go func(key string) {
			// check durable or non durable
			c, err := NewConsumer(key+"-queue", key)
			if err != nil {
				logger.LoggerJMS.Fatalf("%s", err)
			}
			if lifetime > 0 {
				logger.LoggerJMS.Infof("running %s events for %s", key, lifetime)
				time.Sleep(lifetime)
			} else {
				logger.LoggerJMS.Infof("process of receiving %s events running forever", key)
				select {}
			}

			logger.LoggerJMS.Infof("shutting down")
			if err := c.Shutdown(); err != nil {
				logger.LoggerJMS.Fatalf("error during shutdown: %s", err)
			}
		}(key)
	}
}

// NewConsumer for struct
func NewConsumer(queueName string, key string) (*Consumer, error) {
	c := &Consumer{
		conn:    nil,
		channel: nil,
		tag:     consumerTag,
		done:    make(chan error),
	}

	var err error

	logger.LoggerJMS.Infof("dialing %q", amqpURI)
	c.conn, err = amqp.Dial(amqpURI)
	if err != nil {
		return nil, fmt.Errorf("Dial: %s", err)
	}

	go func() {
		logger.LoggerJMS.Infof("closing: %s", <-c.conn.NotifyClose(make(chan *amqp.Error)))
	}()

	logger.LoggerJMS.Infof("got Connection, getting Channel")
	c.channel, err = c.conn.Channel()
	if err != nil {
		return nil, fmt.Errorf("Channel: %s", err)
	}

	logger.LoggerJMS.Infof("got Channel, declaring Exchange (%q)", exchange)
	if err = c.channel.ExchangeDeclare(
		exchange,     // name of the exchange
		exchangeType, // type
		true,         // durable
		false,        // delete when complete
		false,        // internal
		false,        // noWait
		nil,          // arguments
	); err != nil {
		return nil, fmt.Errorf("Exchange Declare: %s", err)
	}

	logger.LoggerJMS.Infof("declared Exchange, declaring Queue %q", queueName)
	queue, err := c.channel.QueueDeclare(
		queueName, // name of the queue
		true,      // durable
		false,     // delete when usused
		false,     // exclusive
		false,     // noWait
		nil,       // arguments
	)
	if err != nil {
		return nil, fmt.Errorf("Queue Declare: %s", err)
	}

	logger.LoggerJMS.Infof("declared Queue (%q %d messages, %d consumers), binding to Exchange (key %q)",
		queue.Name, queue.Messages, queue.Consumers, key)

	if err = c.channel.QueueBind(
		queue.Name, // name of the queue
		key,        // bindingKey
		exchange,   // sourceExchange
		false,      // noWait
		nil,        // arguments
	); err != nil {
		return nil, fmt.Errorf("Queue Bind: %s", err)
	}

	logger.LoggerJMS.Infof("Queue bound to Exchange, starting Consume (consumer tag %q)", c.tag)
	deliveries, err := c.channel.Consume(
		queue.Name, // name
		c.tag,      // consumerTag,
		false,      // noAck
		false,      // exclusive
		false,      // noLocal
		false,      // noWait
		nil,        // arguments
	)
	if err != nil {
		return nil, fmt.Errorf("Queue Consume: %s", err)
	}

	if strings.EqualFold(key, "notification") {
		go handleNotification(deliveries, c.done)
	} else if strings.EqualFold(key, "keyManager") {
		go handleKMConfiguration(deliveries, c.done)
	} else if strings.EqualFold(key, "tokenRevocation") {
		go handleTokenRevocation(deliveries, c.done)
	} else if strings.EqualFold(key, "cacheInvalidation") {
		go handleCacheInvalidation(deliveries, c.done)
	}

	return c, nil
}

// Shutdown when error happens
func (c *Consumer) Shutdown() error {
	// will close() the deliveries channel
	if err := c.channel.Cancel(c.tag, true); err != nil {
		return fmt.Errorf("Consumer cancel failed: %s", err)
	}

	if err := c.conn.Close(); err != nil {
		return fmt.Errorf("AMQP connection close error: %s", err)
	}

	defer logger.LoggerJMS.Infof("AMQP shutdown OK")

	// wait for handle() to exit
	return <-c.done
}

// Consumer struct.
type Consumer struct {
	conn    *amqp.Connection
	channel *amqp.Channel
	tag     string
	done    chan error
}
