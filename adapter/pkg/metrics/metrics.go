package metrics

import (
	"net/http"
	"strconv"

	"github.com/prometheus/client_golang/prometheus/promhttp"
)

// PrometheusMetricType prometheus metric type
const PrometheusMetricType = "prometheus"

// StartPrometheusMetricsServer initializes and starts the metrics server to expose metrics to prometheus.
func StartPrometheusMetricsServer(port int32) {
	http.Handle("/metrics", promhttp.Handler())
	http.ListenAndServe(":"+strconv.FormatInt(int64(port), 10), nil)
}
