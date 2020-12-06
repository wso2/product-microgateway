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

A [**c5.large** Amazon EC2 instance](https://aws.amazon.com/ec2/instance-types/) was used to install WSO2 API Microgateway.
To limit the cpu utilization, docker `--cpus` option is provided as 0.5.

The jmeter is configured such that the maximum waiting time for receiving a response to be 20 seconds.

The following figures shows how the Throughput changes for different number of concurrent users with different backend delays
![picture](plots/thrpt_0ms.png)

The following figures shows how the Average Response Time changes for different number of concurrent users with different backend delays.
![picture](plots/avgt_0ms.png)

Let’s look at the 90th, 95th, and 99th Response Time percentiles. 
This is useful to measure the percentage of requests that exceeded the response time value for a given percentile. 
A percentile can also tell the percentage of requests completed below the particular response time value.
![picture](plots/response_time_0ms.png)

The GC Throughput was calculated for each test to check whether GC operations are not impacting the performance of the server. 
The GC Throughput is the time percentage of the application, which was not busy with GC operations.
![picture](plots/gc_0ms.png)

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
|  Microgateway-Passthrough-JWT | 512M | 10 | 50 | 0 | 0 | 487.92 | 20.44 | 31.09 | 90 | 99.34 | 21.325 |
|  Microgateway-Passthrough-JWT | 512M | 10 | 1024 | 0 | 0 | 471.96 | 21.12 | 49.06 | 91 | 99.39 | 21.328 |
|  Microgateway-Passthrough-JWT | 512M | 10 | 10240 | 0 | 0 | 377.22 | 26.45 | 68.32 | 90 | 99.41 | 21.322 |
|  Microgateway-Passthrough-JWT | 512M | 50 | 50 | 0 | 0 | 543.78 | 91.87 | 72.49 | 283 | 98.26 | 33.253 |
|  Microgateway-Passthrough-JWT | 512M | 50 | 1024 | 0 | 0 | 530.6 | 94.16 | 64.06 | 285 | 98.11 | 38.104 |
|  Microgateway-Passthrough-JWT | 512M | 50 | 10240 | 0 | 0 | 417.35 | 119.71 | 70.88 | 295 | 98.46 | 40.353 |
|  Microgateway-Passthrough-JWT | 512M | 100 | 50 | 0 | 0 | 539.62 | 185.24 | 106.83 | 487 | 97.56 | 62.383 |
|  Microgateway-Passthrough-JWT | 512M | 100 | 1024 | 0 | 0 | 541.57 | 184.58 | 100.7 | 487 | 97.49 | 53.111 |
|  Microgateway-Passthrough-JWT | 512M | 100 | 10240 | 0 | 0 | 417.18 | 239.7 | 103.1 | 519 | 97.76 | 37.132 |
|  Microgateway-Passthrough-JWT | 512M | 200 | 50 | 0 | 0 | 543.9 | 367.62 | 168.89 | 819 | 96.19 | 53.658 |
|  Microgateway-Passthrough-JWT | 512M | 200 | 1024 | 0 | 0 | 504.74 | 396.15 | 175.08 | 899 | 96.31 | 62.377 |
|  Microgateway-Passthrough-JWT | 512M | 200 | 10240 | 0 | 0 | 392.81 | 508.94 | 190.29 | 1023 | 97.4 | 59.947 |
