package logging

import (
	"fmt"
	"github.com/sirupsen/logrus"
	"github.com/wso2/micro-gw/config"
	"io"
	"log"
	"os"
)


var defaultLogLevel = logrus.WarnLevel


func logLevelMapper(pkgLevel string) logrus.Level {
	logLevel := defaultLogLevel
	switch pkgLevel {
	case LEVEL_WARN:
		logLevel =  logrus.WarnLevel
	case LEVEL_DEBUG:
		logLevel =  logrus.DebugLevel
	case LEVEL_ERROR:
		logLevel =  logrus.ErrorLevel
	case LEVEL_INFO:
		logLevel =  logrus.InfoLevel
	case LEVEL_FATAL:
		logLevel =  logrus.FatalLevel
	case LEVEL_PANIC:
		logLevel =  logrus.PanicLevel
	}

	return logLevel
}


func InitPackageLogger(pkgName string) *logrus.Logger {

	// Create the log file if doesn't exist. And append to it if it already exists.
	_, err := os.OpenFile(logFilename, os.O_WRONLY|os.O_APPEND|os.O_CREATE, 0644)

	pkgLogLevel := defaultLogLevel //default log level
	isPackegeLevelDefined := false

	logger := logrus.New()
	logger.SetReportCaller(true)

	formatter := loggerFromat()
	logger.SetFormatter(formatter)

	if err != nil {
		// Cannot open log file. Logging to stderr
		log.Println("failed to open logfile", err)
	} else {
		//log output set to stdout and file
		multiWriter := io.MultiWriter(os.Stdout, setLogRotation(logFilename))
		logger.SetOutput(multiWriter)
	}

	logConf, errReadConfig := config.ReadLogConfigs()
	if errReadConfig != nil {
		log.Fatal("Error loading configuration. ", errReadConfig)
	}
	fmt.Println("this is accessLogs",logConf.AccessLogs)

	for _, pkg := range logConf.Pkg {
		if pkg.Name == pkgName {
			pkgLogLevel = logLevelMapper(pkg.LogLevel)
			isPackegeLevelDefined = true
			break
		}
	}

	if !isPackegeLevelDefined {
		pkgLogLevel = logLevelMapper(logConf.LogLevel)
	}

	logger.SetLevel(pkgLogLevel)
	return logger
}
