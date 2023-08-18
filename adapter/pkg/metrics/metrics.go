package metrics

import (
	"net/http"
	"os"
	"runtime"
	"strconv"
	"time"

	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promhttp"
	"github.com/shirou/gopsutil/v3/cpu"
	"github.com/shirou/gopsutil/v3/host"
	"github.com/shirou/gopsutil/v3/load"
	"github.com/shirou/gopsutil/v3/mem"
	"github.com/shirou/gopsutil/v3/process"
)

// PrometheusMetricType prometheus metric type
const PrometheusMetricType = "prometheus"

var hostInfo = prometheus.NewGaugeVec(prometheus.GaugeOpts{
	Name: "host_info",
	Help: "Host Info",
}, []string{"os"})

var availableCPUs = prometheus.NewGauge(prometheus.GaugeOpts{
	Name: "os_available_cpus",
	Help: "Number of available CPUs",
})

var freePhysicalMemory = prometheus.NewGauge(prometheus.GaugeOpts{
	Name: "os_free_physical_memory",
	Help: "Amount of free physical memory in bytes",
})

var systemCPULoad = prometheus.NewGauge(prometheus.GaugeOpts{
	Name: "os_system_cpu_load",
	Help: "System-wide CPU usage as a percentage",
})

var processCPULoad = prometheus.NewGauge(prometheus.GaugeOpts{
	Name: "os_process_cpu_load_percentage",
	Help: "CPU usage of the current process as a percentage",
})

var loadAvg = prometheus.NewGaugeVec(prometheus.GaugeOpts{
	Name: "os_system_load_average",
	Help: "Current load of CPU in the host system for the last {x} minutes",
}, []string{"duration"})

func init() {
	prometheus.MustRegister(hostInfo, availableCPUs, freePhysicalMemory, systemCPULoad, processCPULoad, loadAvg)
}

// recordMetrics record custom golang metrics
func recordMetrics() {
	for {
		host, err := host.Info()
		hostInfo.WithLabelValues(host.OS).Set(1)

		availableCPUs.Set(float64(runtime.NumCPU()))

		v, err := mem.VirtualMemory()
		if err == nil {
			freePhysicalMemory.Set(float64(v.Free))
		}

		percentages, err := cpu.Percent(0, false)
		if err == nil && len(percentages) > 0 {
			systemCPULoad.Set(percentages[0])
		}

		p, err := process.NewProcess(int32(os.Getpid()))
		if err == nil {
			percent, _ := p.CPUPercent()
			processCPULoad.Set(percent)
		}

		avg, err := load.Avg()
		if err != nil {
			return
		}
		loadAvg.WithLabelValues("1m").Set(avg.Load1)
		loadAvg.WithLabelValues("5m").Set(avg.Load5)
		loadAvg.WithLabelValues("15m").Set(avg.Load15)

		// Sleep 5s before the next measurement
		time.Sleep(5 * time.Second)
	}

}

// StartPrometheusMetricsServer initializes and starts the metrics server to expose metrics to prometheus.
func StartPrometheusMetricsServer(port int32) {
	go recordMetrics()
	http.Handle("/metrics", promhttp.Handler())
	http.ListenAndServe(":"+strconv.FormatInt(int64(port), 10), nil)
}
