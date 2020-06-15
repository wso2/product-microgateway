package logging

import (
	"fmt"
	"github.com/sirupsen/logrus"
	"github.com/wso2/micro-gw/config"
	lumberjack "gopkg.in/natefinch/lumberjack.v2"
	"io"
	"log"
	"os"
	"strings"
)


type PlainFormatter struct {
	TimestampFormat string
	LevelDesc       []string
}

func init() {

	err := initGlobalLogger(logFilename)
	if err != nil {
		log.Println("Failed to initialize logging")
	}
}

func initGlobalLogger(filename string) (error) {

	// Create the log file if doesn't exist. And append to it if it already exists.
	_, err := os.OpenFile(filename, os.O_WRONLY|os.O_APPEND|os.O_CREATE, 0644)

	logrus.SetReportCaller(true)
	formatter := loggerFromat()
	logrus.SetFormatter(formatter)

	//default log level set to warn level
	logrus.SetLevel(defaultLogLevel)

	if err != nil {
		// Cannot open log file. Logging to stderr
		log.Println("failed to open logfile", err)
	} else {
		//log output set to stdout and file
		multiWriter := io.MultiWriter(os.Stdout, setLogRotation(filename))
		logrus.SetOutput(multiWriter)
	}

	return err
}

func loggerFromat() *PlainFormatter {

	formatter := new(PlainFormatter)
	formatter.TimestampFormat = "2006-01-02 15:04:05"
	formatter.LevelDesc = []string{
		LEVEL_PANIC,
		LEVEL_FATAL,
		LEVEL_ERROR,
		LEVEL_WARN,
		LEVEL_INFO,
		LEVEL_DEBUG}

	return formatter
}

func formatFilePath(path string) string {
	arr := strings.Split(path, "/")
	return arr[len(arr)-1]
}

func (f *PlainFormatter) Format(entry *logrus.Entry) ([]byte, error) {
	timestamp := fmt.Sprintf(entry.Time.Format(f.TimestampFormat))

	return []byte(fmt.Sprintf("%s %s [%s:%d] - [%s] [-] %s\n",
		timestamp,
		f.LevelDesc[entry.Level],
		formatFilePath(entry.Caller.File),
		entry.Caller.Line,
		formatFilePath(entry.Caller.Function), entry.Message)), nil
}

func setLogRotation(filename string) io.Writer {
	logConf, errReadConfig := config.ReadLogConfigs()
	if errReadConfig != nil {
		log.Println("Error loading configuration. ", errReadConfig)
		return nil
	}

	rotationWriter := &lumberjack.Logger{
		Filename:   filename,
		MaxSize:    logConf.Rotation.MaxSize, // megabytes
		MaxBackups: logConf.Rotation.MaxBackups,
		MaxAge:     logConf.Rotation.MaxAge,   //days
		Compress:   logConf.Rotation.Compress, // disabled by default
	}

	return rotationWriter
}
