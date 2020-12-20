package config

type Consul struct {
	//ip+port
	Address string
	//http or https
	Scheme         string
	TokenFile      string
	RequestTimeout int
	PollInterval   int
	//Whether consul health checks should be considered when updating respective envoy clusters
	HealthChecksPassingOnly bool
}
