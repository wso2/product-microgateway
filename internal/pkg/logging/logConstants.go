package logging

import "github.com/sirupsen/logrus"

const (
    LOG_FILE_NAME = "resources/logs/microgateway.log"
    DEFAULT_LOG_LEVEL = logrus.WarnLevel

    //log levels
	LEVEL_PANIC = "PANC"
	LEVEL_FATAL = "FATL"
	LEVEL_ERROR = "ERRO"
	LEVEL_WARN  = "WARN"
	LEVEL_INFO  = "INFO"
	LEVEL_DEBUG = "DEBG"
)
