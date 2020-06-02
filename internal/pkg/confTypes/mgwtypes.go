package confTypes

import (
	"sync"
	"time"
)

/**
* Experimenting asynchronous communication between go routines using channels
* This uses singleton pattern where creating a single channel for communication
*
* To get a instance of the channel for a data publisher go routine
*  `publisher := NewSender()`
*
* Create a receiver channel in worker go routine
* receiver := NewReceiver()
*
* From publisher go routine, feed string value to the channel
* publisher<- "some value"
*
* In worker go routine, read the value sent by the publisher
* message := <-receiver
 */
var once sync.Once

var (
	C chan string // better to be interface{} type which could send any type of data.
)

func NewSender() chan string {
	once.Do(func() {
		C = make(chan string)
	})
	return C
}

func NewReceiver() chan string {
	once.Do(func() {
		C = make(chan string)
	})
	return C
}

// The mgw configuration struct.
type Config struct {
	// The server parameters.
	Server struct {
		IP   string
		Port string
	}

	// TLS configuration
	TLS struct {
		Enabled  bool
		KeyStore string // Key store location
		Alias    string // Alias of the private key
		Password string // Key store alias
	}

	// Server admin user credentials.
	Admin struct {
		User struct {
			UserName string
			Password string
		}
	}

	// Defines the swagger definition location. (For now,
	// it is the individual file path. Should be improved to use with directory)
	Apis struct {
		Location string
	}

	//envoy proxy configuration
	Envoy struct {
		ListenerAddress         string
		ListenerPort            uint32
		ApiDefaultPort          uint32
		ClusterTimeoutInSeconds time.Duration
	}
}
