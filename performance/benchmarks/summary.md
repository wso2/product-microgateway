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
| Heap Size | The amount of memory allocated to the application | 1G |
| Concurrent Users | The number of users accessing the application at the same time. | 100, 200 |
| Message Size (Bytes) | The request payload size in Bytes. | 50, 1024 |
| Back-end Delay (ms) | The delay added by the back-end service. | 0 |

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
|  Microgateway-Passthrough-JWT | 1G | 100 | 50 | 0 | 0 | 1414.34 | 70.66 | 41.57 | 214 | 96.75 | 62.62 |
|  Microgateway-Passthrough-JWT | 1G | 100 | 1024 | 0 | 0 | 1426.84 | 70.03 | 39.33 | 206 | 96.69 | 60.649 |
|  Microgateway-Passthrough-JWT | 1G | 200 | 50 | 0 | 0 | 1435.99 | 139.23 | 64.31 | 345 | 95.52 | 67.067 |
|  Microgateway-Passthrough-JWT | 1G | 200 | 1024 | 0 | 0 | 1396.27 | 143.19 | 61.24 | 337 | 95.76 | 63.964 |
|  Microgateway-Passthrough-OAuth2 | 1G | 100 | 50 | 0 | 6.07 | 23.04 | 4338.41 | 15056.55 | 60927 | 98.44 | 25.967 |
|  Microgateway-Passthrough-OAuth2 | 1G | 100 | 1024 | 0 | 1 | 112.96 | 885.03 | 6205.03 | 60159 | 98.71 | 16.261 |
|  Microgateway-Passthrough-OAuth2 | 1G | 200 | 50 | 0 | 15.44 | 31.25 | 6314.25 | 17021.03 | 64767 | 99.22 | 16.445 |
|  Microgateway-Passthrough-OAuth2 | 1G | 200 | 1024 | 0 | 99.89 | 4.12 | 44893.63 | 26219.53 | 70143 | 99.77 | 16.457 |
