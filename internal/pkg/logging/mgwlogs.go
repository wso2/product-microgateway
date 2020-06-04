package logging

import (
	"fmt"
	slog "github.com/sirupsen/logrus"
	lumberjack "gopkg.in/natefinch/lumberjack.v2"
	"os"
	"runtime"
	"strings"
)

type PlainFormatter struct {
	TimestampFormat string
	LevelDesc []string
	CallerPrettyfier func(*runtime.Frame) (function string, file string)
}

const (
	FORMAT_DEFAULT = "%s %s [%s:%d] - [%s] [-] %s\n"

	LEVEL_PANIC = "PANC"
	LEVEL_FATAL = "FATL"
	LEVEL_ERROR = "ERRO"
	LEVEL_WARN = "WARN"
	LEVEL_INFO = "INFO"
	LEVEL_DEBUG = "DEBG"
)

func init() {

	var filename string = "logfile.log"
	// Create the log file if doesn't exist. And append to it if it already exists.
	_, err := os.OpenFile(filename, os.O_WRONLY | os.O_APPEND | os.O_CREATE, 0644)

	formatter := new(PlainFormatter)

	slog.SetReportCaller(true)
	formatter.TimestampFormat = "2006-01-02 15:04:05"
	formatter.LevelDesc = []string{
		LEVEL_PANIC ,
		LEVEL_FATAL,
		LEVEL_ERROR,
		LEVEL_WARN,
		LEVEL_INFO,
		LEVEL_DEBUG}
	slog.SetFormatter(formatter)

	if err != nil {
		// Cannot open log file. Logging to stderr
		fmt.Println(err)
	}else{
		//mw := io.MultiWriter(os.Stdout, f)
		//slog.SetOutput(mw)
		slog.SetOutput(&lumberjack.Logger{
			Filename:   filename,
			MaxSize:    1, // megabytes
			MaxBackups: 3,
			MaxAge:     28, //days
			Compress:   true, // disabled by default
		})

		//slog.SetOutput(os.Stdout)
	}

}

func formatFilePath(path string) string {
	arr := strings.Split(path, "/")
	return arr[len(arr)-1]
}


func (f *PlainFormatter) Format(entry *slog.Entry) ([]byte, error) {
	timestamp := fmt.Sprintf(entry.Time.Format(f.TimestampFormat))

	return []byte(fmt.Sprintf(FORMAT_DEFAULT,
		timestamp,
		f.LevelDesc[entry.Level],
		formatFilePath(entry.Caller.File),
		entry.Caller.Line,
		formatFilePath(entry.Caller.Function), entry.Message)), nil
}


