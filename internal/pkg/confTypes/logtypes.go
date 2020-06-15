package confTypes

type pkg struct {
	Name     string
	LogLevel string
}

type accessLog struct {
	LogFile string
	Format string
}

// The log configuration struct.
type LogConfig struct {

	Logfile string
	Format  string
	LogLevel  string
	// log rotation parameters.
	Rotation struct {
		IP   string
		Port string
		MaxSize  int // megabytes
		MaxBackups  int
		MaxAge  int    //days
		Compress  bool
	}

	Pkg []pkg
	AccessLogs accessLog

}


