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
| Concurrent Users | The number of users accessing the application at the same time. | 50, 100, 200, 300, 500, 1000 |
| Message Size (Bytes) | The request payload size in Bytes. | 50, 1024, 10240 |
| Back-end Delay (ms) | The delay added by the back-end service. | 0, 30, 500, 1000 |

The duration of each test is **1200 seconds**. The warm-up period is **300 seconds**.
The measurement results are collected after the warm-up period.

A [**c5.xlarge** Amazon EC2 instance](https://aws.amazon.com/ec2/instance-types/) was used to install WSO2 API Microgateway.
And the microgateway was started in the **docker** environment with **--cpus = 2** condition.

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
|  Microgateway-Passthrough-JWT | 512M | 50 | 50 | 0 | 0 | 2581.86 | 19.29 | 19.44 | 65 | 98.64 | 17.648 |
|  Microgateway-Passthrough-JWT | 512M | 50 | 50 | 30 | 0 | 1593.92 | 31.3 | 0.92 | 34 | 99.28 | 10.321 |
|  Microgateway-Passthrough-JWT | 512M | 50 | 50 | 500 | 0 | 99.45 | 502.61 | 1.26 | 507 | 99.86 | 17.581 |
|  Microgateway-Passthrough-JWT | 512M | 50 | 50 | 1000 | 0 | 49.84 | 1002.14 | 0.83 | 1007 | 99.88 | 17.648 |
|  Microgateway-Passthrough-JWT | 512M | 50 | 1024 | 0 | 0 | 2520.69 | 19.76 | 19.81 | 66 | 98.63 | 17.583 |
|  Microgateway-Passthrough-JWT | 512M | 50 | 1024 | 30 | 0 | 1585.96 | 31.46 | 0.94 | 34 | 99.29 | 17.645 |
|  Microgateway-Passthrough-JWT | 512M | 50 | 1024 | 500 | 0 | 99.5 | 502.95 | 1.39 | 509 | 99.87 | 17.577 |
|  Microgateway-Passthrough-JWT | 512M | 50 | 1024 | 1000 | 0 | 49.79 | 1002.33 | 1.23 | 1007 | 99.89 | 17.649 |
|  Microgateway-Passthrough-JWT | 512M | 50 | 10240 | 0 | 0 | 1794.61 | 27.77 | 23.03 | 75 | 98.99 | 17.646 |
|  Microgateway-Passthrough-JWT | 512M | 50 | 10240 | 30 | 0 | 1557.26 | 32.02 | 1.07 | 35 | 99.23 | 17.645 |
|  Microgateway-Passthrough-JWT | 512M | 50 | 10240 | 500 | 0 | 99.42 | 503.27 | 1.22 | 507 | 99.86 | 17.646 |
|  Microgateway-Passthrough-JWT | 512M | 50 | 10240 | 1000 | 0 | 49.81 | 1002.82 | 1.66 | 1007 | 99.89 | 17.648 |
|  Microgateway-Passthrough-JWT | 512M | 100 | 50 | 0 | 0 | 2663.05 | 37.46 | 24.39 | 81 | 97.31 | 17.587 |
|  Microgateway-Passthrough-JWT | 512M | 100 | 50 | 30 | 0 | 2609.92 | 38.23 | 6.89 | 59 | 98.01 | 17.648 |
|  Microgateway-Passthrough-JWT | 512M | 100 | 50 | 500 | 0 | 199.05 | 502.74 | 1.61 | 509 | 99.72 | 17.578 |
|  Microgateway-Passthrough-JWT | 512M | 100 | 50 | 1000 | 0 | 99.7 | 1002.4 | 1.38 | 1007 | 99.77 | 17.648 |
|  Microgateway-Passthrough-JWT | 512M | 100 | 1024 | 0 | 0 | 2570.57 | 38.81 | 24.86 | 83 | 97.52 | 17.646 |
|  Microgateway-Passthrough-JWT | 512M | 100 | 1024 | 30 | 0 | 2500.12 | 39.91 | 7.51 | 61 | 98.03 | 17.648 |
|  Microgateway-Passthrough-JWT | 512M | 100 | 1024 | 500 | 0 | 198.93 | 502.99 | 1.63 | 509 | 99.7 | 17.649 |
|  Microgateway-Passthrough-JWT | 512M | 100 | 1024 | 1000 | 0 | 99.67 | 1002.5 | 1.51 | 1011 | 99.78 | 10.319 |
|  Microgateway-Passthrough-JWT | 512M | 100 | 10240 | 0 | 0 | 1757.11 | 56.81 | 28.73 | 105 | 98.2 | 17.648 |
|  Microgateway-Passthrough-JWT | 512M | 100 | 10240 | 30 | 0 | 1816.25 | 54.95 | 11.55 | 88 | 98.19 | 17.648 |
|  Microgateway-Passthrough-JWT | 512M | 100 | 10240 | 500 | 0 | 198.73 | 503.61 | 1.94 | 511 | 99.67 | 17.645 |
|  Microgateway-Passthrough-JWT | 512M | 100 | 10240 | 1000 | 0 | 99.69 | 1002.67 | 1.74 | 1011 | 99.74 | 17.622 |
|  Microgateway-Passthrough-JWT | 512M | 200 | 50 | 0 | 0 | 2632.28 | 75.87 | 23.97 | 110 | 95.71 | 17.648 |
|  Microgateway-Passthrough-JWT | 512M | 200 | 50 | 30 | 0 | 2671.76 | 74.73 | 21.72 | 111 | 95.97 | 17.646 |
|  Microgateway-Passthrough-JWT | 512M | 200 | 50 | 500 | 0 | 397.66 | 503.1 | 2.29 | 515 | 99.42 | 10.321 |
|  Microgateway-Passthrough-JWT | 512M | 200 | 50 | 1000 | 0 | 199.3 | 1003.01 | 2.44 | 1015 | 99.63 | 17.641 |
|  Microgateway-Passthrough-JWT | 512M | 200 | 1024 | 0 | 0 | 2529.93 | 78.95 | 24.43 | 118 | 95.67 | 17.646 |
|  Microgateway-Passthrough-JWT | 512M | 200 | 1024 | 30 | 0 | 2554.04 | 78.18 | 21.97 | 115 | 96.1 | 17.627 |
|  Microgateway-Passthrough-JWT | 512M | 200 | 1024 | 500 | 0 | 397.68 | 503.2 | 2.36 | 515 | 99.4 | 17.607 |
|  Microgateway-Passthrough-JWT | 512M | 200 | 1024 | 1000 | 0 | 199.44 | 1002.51 | 1.88 | 1011 | 99.59 | 17.587 |
|  Microgateway-Passthrough-JWT | 512M | 200 | 10240 | 0 | 0 | 1732.51 | 115.32 | 37.06 | 202 | 97.06 | 17.648 |
|  Microgateway-Passthrough-JWT | 512M | 200 | 10240 | 30 | 0 | 1773.3 | 112.66 | 25.28 | 189 | 96.68 | 17.646 |
|  Microgateway-Passthrough-JWT | 512M | 200 | 10240 | 500 | 0 | 397.22 | 503.86 | 2.64 | 515 | 99.34 | 17.646 |
|  Microgateway-Passthrough-JWT | 512M | 200 | 10240 | 1000 | 0 | 199.16 | 1003.24 | 2.64 | 1015 | 99.56 | 17.648 |
|  Microgateway-Passthrough-JWT | 512M | 300 | 50 | 0 | 0 | 2557.56 | 117.18 | 24.81 | 180 | 93.97 | 17.622 |
|  Microgateway-Passthrough-JWT | 512M | 300 | 50 | 30 | 0 | 2687.75 | 111.45 | 26.06 | 193 | 94.29 | 17.646 |
|  Microgateway-Passthrough-JWT | 512M | 300 | 50 | 500 | 0 | 596.63 | 502.92 | 2.82 | 515 | 98.9 | 17.646 |
|  Microgateway-Passthrough-JWT | 512M | 300 | 50 | 1000 | 0 | 298.96 | 1002.57 | 2.1 | 1015 | 99.37 | 17.648 |
|  Microgateway-Passthrough-JWT | 512M | 300 | 1024 | 0 | 0 | 2467.09 | 121.49 | 26.82 | 186 | 93.94 | 17.578 |
|  Microgateway-Passthrough-JWT | 512M | 300 | 1024 | 30 | 0 | 2562.28 | 116.94 | 29.68 | 197 | 94.48 | 17.622 |
|  Microgateway-Passthrough-JWT | 512M | 300 | 1024 | 500 | 0 | 596.58 | 502.91 | 2.62 | 515 | 98.95 | 17.587 |
|  Microgateway-Passthrough-JWT | 512M | 300 | 1024 | 1000 | 0 | 299.03 | 1002.65 | 2.22 | 1015 | 99.35 | 17.646 |
|  Microgateway-Passthrough-JWT | 512M | 300 | 10240 | 0 | 0 | 1721.68 | 174.15 | 48.75 | 297 | 95.56 | 17.644 |
|  Microgateway-Passthrough-JWT | 512M | 300 | 10240 | 30 | 0 | 1755.17 | 170.81 | 42.68 | 281 | 95.52 | 17.588 |
|  Microgateway-Passthrough-JWT | 512M | 300 | 10240 | 500 | 0 | 595.95 | 503.53 | 2.63 | 515 | 98.86 | 17.644 |
|  Microgateway-Passthrough-JWT | 512M | 300 | 10240 | 1000 | 0 | 298.78 | 1003.09 | 2.65 | 1015 | 99.31 | 17.588 |
|  Microgateway-Passthrough-JWT | 512M | 500 | 50 | 0 | 0 | 2434.88 | 205.22 | 30.1 | 303 | 88.73 | 69.153 |
|  Microgateway-Passthrough-JWT | 512M | 500 | 50 | 30 | 0 | 2550.27 | 195.92 | 31.76 | 295 | 86.07 | 67.134 |
|  Microgateway-Passthrough-JWT | 512M | 500 | 50 | 500 | 0 | 994.92 | 502.48 | 3.5 | 523 | 97.46 | 17.648 |
|  Microgateway-Passthrough-JWT | 512M | 500 | 50 | 1000 | 0 | 497.9 | 1003.59 | 5.31 | 1031 | 98.6 | 17.624 |
|  Microgateway-Passthrough-JWT | 512M | 500 | 1024 | 0 | 0 | 2336.83 | 213.88 | 32.66 | 313 | 88.76 | 69.491 |
|  Microgateway-Passthrough-JWT | 512M | 500 | 1024 | 30 | 0 | 2461.01 | 203.02 | 32.27 | 299 | 91.09 | 67.054 |
|  Microgateway-Passthrough-JWT | 512M | 500 | 1024 | 500 | 0 | 993.18 | 503.31 | 5.39 | 531 | 97.42 | 17.622 |
|  Microgateway-Passthrough-JWT | 512M | 500 | 1024 | 1000 | 0 | 497.95 | 1003.31 | 4.91 | 1031 | 98.61 | 17.624 |
|  Microgateway-Passthrough-JWT | 512M | 500 | 10240 | 0 | 0 | 1694.16 | 295.3 | 72.99 | 491 | 92.9 | 68.376 |
|  Microgateway-Passthrough-JWT | 512M | 500 | 10240 | 30 | 0 | 1723.26 | 290.29 | 62.44 | 465 | 92.76 | 68.123 |
|  Microgateway-Passthrough-JWT | 512M | 500 | 10240 | 500 | 0 | 993.21 | 503.6 | 3.57 | 523 | 97.19 | 17.648 |
|  Microgateway-Passthrough-JWT | 512M | 500 | 10240 | 1000 | 0 | 497.45 | 1003.75 | 4.4 | 1023 | 98.49 | 17.648 |
|  Microgateway-Passthrough-JWT | 512M | 1000 | 50 | 0 | 0 | 2214.54 | 451.69 | 66.76 | 655 | 81.45 | 118.932 |
|  Microgateway-Passthrough-JWT | 512M | 1000 | 50 | 30 | 0 | 2309.2 | 433.22 | 69.76 | 651 | 78.29 | 117.718 |
|  Microgateway-Passthrough-JWT | 512M | 1000 | 50 | 500 | 0 | 1879.53 | 531.94 | 34.95 | 663 | 85.58 | 108.833 |
|  Microgateway-Passthrough-JWT | 512M | 1000 | 50 | 1000 | 0 | 994.17 | 1004.64 | 9.58 | 1039 | 92.98 | 108.237 |
|  Microgateway-Passthrough-JWT | 512M | 1000 | 1024 | 0 | 0 | 2138.72 | 467.7 | 70.02 | 679 | 81.25 | 120.766 |
|  Microgateway-Passthrough-JWT | 512M | 1000 | 1024 | 30 | 0 | 2201.14 | 454.49 | 73.74 | 683 | 79.79 | 119.278 |
|  Microgateway-Passthrough-JWT | 512M | 1000 | 1024 | 500 | 0 | 1902.77 | 525.58 | 31.42 | 635 | 84.69 | 110.6 |
|  Microgateway-Passthrough-JWT | 512M | 1000 | 1024 | 1000 | 0 | 992.78 | 1006.01 | 17.04 | 1119 | 92.93 | 110.26 |
|  Microgateway-Passthrough-JWT | 512M | 1000 | 10240 | 0 | 0 | 1576.99 | 634.1 | 136.32 | 987 | 86.53 | 119.284 |
|  Microgateway-Passthrough-JWT | 512M | 1000 | 10240 | 30 | 0 | 1597.05 | 626.03 | 129.18 | 963 | 86.17 | 118.802 |
|  Microgateway-Passthrough-JWT | 512M | 1000 | 10240 | 500 | 0 | 1568.24 | 637.49 | 83.68 | 891 | 86.6 | 115.417 |
|  Microgateway-Passthrough-JWT | 512M | 1000 | 10240 | 1000 | 0 | 988.74 | 1010.17 | 18.99 | 1127 | 92.91 | 114.805 |
|  Microgateway-Passthrough-OAuth2 | 512M | 50 | 50 | 0 | 0 | 3050.84 | 16.32 | 18.24 | 63 | 98.46 | 16.359 |
|  Microgateway-Passthrough-OAuth2 | 512M | 50 | 50 | 30 | 0 | 1596.6 | 31.25 | 1.01 | 34 | 99.35 | 16.697 |
|  Microgateway-Passthrough-OAuth2 | 512M | 50 | 50 | 500 | 0 | 99.53 | 502.83 | 1.37 | 507 | 99.92 | 16.875 |
|  Microgateway-Passthrough-OAuth2 | 512M | 50 | 50 | 1000 | 0 | 49.85 | 1002.11 | 0.72 | 1007 | 99.94 | 17.185 |
|  Microgateway-Passthrough-OAuth2 | 512M | 50 | 1024 | 0 | 0 | 3074.64 | 16.2 | 18.2 | 63 | 98.41 | 16.452 |
|  Microgateway-Passthrough-OAuth2 | 512M | 50 | 1024 | 30 | 0 | 1597.42 | 31.24 | 0.9 | 33 | 99.33 | 16.573 |
|  Microgateway-Passthrough-OAuth2 | 512M | 50 | 1024 | 500 | 0 | 99.49 | 502.85 | 1.35 | 507 | 99.92 | 16.765 |
|  Microgateway-Passthrough-OAuth2 | 512M | 50 | 1024 | 1000 | 0 | 49.75 | 1002.22 | 0.96 | 1007 | 99.94 | 16.971 |
|  Microgateway-Passthrough-OAuth2 | 512M | 50 | 10240 | 0 | 0 | 1963.49 | 25.39 | 22.24 | 73 | 98.95 | 13.478 |
|  Microgateway-Passthrough-OAuth2 | 512M | 50 | 10240 | 30 | 0 | 1569.69 | 31.78 | 0.93 | 35 | 99.25 | 16.213 |
|  Microgateway-Passthrough-OAuth2 | 512M | 50 | 10240 | 500 | 0 | 99.43 | 503.14 | 1.34 | 509 | 99.91 | 16.773 |
|  Microgateway-Passthrough-OAuth2 | 512M | 50 | 10240 | 1000 | 0 | 49.83 | 1003.03 | 1.83 | 1007 | 99.93 | 17.213 |
|  Microgateway-Passthrough-OAuth2 | 512M | 100 | 50 | 0 | 0 | 3184.79 | 31.32 | 23.25 | 76 | 97.32 | 17.859 |
|  Microgateway-Passthrough-OAuth2 | 512M | 100 | 50 | 30 | 0 | 3109.49 | 32.09 | 1.8 | 38 | 98.25 | 16.82 |
|  Microgateway-Passthrough-OAuth2 | 512M | 100 | 50 | 500 | 0 | 198.96 | 502.79 | 1.77 | 509 | 99.83 | 17.167 |
|  Microgateway-Passthrough-OAuth2 | 512M | 100 | 50 | 1000 | 0 | 99.72 | 1002.37 | 1.41 | 1011 | 99.89 | 17.785 |
|  Microgateway-Passthrough-OAuth2 | 512M | 100 | 1024 | 0 | 0 | 3207.55 | 31.1 | 23.15 | 76 | 97.42 | 17.842 |
|  Microgateway-Passthrough-OAuth2 | 512M | 100 | 1024 | 30 | 0 | 3026.4 | 32.97 | 2.45 | 42 | 98.19 | 17.077 |
|  Microgateway-Passthrough-OAuth2 | 512M | 100 | 1024 | 500 | 0 | 199 | 502.78 | 1.72 | 509 | 99.84 | 17.223 |
|  Microgateway-Passthrough-OAuth2 | 512M | 100 | 1024 | 1000 | 0 | 99.65 | 1002.32 | 1.25 | 1007 | 99.88 | 18.486 |
|  Microgateway-Passthrough-OAuth2 | 512M | 100 | 10240 | 0 | 0 | 1935.55 | 51.58 | 28.06 | 100 | 98.3 | 17.575 |
|  Microgateway-Passthrough-OAuth2 | 512M | 100 | 10240 | 30 | 0 | 2043.68 | 48.84 | 8.08 | 71 | 98.31 | 16.795 |
|  Microgateway-Passthrough-OAuth2 | 512M | 100 | 10240 | 500 | 0 | 198.79 | 503.26 | 1.72 | 509 | 99.82 | 17.237 |
|  Microgateway-Passthrough-OAuth2 | 512M | 100 | 10240 | 1000 | 0 | 99.63 | 1003.21 | 2.17 | 1011 | 99.88 | 17.955 |
|  Microgateway-Passthrough-OAuth2 | 512M | 200 | 50 | 0 | 0 | 3060.03 | 65.27 | 25.76 | 104 | 95.66 | 20.901 |
|  Microgateway-Passthrough-OAuth2 | 512M | 200 | 50 | 30 | 0 | 3263.12 | 61.2 | 16.28 | 99 | 96.28 | 18.579 |
|  Microgateway-Passthrough-OAuth2 | 512M | 200 | 50 | 500 | 0 | 398.02 | 502.67 | 2.38 | 515 | 99.57 | 18.17 |
|  Microgateway-Passthrough-OAuth2 | 512M | 200 | 50 | 1000 | 0 | 199.35 | 1002.5 | 1.9 | 1011 | 99.74 | 19.178 |
|  Microgateway-Passthrough-OAuth2 | 512M | 200 | 1024 | 0 | 0 | 3111.89 | 64.18 | 25.87 | 104 | 95.74 | 19.599 |
|  Microgateway-Passthrough-OAuth2 | 512M | 200 | 1024 | 30 | 0 | 3245.87 | 61.52 | 16.29 | 98 | 96.3 | 19.129 |
|  Microgateway-Passthrough-OAuth2 | 512M | 200 | 1024 | 500 | 0 | 397.78 | 502.91 | 2.54 | 515 | 99.56 | 18.254 |
|  Microgateway-Passthrough-OAuth2 | 512M | 200 | 1024 | 1000 | 0 | 199.4 | 1002.54 | 1.79 | 1011 | 99.74 | 18.792 |
|  Microgateway-Passthrough-OAuth2 | 512M | 200 | 10240 | 0 | 0 | 1908.1 | 104.7 | 35.95 | 195 | 97.16 | 19.728 |
|  Microgateway-Passthrough-OAuth2 | 512M | 200 | 10240 | 30 | 0 | 1970.83 | 101.37 | 18.53 | 166 | 96.56 | 19.782 |
|  Microgateway-Passthrough-OAuth2 | 512M | 200 | 10240 | 500 | 0 | 397.71 | 503.16 | 1.97 | 511 | 99.52 | 18.646 |
|  Microgateway-Passthrough-OAuth2 | 512M | 200 | 10240 | 1000 | 0 | 199.26 | 1003.24 | 2.44 | 1015 | 99.71 | 18.931 |
|  Microgateway-Passthrough-OAuth2 | 512M | 300 | 50 | 0 | 0 | 3035.66 | 98.73 | 23.66 | 168 | 94.03 | 19.314 |
|  Microgateway-Passthrough-OAuth2 | 512M | 300 | 50 | 30 | 0 | 3168.25 | 94.58 | 19.82 | 155 | 93.61 | 21.786 |
|  Microgateway-Passthrough-OAuth2 | 512M | 300 | 50 | 500 | 0 | 597.23 | 502.53 | 2.68 | 515 | 99.15 | 18.858 |
|  Microgateway-Passthrough-OAuth2 | 512M | 300 | 50 | 1000 | 0 | 299.08 | 1002.42 | 1.96 | 1015 | 99.51 | 19.513 |
|  Microgateway-Passthrough-OAuth2 | 512M | 300 | 1024 | 0 | 0 | 3075.74 | 97.44 | 25.36 | 168 | 93.11 | 21.336 |
|  Microgateway-Passthrough-OAuth2 | 512M | 300 | 1024 | 30 | 0 | 3107.05 | 96.44 | 19.66 | 156 | 93.3 | 20.335 |
|  Microgateway-Passthrough-OAuth2 | 512M | 300 | 1024 | 500 | 0 | 597.12 | 502.53 | 2.76 | 515 | 99.16 | 19.164 |
|  Microgateway-Passthrough-OAuth2 | 512M | 300 | 1024 | 1000 | 0 | 299.03 | 1002.59 | 9.48 | 1015 | 99.52 | 19.236 |
|  Microgateway-Passthrough-OAuth2 | 512M | 300 | 10240 | 0 | 0 | 1893.6 | 158.32 | 46.7 | 285 | 95.89 | 21.33 |
|  Microgateway-Passthrough-OAuth2 | 512M | 300 | 10240 | 30 | 0 | 1972.37 | 151.97 | 39.58 | 261 | 95.67 | 20.927 |
|  Microgateway-Passthrough-OAuth2 | 512M | 300 | 10240 | 500 | 0 | 596.53 | 503.25 | 2.35 | 515 | 99.07 | 18.832 |
|  Microgateway-Passthrough-OAuth2 | 512M | 300 | 10240 | 1000 | 0 | 298.94 | 1002.48 | 1.82 | 1011 | 99.48 | 19.941 |
|  Microgateway-Passthrough-OAuth2 | 512M | 500 | 50 | 0 | 0 | 2963.27 | 168.54 | 33.22 | 269 | 86.71 | 71.484 |
|  Microgateway-Passthrough-OAuth2 | 512M | 500 | 50 | 30 | 0 | 3075.91 | 162.42 | 39.92 | 227 | 88.06 | 74.12 |
|  Microgateway-Passthrough-OAuth2 | 512M | 500 | 50 | 500 | 0 | 995.69 | 501.98 | 2.55 | 515 | 97.9 | 23.68 |
|  Microgateway-Passthrough-OAuth2 | 512M | 500 | 50 | 1000 | 0 | 498.52 | 1002.58 | 2.93 | 1019 | 98.82 | 21.314 |
|  Microgateway-Passthrough-OAuth2 | 512M | 500 | 1024 | 0 | 0 | 2918 | 171.23 | 31.54 | 277 | 86.51 | 72.571 |
|  Microgateway-Passthrough-OAuth2 | 512M | 500 | 1024 | 30 | 0 | 3106.95 | 160.8 | 39.34 | 230 | 88.7 | 74.971 |
|  Microgateway-Passthrough-OAuth2 | 512M | 500 | 1024 | 500 | 0 | 995.55 | 502.13 | 2.49 | 515 | 97.88 | 21.963 |
|  Microgateway-Passthrough-OAuth2 | 512M | 500 | 1024 | 1000 | 0 | 498.6 | 1002.53 | 2.56 | 1019 | 98.76 | 22.69 |
|  Microgateway-Passthrough-OAuth2 | 512M | 500 | 10240 | 0 | 0 | 1899.48 | 263.33 | 68.95 | 451 | 92.54 | 80.969 |
|  Microgateway-Passthrough-OAuth2 | 512M | 500 | 10240 | 30 | 0 | 1925.78 | 259.73 | 57.45 | 405 | 92.52 | 80.864 |
|  Microgateway-Passthrough-OAuth2 | 512M | 500 | 10240 | 500 | 0 | 994.2 | 503.01 | 2.84 | 519 | 97.56 | 24.035 |
|  Microgateway-Passthrough-OAuth2 | 512M | 500 | 10240 | 1000 | 0 | 497.96 | 1003.11 | 3.65 | 1023 | 98.65 | 21.12 |
|  Microgateway-Passthrough-OAuth2 | 512M | 1000 | 50 | 0 | 58.51 | 39.31 | 23997.9 | 31066.81 | 122879 | 98.1 | 164.292 |
|  Microgateway-Passthrough-OAuth2 | 512M | 1000 | 50 | 30 | 100 | 20.81 | 44304.07 | 35890.06 | 127487 | 98.32 | 148.129 |
|  Microgateway-Passthrough-OAuth2 | 512M | 1000 | 50 | 500 | 2.48 | 1935.31 | 516.59 | 86.37 | 659 | 88.44 | 112.992 |
|  Microgateway-Passthrough-OAuth2 | 512M | 1000 | 50 | 1000 | 30.46 | 81.59 | 11363.52 | 22594.68 | 110079 | 97.64 | 153.755 |
|  Microgateway-Passthrough-OAuth2 | 512M | 1000 | 1024 | 0 | 59.76 | 38.86 | 23437.11 | 30206.76 | 118271 | 98.1 | 165.274 |
|  Microgateway-Passthrough-OAuth2 | 512M | 1000 | 1024 | 30 | 99.76 | 20.47 | 43491.24 | 32416.41 | 128511 | 98.29 | 147.06 |
|  Microgateway-Passthrough-OAuth2 | 512M | 1000 | 1024 | 500 | 0 | 1888.16 | 529.56 | 33.34 | 663 | 87.21 | 116.228 |
|  Microgateway-Passthrough-OAuth2 | 512M | 1000 | 1024 | 1000 | 53.31 | 46.95 | 19891.08 | 28557.61 | 114175 | 97.93 | 164.953 |
|  Microgateway-Passthrough-OAuth2 | 512M | 1000 | 10240 | 0 | 99.6 | 22.41 | 42393.93 | 33360.64 | 130047 | 98.26 | 152.321 |
|  Microgateway-Passthrough-OAuth2 | 512M | 1000 | 10240 | 30 | 61.21 | 40.12 | 22551.29 | 29843.36 | 117247 | 97.92 | 165.495 |
|  Microgateway-Passthrough-OAuth2 | 512M | 1000 | 10240 | 500 | 2.12 | 1695.22 | 589.77 | 108.93 | 835 | 87.17 | 118.226 |
|  Microgateway-Passthrough-OAuth2 | 512M | 1000 | 10240 | 1000 | 59.74 | 37.81 | 24152.85 | 30471.8 | 121343 | 98.03 | 169.328 |
