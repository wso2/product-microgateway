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
| Heap Size | The amount of memory allocated to the application | 8G |
| Concurrent Users | The number of users accessing the application at the same time. | 100, 200, 500 |
| Message Size (Bytes) | The request payload size in Bytes. | 1024 |
| Back-end Delay (ms) | The delay added by the back-end service. | 0 |

The duration of each test is **600 seconds**. The warm-up period is **300 seconds**.
The measurement results are collected after the warm-up period.

A [**m5.2xlarge** Amazon EC2 instance](https://aws.amazon.com/ec2/instance-types/) was used to install WSO2 API Microgateway. The microgateway has been deployed inside a docker container with the option "--cpus=8".

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
|  Microgateway-Passthrough-JWT | 8G | 100 | 1024 | 0 | 0 | 5839.36 | 17.07 | 20.95 | 102 | 94.76 | 191.883 |
|  Microgateway-Passthrough-JWT | 8G | 200 | 1024 | 0 | 0 | 5970.54 | 33.43 | 28.42 | 131 | 94.4 | 360.325 |
|  Microgateway-Passthrough-JWT | 8G | 500 | 1024 | 0 | 33.06 | 10.16 | 39691.74 | 56479.87 | 120319 | 99.95 | 30.172 |
|  Microgateway-Passthrough-OAuth2 | 8G | 100 | 1024 | 0 | 0 | 6879.74 | 14.49 | 17.28 | 82 | 95.49 | 236.78 |
|  Microgateway-Passthrough-OAuth2 | 8G | 200 | 1024 | 0 | 0 | 7025.04 | 28.41 | 18.05 | 96 | 95.46 | 165.034 |
|  Microgateway-Passthrough-OAuth2 | 8G | 500 | 1024 | 0 | 99.97 | 15.96 | 24713.82 | 24443.82 | 70143 | 99.86 | 30.173 |
