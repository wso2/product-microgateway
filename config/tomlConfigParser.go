package config

import (
	"github.com/BurntSushi/toml"
	"github.com/sirupsen/logrus"
	config "github.com/wso2/micro-gw/internal/pkg/confTypes"
	"io/ioutil"
	"log"
	"os"
	"sync"
)
var (
	once sync.Once
	anc *config.Config
	e error
)

func ReadConfigs() (*config.Config, error) {
	once.Do(func() {
		anc = new(config.Config)
		mgwHome, _ := os.Getwd()
		logrus.Info("MGW_HOME: ", mgwHome)
		_, err := os.Stat(mgwHome + "/resources/conf/config.toml")
		if err != nil {
			log.Panic("Configuration file not found.", err)
		}
		content, readErr := ioutil.ReadFile(mgwHome + "/resources/conf/config.toml")
		if readErr != nil {
			log.Panic("Error reading configurations. ", readErr)
		}
		_, e = toml.Decode(string(content), anc)
	})

	return anc, e
}

