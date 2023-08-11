package metrics

import (
	"net/http"

	"github.com/prometheus/client_golang/prometheus/promhttp"
)

// StartMetricsServer initializes and starts the metrics server on the given port.
func StartMetricsServer(port string) {
	http.Handle("/metrics", promhttp.Handler())
	http.ListenAndServe(":"+port, nil)
}
