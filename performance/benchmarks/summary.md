# WSO2 API Microgateway Performance Test Results

During each release, we execute various automated performance test scenarios and publish the results.

| Test Scenarios | Description |
| --- | --- |
| Microgateway-Passthrough-OAuth2 | A secured API, which directly invokes the backend through Microgateway using OAuth2 tokens |
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
| Concurrent Users | The number of users accessing the application at the same time. | 100, 200 |
| Message Size (Bytes) | The request payload size in Bytes. | 50, 1024 |
| Back-end Delay (ms) | The delay added by the back-end service. | 0 |

The duration of each test is **1200 seconds**. The warm-up period is **300 seconds**.
The measurement results are collected after the warm-up period.

A [**c5.xlarge** Amazon EC2 instance](https://aws.amazon.com/ec2/instance-types/) was used to install WSO2 API Microgateway.

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
|  Microgateway-Passthrough-JWT | 512M | 100 | 50 | 0 | 0 | 2799.53 | 35.66 | 24.1 | 79 | 97.4 | 17.577 |
|  Microgateway-Passthrough-JWT | 512M | 100 | 1024 | 0 | 0 | 2730.95 | 36.55 | 24.36 | 81 | 97.51 | 17.588 |
|  Microgateway-Passthrough-JWT | 512M | 200 | 50 | 0 | 0 | 2729.9 | 73.19 | 24.05 | 107 | 95.69 | 17.646 |
|  Microgateway-Passthrough-JWT | 512M | 200 | 1024 | 0 | 0 | 2609.5 | 76.57 | 24.32 | 113 | 95.46 | 17.621 |
|  Microgateway-Passthrough-OAuth2 | 512M | 100 | 50 | 0 | 0 | 3374.33 | 29.57 | 22.93 | 75 | 97.56 | 19.555 |
|  Microgateway-Passthrough-OAuth2 | 512M | 100 | 1024 | 0 | 0 | 3346.33 | 29.83 | 22.7 | 75 | 97.57 | 20.522 |
|  Microgateway-Passthrough-OAuth2 | 512M | 200 | 50 | 0 | 0 | 3360.17 | 59.46 | 26.18 | 100 | 94.94 | 23.504 |
|  Microgateway-Passthrough-OAuth2 | 512M | 200 | 1024 | 0 | 0 | 3297.49 | 60.58 | 26.14 | 101 | 94.79 | 20.683 |
