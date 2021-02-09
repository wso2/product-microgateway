package messaging

import (
	"net/url"
	"strconv"
	"strings"
	"time"

	"github.com/streadway/amqp"
	logger "github.com/wso2/micro-gw/loggers"
)

// Try to connect to the RabbitMQ server as long as it takes to establish a connection
func connectToRabbitMQ(url string) (*amqp.Connection, error) {
	var err error = nil
	var conn *amqp.Connection
	conn, err = amqp.Dial(url)
	if err == nil {
		return conn, nil
	}
	_, conn, err = connectionRetry("")
	return conn, err
}

// reconnect reconnects to server if the connection or a channel
// is closed unexpectedly.
func (c *Consumer) reconnect(key string) {
	var err error
	conErr := <-c.conn.NotifyClose(make(chan *amqp.Error))
	if conErr != nil {
		logger.LoggerMsg.Errorf("CRITICAL: Connection dropped for %s, reconnecting...", key)
		c, rabbitConn, err = connectionRetry(key)
		if err != nil {
			logger.LoggerMsg.Errorf("Cannot establish connection for topic %s", key)
		}
	}
}

// connectionRetry
func connectionRetry(key string) (*Consumer, *amqp.Connection, error) {
	var err error = nil
	var i int
	logger.LoggerMsg.Infof("length.... %d", len(amqpURIArray))

	for j := 0; j < len(amqpURIArray); j++ {
		var maxAttempt int = amqpURIArray[j].retryCount
		var retryInterval time.Duration = time.Duration(amqpURIArray[j].connectionDelay) * time.Second

		if maxAttempt == 0 {
			maxAttempt = 30
		}

		if retryInterval == 0 {
			retryInterval = 10 * time.Second
		}

		logger.LoggerMsg.Infof("maxAttempt %d", maxAttempt)
		logger.LoggerMsg.Infof("retryInterval %d", retryInterval)

		for i := 1; i <= maxAttempt; i++ {
			logger.LoggerMsg.Infof("connecting...%s", amqpURIArray[j].url)
			rabbitConn, err = amqp.Dial(amqpURIArray[j].url + "/")
			if err == nil {
				if key != "" && len(key) > 0 {
					logger.LoggerMsg.Infof("Reconnected to topic %s", key)
					c, err := StartConsumer(key)
					return c, rabbitConn, err
				}
				return nil, rabbitConn, nil
			}
			time.Sleep(retryInterval)
		}
		if i == maxAttempt {
			logger.LoggerMsg.Infof("Exceeds maxAttempts %d. move to next url", maxAttempt)
		}
	}
	return nil, rabbitConn, err
}

//extract AMQPURLList from EventListening connection url
func retrieveAMQPURLList() []amqpFailoverURL {
	connectionURL := mgwConfig.ControlPlane.EventHub.JmsConnectionParameters.EventListeningEndpoints
	mainURL := strings.Split(connectionURL, "@")[0]
	strParts := strings.Split(connectionURL, "brokerlist=")
	if strParts != nil {
		brokerList := strParts[1]
		brokerList = brokerList[1 : len(brokerList)-1]
		urlList := strings.Split(brokerList, ";")
		amqlURLList := []amqpFailoverURL{}

		for _, conURL := range urlList {
			var delay int = 0
			var retries int = 0
			host := strings.Split(conURL, "tcp://")[1]
			amqpConnectionURL := strings.Split(mainURL+"@"+host, "?")[0]
			u, err := url.Parse(mainURL + "@" + host)
			if err != nil {
				panic(err)
			}
			m, _ := url.ParseQuery(u.RawQuery)
			if m["connectdelay"] != nil {
				connectdelay := m["connectdelay"][0]
				delay, err = strconv.Atoi(connectdelay[1 : len(connectdelay)-1])
			}

			if m["retries"] != nil {
				retrycount := m["retries"][0]
				retries, err = strconv.Atoi(retrycount[1 : len(retrycount)-1])
			}

			failoverurlObj := amqpFailoverURL{url: amqpConnectionURL, retryCount: retries, connectionDelay: delay}
			amqlURLList = append(amqlURLList, failoverurlObj)
		}
		return amqlURLList
	}
	return nil
}

type amqpFailoverURL struct {
	url             string
	retryCount      int
	connectionDelay int
}
