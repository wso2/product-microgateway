package configs

import (
	"github.com/BurntSushi/toml"
	logger "github.com/sirupsen/logrus"
	config "github.com/wso2/micro-gw/configs/confTypes"
	"io/ioutil"
	"os"
	"sync"
)
var (
	once_c sync.Once
	once_lc sync.Once
	configs *config.Config
	logConfigs *config.LogConfig
	e error
)

func ReadConfigs() (*config.Config, error) {
	once_c.Do(func() {
		configs = new(config.Config)
		mgwHome, _ := os.Getwd()
		logger.Info("MGW_HOME: ", mgwHome)
		_, err := os.Stat(mgwHome + "/resources/conf/config.toml")
		if err != nil {
			logger.Fatal("Configuration file not found.", err)
		}
		content, readErr := ioutil.ReadFile(mgwHome + "/resources/conf/config.toml")
		if readErr != nil {
			logger.Fatal("Error reading configurations. ", readErr)
		}
		_, e = toml.Decode(string(content), configs)
	})

	return configs, e
}

func ReadLogConfigs() (*config.LogConfig, error) {
	once_lc.Do(func() {
		logConfigs = new(config.LogConfig)
		mgwHome, _ := os.Getwd()
		//logger.Info("MGW_HOME: ", mgwHome)
		_, err := os.Stat(mgwHome + "/resources/conf/log_config.toml")
		if err != nil {
			logger.Fatal("Log configuration file not found.", err)
		}
		content, readErr := ioutil.ReadFile(mgwHome + "/resources/conf/log_config.toml")
		if readErr != nil {
			logger.Fatal("Error reading log configurations. ", readErr)
		}
		_, e = toml.Decode(string(content), logConfigs)

	})

	return logConfigs, e
}

func ClearLogConfigInstance()  {
	once_lc = sync.Once{}
}