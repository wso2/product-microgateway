# WSO2 API Microgateway Performance Test Results

During each release, we execute various automated performance test scenarios and publish the results.

| Test Scenarios | Description |
| --- | --- |
| Microgateway-Passthrough-NoFilters | A secured API, which directly invokes the backend through Microgateway using OAuth2 tokens |

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
| Heap Size | The amount of memory allocated to the application | 256M |
| Concurrent Users | The number of users accessing the application at the same time. | 100, 200, 500 |
| Message Size (Bytes) | The request payload size in Bytes. | 50, 1024, 10240 |
| Back-end Delay (ms) | The delay added by the back-end service. | 0, 30 |

The duration of each test is **900 seconds**. The warm-up period is **300 seconds**.
The measurement results are collected after the warm-up period.

A [**c5.large** Amazon EC2 instance](https://aws.amazon.com/ec2/instance-types/) was used to install WSO2 API Microgateway.

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
|  Microgateway-Passthrough-NoFilters | 256M | 100 | 50 | 0 | 0 | 5957.06 | 16.75 | 10.63 | 54 | 94.32 | 27.335 |
|  Microgateway-Passthrough-NoFilters | 256M | 100 | 50 | 30 | 0 | 3068.41 | 32.55 | 2.8 | 43 | 97.11 | 20.273 |
|  Microgateway-Passthrough-NoFilters | 256M | 100 | 1024 | 0 | 0 | 5454.73 | 18.28 | 9.74 | 50 | 94.72 | 20.323 |
|  Microgateway-Passthrough-NoFilters | 256M | 100 | 1024 | 30 | 0 | 3051.49 | 32.73 | 2.75 | 43 | 97.07 | 20.304 |
|  Microgateway-Passthrough-NoFilters | 256M | 100 | 10240 | 0 | 0 | 2531.26 | 39.41 | 10.9 | 71 | 97.07 | 22.241 |
|  Microgateway-Passthrough-NoFilters | 256M | 100 | 10240 | 30 | 0 | 2307.7 | 43.25 | 6.64 | 63 | 97.23 | 20.303 |
|  Microgateway-Passthrough-NoFilters | 256M | 200 | 50 | 0 | 0 | 5730.62 | 34.83 | 21.83 | 111 | 89.51 | 34.4 |
|  Microgateway-Passthrough-NoFilters | 256M | 200 | 50 | 30 | 0 | 4778.53 | 41.79 | 8.82 | 70 | 91.82 | 24.065 |
|  Microgateway-Passthrough-NoFilters | 256M | 200 | 1024 | 0 | 0 | 5363.86 | 37.21 | 20.42 | 106 | 89.76 | 32.907 |
|  Microgateway-Passthrough-NoFilters | 256M | 200 | 1024 | 30 | 0 | 4569.47 | 43.7 | 9.14 | 72 | 91.92 | 20.265 |
|  Microgateway-Passthrough-NoFilters | 256M | 200 | 10240 | 0 | 0 | 2548.66 | 78.32 | 27.48 | 151 | 94.19 | 20.275 |
|  Microgateway-Passthrough-NoFilters | 256M | 200 | 10240 | 30 | 0 | 2413.89 | 82.73 | 16.89 | 128 | 94.58 | 20.261 |
|  Microgateway-Passthrough-NoFilters | 256M | 500 | 50 | 0 | 0 | 4221.7 | 118.28 | 54.12 | 335 | 72.51 | 54.285 |
|  Microgateway-Passthrough-NoFilters | 256M | 500 | 50 | 30 | 0 | 4240.83 | 117.75 | 39.45 | 293 | 73.66 | 54.102 |
|  Microgateway-Passthrough-NoFilters | 256M | 500 | 1024 | 0 | 0 | 4032.41 | 123.87 | 56.18 | 349 | 72.57 | 55.449 |
|  Microgateway-Passthrough-NoFilters | 256M | 500 | 1024 | 30 | 0 | 4102.04 | 121.75 | 41.13 | 303 | 73.11 | 55.259 |
|  Microgateway-Passthrough-NoFilters | 256M | 500 | 10240 | 0 | 0 | 2178.95 | 229.25 | 81.73 | 497 | 81.2 | 58.444 |
|  Microgateway-Passthrough-NoFilters | 256M | 500 | 10240 | 30 | 0 | 2168.52 | 230.4 | 72.21 | 479 | 81.35 | 58.152 |
