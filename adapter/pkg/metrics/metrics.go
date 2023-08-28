package metrics

import (
	"net/http"
	"os"
	"runtime"
	"strconv"
	"time"

	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/collectors"
	"github.com/prometheus/client_golang/prometheus/promhttp"
	"github.com/prometheus/procfs"
	"github.com/shirou/gopsutil/v3/cpu"
	"github.com/shirou/gopsutil/v3/host"
	"github.com/shirou/gopsutil/v3/load"
	"github.com/shirou/gopsutil/v3/mem"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
)

// PrometheusMetricType prometheus metric type
const PrometheusMetricType = "prometheus"

var (
	prometheusMetricRegistry = prometheus.NewRegistry()

	hostInfo = prometheus.NewGaugeVec(prometheus.GaugeOpts{
		Name: "host_info",
		Help: "Host Info",
	}, []string{"os"})

	availableCPUs = prometheus.NewGauge(prometheus.GaugeOpts{
		Name: "os_available_cpu_total",
		Help: "Number of available CPUs.",
	})

	freePhysicalMemory = prometheus.NewGauge(prometheus.GaugeOpts{
		Name: "os_free_physical_memory_bytes",
		Help: "Amount of free physical memory.",
	})

	totalVirtualMemory = prometheus.NewGauge(prometheus.GaugeOpts{
		Name: "os_total_virtual_memory_bytes",
		Help: "Amount of total virtual memory.",
	})
	usedVirtualMemory = prometheus.NewGauge(prometheus.GaugeOpts{
		Name: "os_used_virtual_memory_bytes",
		Help: "Amount of used virtual memory.",
	})

	systemCPULoad = prometheus.NewGauge(prometheus.GaugeOpts{
		Name: "os_system_cpu_load_percentage",
		Help: "System-wide CPU usage as a percentage.",
	})

	loadAvg = prometheus.NewGaugeVec(prometheus.GaugeOpts{
		Name: "os_system_load_average",
		Help: "Current load of CPU in the host system for the last {x} minutes",
	}, []string{"duration"})

	processStartTime = prometheus.NewGauge(prometheus.GaugeOpts{
		Name: "process_start_time_seconds",
		Help: "Start time of the process since unix epoch in seconds.",
	})

	processOpenFDs = prometheus.NewGauge(prometheus.GaugeOpts{
		Name: "process_open_fds",
		Help: "Number of open file descriptors.",
	})
)

func init() {
	// Register the Go collector with the registry
	goCollector := collectors.NewGoCollector()
	prometheusMetricRegistry.MustRegister(goCollector)

	// Register other metrics
	prometheusMetricRegistry.MustRegister(hostInfo, availableCPUs, freePhysicalMemory, usedVirtualMemory, totalVirtualMemory,
		systemCPULoad, loadAvg, processStartTime, processOpenFDs)
}

// recordMetrics record custom golang metrics
func recordMetrics() {
	for {
		host, err := host.Info()
		if handleError(err, "Failed to get host info") {
			return
		}
		hostInfo.WithLabelValues(host.OS).Set(1)
		availableCPUs.Set(float64(runtime.NumCPU()))

		v, err := mem.VirtualMemory()
		if handleError(err, "Failed to read virtual memory metrics") {
			return
		}
		freePhysicalMemory.Set(float64(v.Free))
		usedVirtualMemory.Set(float64(v.Used))
		totalVirtualMemory.Set(float64(v.Total))

		percentages, err := cpu.Percent(0, false)
		if handleError(err, "Failed to read cpu usage metrics") || len(percentages) == 0 {
			return
		}
		systemCPULoad.Set(percentages[0])

		pid := os.Getpid()
		p, err := procfs.NewProc(pid)
		if handleError(err, "Failed to get current process") {
			return
		}
		stat, err := p.Stat()
		if handleError(err, "Failed to get process stats") {
			return
		}
		t, err := stat.StartTime()
		if handleError(err, "Failed to read process start time") {
			return
		}
		processStartTime.Set(t)
		fds, err := p.FileDescriptorsLen()
		if handleError(err, "Failed to read file descriptor count") {
			return
		}
		processOpenFDs.Set(float64(fds))

		avg, err := load.Avg()
		if handleError(err, "Failed to read cpu load averages") {
			return
		}
		loadAvg.WithLabelValues("1m").Set(avg.Load1)
		loadAvg.WithLabelValues("5m").Set(avg.Load5)
		loadAvg.WithLabelValues("15m").Set(avg.Load15)

		// Sleep 5s before the next measurement
		time.Sleep(5 * time.Second)
	}

}

func handleError(err error, message string) bool {
	if err != nil {
		logger.LoggerMgw.Error(message)
		return true
	}
	return false
}

// StartPrometheusMetricsServer initializes and starts the metrics server to expose metrics to prometheus.
func StartPrometheusMetricsServer(port int32) {
	go recordMetrics()
	http.Handle("/metrics", promhttp.HandlerFor(prometheusMetricRegistry, promhttp.HandlerOpts{}))
	http.ListenAndServe(":"+strconv.FormatInt(int64(port), 10), nil)
}
