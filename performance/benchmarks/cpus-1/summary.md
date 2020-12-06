# WSO2 API Microgateway Performance Test Results

During each release, we execute various automated performance test scenarios and publish the results.

| Test Scenarios | Description |
| --- | --- |
| Microgateway-Passthrough-JWT | A secured API, which directly invokes the backend through Microgateway using JWT tokens |

Our test client is [Apache JMeter](https://jmeter.apache.org/index.html). We test each scenario for a fixed duration of
time. We split the test results into warmup and measurement parts and use the measurement part to compute the
performance metrics.

Test scenarios use a [Netty](https://netty.io/) based back-end service which echoes back any request
posted to it after a specified period of time.

We run the performance tests under different numbers of concurrent users, message sizes (payloads) and back-end service
delays.

The main performance metrics:

1. **Throughput**: The number of requests that the WSO2 API Microgateway processes during a specific time interval (e.g. per second).
2. **Response Time**: The end-to-end latency for an operation of invoking an API. The complete distribution of response times was recorded.

In addition to the above metrics, we measure the load average and several memory-related metrics.

The following are the test parameters.

| Test Parameter | Description | Values |
| --- | --- | --- |
| Scenario Name | The name of the test scenario. | Refer to the above table. |
| Heap Size | The amount of memory allocated to the application | 512M |
| Concurrent Users | The number of users accessing the application at the same time. | 10, 50, 100, 200 |
| Message Size (Bytes) | The request payload size in Bytes. | 50, 1024, 10240 |
| Back-end Delay (ms) | The delay added by the back-end service. | 0 |

The duration of each test is **900 seconds**. The warm-up period is **300 seconds**.
The measurement results are collected after the warm-up period.

A [**c5.xlarge** Amazon EC2 instance](https://aws.amazon.com/ec2/instance-types/) was used to install WSO2 API Microgateway.
To limit the cpu utilization, docker `--cpus` option is provided as 1.

The jmeter is configured such that the maximum waiting time for receiving a response to be 20 seconds.

The following are the measurements collected from each performance test conducted for a given combination of
test parameters.

| Measurement | Description |
| --- | --- |
| Error % | Percentage of requests with errors |
| Average Response Time (ms) | The average response time of a set of results |
| Standard Deviation of Response Time (ms) | The “Standard Deviation” of the response time. |
| 99th Percentile of Response Time (ms) | 99% of the requests took no more than this time. The remaining samples took at least as long as this |
| Throughput (Requests/sec) | The throughput measured in requests per second. |
| Average Memory Footprint After Full GC (M) | The average memory consumed by the application after a full garbage collection event. |

The following is the summary of performance test results collected for the measurement period.

|  Scenario Name | Heap Size | Concurrent Users | Message Size (Bytes) | Back-end Service Delay (ms) | Error % | Throughput (Requests/sec) | Average Response Time (ms) | Standard Deviation of Response Time (ms) | 99th Percentile of Response Time (ms) | WSO2 API Microgateway GC Throughput (%) | Average WSO2 API Microgateway Memory Footprint After Full GC (M) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
|  Microgateway-Passthrough-JWT | 512M | 10 | 50 | 0 | 0 | 1227.87 | 8.11 | 37.46 | 73 | 98.89 | 21.354 |
|  Microgateway-Passthrough-JWT | 512M | 10 | 1024 | 0 | 0 | 1210.77 | 8.22 | 17.93 | 73 | 99.03 | 21.358 |
|  Microgateway-Passthrough-JWT | 512M | 10 | 10240 | 0 | 0 | 922.65 | 10.78 | 21.5 | 76 | 99.22 | 21.299 |
|  Microgateway-Passthrough-JWT | 512M | 50 | 50 | 0 | 0 | 1347.4 | 37.04 | 36.99 | 104 | 98.08 | 42.398 |
|  Microgateway-Passthrough-JWT | 512M | 50 | 1024 | 0 | 0 | 1286.02 | 38.81 | 37.22 | 104 | 98.32 | 41.319 |
|  Microgateway-Passthrough-JWT | 512M | 50 | 10240 | 0 | 0 | 982.06 | 50.83 | 37.46 | 101 | 98.38 | 37.688 |
|  Microgateway-Passthrough-JWT | 512M | 100 | 50 | 0 | 0 | 1345.89 | 74.22 | 44.5 | 193 | 97.1 | 54.69 |
|  Microgateway-Passthrough-JWT | 512M | 100 | 1024 | 0 | 0 | 1305.35 | 76.52 | 49.65 | 194 | 97.28 | 58.738 |
|  Microgateway-Passthrough-JWT | 512M | 100 | 10240 | 0 | 0 | 982.97 | 101.63 | 42.54 | 198 | 97.77 | 51.889 |
|  Microgateway-Passthrough-JWT | 512M | 200 | 50 | 0 | 0 | 1266.96 | 153.04 | 74.95 | 321 | 94.93 | 58.844 |
|  Microgateway-Passthrough-JWT | 512M | 200 | 1024 | 0 | 0 | 1280.73 | 156.04 | 72.56 | 331 | 95.06 | 64.305 |
|  Microgateway-Passthrough-JWT | 512M | 200 | 10240 | 0 | 0 | 982.94 | 203.41 | 63.14 | 387 | 95.86 | 58.063 |
