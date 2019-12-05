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
| Heap Size | The amount of memory allocated to the application | 4G |
| Concurrent Users | The number of users accessing the application at the same time. | 50, 100, 200, 300, 500, 1000 |
| Message Size (Bytes) | The request payload size in Bytes. | 50, 1024, 10240 |
| Back-end Delay (ms) | The delay added by the back-end service. | 0, 30, 500, 1000 |

The duration of each test is **1200 seconds**. The warm-up period is **300 seconds**.
The measurement results are collected after the warm-up period.

A [**m5.xlarge** Amazon EC2 instance](https://aws.amazon.com/ec2/instance-types/) was used to install WSO2 API Microgateway. The microgateway has been deployed in a docker container with the option "--cpus=2". 

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
|  Microgateway-Passthrough-JWT | 4G | 50 | 50 | 0 | 0 | 1746.53 | 28.53 | 28.56 | 114 | 94.86 | 25.568 |
|  Microgateway-Passthrough-JWT | 4G | 50 | 50 | 30 | 0 | 1510.05 | 33.03 | 7.42 | 81 | 96.36 | 25.567 |
|  Microgateway-Passthrough-JWT | 4G | 50 | 50 | 500 | 0 | 99.47 | 503.04 | 4.11 | 515 | 99.92 | 25.567 |
|  Microgateway-Passthrough-JWT | 4G | 50 | 50 | 1000 | 0 | 49.84 | 1002.25 | 3.58 | 1007 | 99.95 | 25.566 |
|  Microgateway-Passthrough-JWT | 4G | 50 | 1024 | 0 | 0 | 1696.28 | 29.38 | 29.03 | 117 | 94.79 | 25.566 |
|  Microgateway-Passthrough-JWT | 4G | 50 | 1024 | 30 | 0 | 1505.02 | 33.14 | 7.47 | 81 | 96.37 | 25.565 |
|  Microgateway-Passthrough-JWT | 4G | 50 | 1024 | 500 | 0 | 99.49 | 502.94 | 3.12 | 511 | 99.92 | 25.566 |
|  Microgateway-Passthrough-JWT | 4G | 50 | 1024 | 1000 | 0 | 49.84 | 1002.2 | 2.74 | 1007 | 99.95 | 25.564 |
|  Microgateway-Passthrough-JWT | 4G | 50 | 10240 | 0 | 0 | 1263.58 | 39.45 | 30.46 | 123 | 95.91 | 25.565 |
|  Microgateway-Passthrough-JWT | 4G | 50 | 10240 | 30 | 0 | 1271.01 | 39.23 | 10.27 | 93 | 96.74 | 25.568 |
|  Microgateway-Passthrough-JWT | 4G | 50 | 10240 | 500 | 0 | 99.29 | 503.41 | 3.76 | 515 | 99.9 | 25.564 |
|  Microgateway-Passthrough-JWT | 4G | 50 | 10240 | 1000 | 0 | 49.81 | 1002.45 | 3.35 | 1007 | 99.95 | 25.563 |
|  Microgateway-Passthrough-JWT | 4G | 100 | 50 | 0 | 0 | 1770.56 | 56.38 | 39.79 | 183 | 93.75 | 25.563 |
|  Microgateway-Passthrough-JWT | 4G | 100 | 50 | 30 | 0 | 1832.58 | 54.46 | 17.68 | 122 | 95.1 | 25.572 |
|  Microgateway-Passthrough-JWT | 4G | 100 | 50 | 500 | 0 | 198.88 | 502.96 | 4.59 | 519 | 99.8 | 25.571 |
|  Microgateway-Passthrough-JWT | 4G | 100 | 50 | 1000 | 0 | 99.67 | 1002.28 | 2.73 | 1007 | 99.9 | 25.568 |
|  Microgateway-Passthrough-JWT | 4G | 100 | 1024 | 0 | 0 | 1723.16 | 57.92 | 39.13 | 182 | 94.6 | 25.564 |
|  Microgateway-Passthrough-JWT | 4G | 100 | 1024 | 30 | 0 | 1793.18 | 55.65 | 18.6 | 128 | 94.87 | 25.564 |
|  Microgateway-Passthrough-JWT | 4G | 100 | 1024 | 500 | 0 | 198.88 | 503.15 | 4.07 | 519 | 99.81 | 25.566 |
|  Microgateway-Passthrough-JWT | 4G | 100 | 1024 | 1000 | 0 | 99.65 | 1002.41 | 3.75 | 1011 | 99.91 | 25.566 |
|  Microgateway-Passthrough-JWT | 4G | 100 | 10240 | 0 | 0 | 1265.51 | 78.87 | 40.89 | 196 | 95.83 | 25.565 |
|  Microgateway-Passthrough-JWT | 4G | 100 | 10240 | 30 | 0 | 1295.9 | 77.01 | 25.53 | 157 | 95.96 | 25.564 |
|  Microgateway-Passthrough-JWT | 4G | 100 | 10240 | 500 | 0 | 198.6 | 503.63 | 2.86 | 519 | 99.79 | 25.567 |
|  Microgateway-Passthrough-JWT | 4G | 100 | 10240 | 1000 | 0 | 99.56 | 1003.03 | 4.26 | 1019 | 99.9 | 25.571 |
|  Microgateway-Passthrough-JWT | 4G | 200 | 50 | 0 | 0 | 1802.08 | 110.85 | 51.93 | 281 | 94.89 | 25.568 |
|  Microgateway-Passthrough-JWT | 4G | 200 | 50 | 30 | 0 | 1832.69 | 108.99 | 32.79 | 210 | 94.65 | 25.566 |
|  Microgateway-Passthrough-JWT | 4G | 200 | 50 | 500 | 0 | 397.61 | 503.18 | 4.18 | 523 | 99.38 | 25.572 |
|  Microgateway-Passthrough-JWT | 4G | 200 | 50 | 1000 | 0 | 199.26 | 1003.12 | 4.38 | 1023 | 99.75 | 25.567 |
|  Microgateway-Passthrough-JWT | 4G | 200 | 1024 | 0 | 0 | 1701.2 | 117.44 | 55.62 | 293 | 94.51 | 25.563 |
|  Microgateway-Passthrough-JWT | 4G | 200 | 1024 | 30 | 0 | 1695.08 | 117.85 | 38.74 | 232 | 93.83 | 25.564 |
|  Microgateway-Passthrough-JWT | 4G | 200 | 1024 | 500 | 0 | 397.31 | 503.39 | 4.87 | 527 | 99.38 | 25.565 |
|  Microgateway-Passthrough-JWT | 4G | 200 | 1024 | 1000 | 0 | 199.06 | 1003.24 | 4.84 | 1031 | 99.75 | 25.567 |
|  Microgateway-Passthrough-JWT | 4G | 200 | 10240 | 0 | 0 | 1266.82 | 157.72 | 67.19 | 371 | 95.42 | 25.565 |
|  Microgateway-Passthrough-JWT | 4G | 200 | 10240 | 30 | 0 | 1262.6 | 158.26 | 53.04 | 305 | 95.45 | 25.565 |
|  Microgateway-Passthrough-JWT | 4G | 200 | 10240 | 500 | 0 | 396.85 | 504.13 | 4.99 | 531 | 99.37 | 25.564 |
|  Microgateway-Passthrough-JWT | 4G | 200 | 10240 | 1000 | 0 | 199.17 | 1003.71 | 5.28 | 1031 | 99.74 | 25.565 |
|  Microgateway-Passthrough-JWT | 4G | 300 | 50 | 0 | 0 | 1741.42 | 172.15 | 71.97 | 397 | 94.67 | 25.568 |
|  Microgateway-Passthrough-JWT | 4G | 300 | 50 | 30 | 0 | 1789.97 | 167.46 | 60.6 | 333 | 94.77 | 104.415 |
|  Microgateway-Passthrough-JWT | 4G | 300 | 50 | 500 | 0 | 596.11 | 503.3 | 5.55 | 535 | 98.75 | 25.569 |
|  Microgateway-Passthrough-JWT | 4G | 300 | 50 | 1000 | 0 | 298.71 | 1003.08 | 4.95 | 1031 | 99.54 | 25.569 |
|  Microgateway-Passthrough-JWT | 4G | 300 | 1024 | 0 | 0 | 1690.52 | 177.35 | 73.42 | 399 | 94.54 | 25.564 |
|  Microgateway-Passthrough-JWT | 4G | 300 | 1024 | 30 | 0 | 1706.47 | 175.68 | 61.68 | 367 | 94.72 | 25.567 |
|  Microgateway-Passthrough-JWT | 4G | 300 | 1024 | 500 | 0 | 596.15 | 503.33 | 5.68 | 535 | 98.77 | 25.565 |
|  Microgateway-Passthrough-JWT | 4G | 300 | 1024 | 1000 | 0 | 298.89 | 1003 | 4.86 | 1031 | 99.57 | 25.572 |
|  Microgateway-Passthrough-JWT | 4G | 300 | 10240 | 0 | 0 | 1227.94 | 244.26 | 95.57 | 519 | 95.42 | 25.567 |
|  Microgateway-Passthrough-JWT | 4G | 300 | 10240 | 30 | 0 | 1251.49 | 239.66 | 79.61 | 481 | 95.27 | 25.573 |
|  Microgateway-Passthrough-JWT | 4G | 300 | 10240 | 500 | 0 | 594.8 | 504.46 | 6.62 | 543 | 98.64 | 25.571 |
|  Microgateway-Passthrough-JWT | 4G | 300 | 10240 | 1000 | 0 | 298.79 | 1002.72 | 3.85 | 1019 | 99.51 | 25.57 |
|  Microgateway-Passthrough-JWT | 4G | 500 | 50 | 0 | 0 | 1696.83 | 294.71 | 105.88 | 607 | 94.55 | 25.568 |
|  Microgateway-Passthrough-JWT | 4G | 500 | 50 | 30 | 0 | 1750.97 | 285.6 | 92.13 | 571 | 94.28 | 107.852 |
|  Microgateway-Passthrough-JWT | 4G | 500 | 50 | 500 | 0 | 986.72 | 506.72 | 12.68 | 571 | 97.11 | 25.567 |
|  Microgateway-Passthrough-JWT | 4G | 500 | 50 | 1000 | 0 | 496.26 | 1006.64 | 11.23 | 1063 | 98.85 | 25.571 |
|  Microgateway-Passthrough-JWT | 4G | 500 | 1024 | 0 | 0 | 1696.51 | 294.78 | 105.19 | 607 | 94.31 | 25.571 |
|  Microgateway-Passthrough-JWT | 4G | 500 | 1024 | 30 | 0 | 1650.15 | 303.04 | 99.41 | 599 | 94.52 | 25.564 |
|  Microgateway-Passthrough-JWT | 4G | 500 | 1024 | 500 | 0 | 988.37 | 505.93 | 11.26 | 567 | 97.08 | 25.57 |
|  Microgateway-Passthrough-JWT | 4G | 500 | 1024 | 1000 | 0 | 495.67 | 1007.33 | 12.22 | 1063 | 98.87 | 25.565 |
|  Microgateway-Passthrough-JWT | 4G | 500 | 10240 | 0 | 0 | 1226.36 | 407.8 | 148.47 | 851 | 95.09 | 25.567 |
|  Microgateway-Passthrough-JWT | 4G | 500 | 10240 | 30 | 0 | 1241.21 | 402.9 | 131.06 | 791 | 94.93 | 25.569 |
|  Microgateway-Passthrough-JWT | 4G | 500 | 10240 | 500 | 0 | 987.42 | 506.47 | 10.78 | 567 | 96.93 | 25.57 |
|  Microgateway-Passthrough-JWT | 4G | 500 | 10240 | 1000 | 0 | 497.5 | 1003.43 | 5.8 | 1039 | 98.81 | 25.564 |
|  Microgateway-Passthrough-JWT | 4G | 1000 | 50 | 0 | 0 | 1642.5 | 608.76 | 186.85 | 1143 | 92.82 | 154.833 |
|  Microgateway-Passthrough-JWT | 4G | 1000 | 50 | 30 | 0 | 1677.65 | 596.01 | 167.92 | 1087 | 92.69 | 139.019 |
|  Microgateway-Passthrough-JWT | 4G | 1000 | 50 | 500 | 0 | 1651.79 | 605.06 | 58.17 | 771 | 93.97 | 153.324 |
|  Microgateway-Passthrough-JWT | 4G | 1000 | 50 | 1000 | 0 | 993.33 | 1005.48 | 12.93 | 1087 | 95.88 | 25.569 |
|  Microgateway-Passthrough-JWT | 4G | 1000 | 1024 | 0 | 0 | 1586.55 | 630.32 | 202.29 | 1199 | 93.31 | 25.566 |
|  Microgateway-Passthrough-JWT | 4G | 1000 | 1024 | 30 | 0 | 1628.77 | 613.61 | 177.77 | 1111 | 93.08 | 25.572 |
|  Microgateway-Passthrough-JWT | 4G | 1000 | 1024 | 500 | 0 | 1629.92 | 613.37 | 57.18 | 787 | 94 | 25.569 |
|  Microgateway-Passthrough-JWT | 4G | 1000 | 1024 | 1000 | 0 | 993.21 | 1005.35 | 12.7 | 1087 | 96.03 | 25.565 |
|  Microgateway-Passthrough-JWT | 4G | 1000 | 10240 | 0 | 0 | 1207.92 | 827.37 | 270.8 | 1599 | 94.69 | 25.565 |
|  Microgateway-Passthrough-JWT | 4G | 1000 | 10240 | 30 | 0 | 1235.17 | 808.8 | 245.43 | 1495 | 94.69 | 25.57 |
|  Microgateway-Passthrough-JWT | 4G | 1000 | 10240 | 500 | 0 | 1222.59 | 817.46 | 127.03 | 1167 | 94.9 | 25.567 |
|  Microgateway-Passthrough-JWT | 4G | 1000 | 10240 | 1000 | 0 | 987.02 | 1011.92 | 18.37 | 1095 | 95.77 | 25.567 |
|  Microgateway-Passthrough-OAuth2 | 4G | 50 | 50 | 0 | 0 | 1950.05 | 25.56 | 27.23 | 109 | 94.88 | 25.567 |
|  Microgateway-Passthrough-OAuth2 | 4G | 50 | 50 | 30 | 0 | 1516.34 | 32.9 | 8.21 | 89 | 96.34 | 25.565 |
|  Microgateway-Passthrough-OAuth2 | 4G | 50 | 50 | 500 | 0 | 99.51 | 502.79 | 3.31 | 507 | 99.9 | 25.566 |
|  Microgateway-Passthrough-OAuth2 | 4G | 50 | 50 | 1000 | 0 | 49.77 | 1002.25 | 3.09 | 1007 | 99.93 | 25.57 |
|  Microgateway-Passthrough-OAuth2 | 4G | 50 | 1024 | 0 | 0 | 1929.75 | 25.83 | 27.31 | 108 | 94.79 | 86.819 |
|  Microgateway-Passthrough-OAuth2 | 4G | 50 | 1024 | 30 | 0 | 1512.14 | 32.99 | 8.24 | 89 | 96.34 | 25.572 |
|  Microgateway-Passthrough-OAuth2 | 4G | 50 | 1024 | 500 | 0 | 99.51 | 502.79 | 3.04 | 507 | 99.9 | 25.572 |
|  Microgateway-Passthrough-OAuth2 | 4G | 50 | 1024 | 1000 | 0 | 49.76 | 1003.04 | 5.28 | 1015 | 99.93 | 25.566 |
|  Microgateway-Passthrough-OAuth2 | 4G | 50 | 10240 | 0 | 0 | 1347.45 | 37 | 30.18 | 124 | 95.81 | 25.568 |
|  Microgateway-Passthrough-OAuth2 | 4G | 50 | 10240 | 30 | 0 | 1321.03 | 37.76 | 10.58 | 100 | 96.48 | 25.566 |
|  Microgateway-Passthrough-OAuth2 | 4G | 50 | 10240 | 500 | 0 | 99.33 | 503.33 | 3.64 | 509 | 99.89 | 25.566 |
|  Microgateway-Passthrough-OAuth2 | 4G | 50 | 10240 | 1000 | 0 | 49.81 | 1002.37 | 3.72 | 1007 | 99.92 | 25.573 |
|  Microgateway-Passthrough-OAuth2 | 4G | 100 | 50 | 0 | 0 | 2034.3 | 49.07 | 36.1 | 166 | 94.92 | 54.597 |
|  Microgateway-Passthrough-OAuth2 | 4G | 100 | 50 | 30 | 0 | 1988.05 | 50.21 | 16.39 | 122 | 94.96 | 25.564 |
|  Microgateway-Passthrough-OAuth2 | 4G | 100 | 50 | 500 | 0 | 199.04 | 502.72 | 2.69 | 509 | 99.78 | 25.565 |
|  Microgateway-Passthrough-OAuth2 | 4G | 100 | 50 | 1000 | 0 | 99.66 | 1002.62 | 4.21 | 1015 | 99.88 | 25.571 |
|  Microgateway-Passthrough-OAuth2 | 4G | 100 | 1024 | 0 | 0 | 2020.3 | 49.41 | 35.3 | 162 | 94.98 | 78.018 |
|  Microgateway-Passthrough-OAuth2 | 4G | 100 | 1024 | 30 | 0 | 1995.2 | 50.02 | 18.08 | 122 | 94.81 | 128.744 |
|  Microgateway-Passthrough-OAuth2 | 4G | 100 | 1024 | 500 | 0 | 198.95 | 502.77 | 3.21 | 515 | 99.79 | 25.565 |
|  Microgateway-Passthrough-OAuth2 | 4G | 100 | 1024 | 1000 | 0 | 99.71 | 1002.32 | 2.48 | 1011 | 99.89 | 25.565 |
|  Microgateway-Passthrough-OAuth2 | 4G | 100 | 10240 | 0 | 0 | 1405.3 | 71.05 | 39.25 | 189 | 95.36 | 25.564 |
|  Microgateway-Passthrough-OAuth2 | 4G | 100 | 10240 | 30 | 0 | 1421.68 | 70.21 | 24.43 | 158 | 95.75 | 25.566 |
|  Microgateway-Passthrough-OAuth2 | 4G | 100 | 10240 | 500 | 0 | 198.88 | 503.2 | 3.19 | 515 | 99.78 | 25.569 |
|  Microgateway-Passthrough-OAuth2 | 4G | 100 | 10240 | 1000 | 0 | 99.64 | 1002.49 | 3.61 | 1011 | 99.87 | 25.564 |
|  Microgateway-Passthrough-OAuth2 | 4G | 200 | 50 | 0 | 0 | 2097.48 | 95.24 | 46.97 | 233 | 94.92 | 60.446 |
|  Microgateway-Passthrough-OAuth2 | 4G | 200 | 50 | 30 | 0 | 2050.15 | 97.44 | 29.36 | 195 | 95.04 | 92.407 |
|  Microgateway-Passthrough-OAuth2 | 4G | 200 | 50 | 500 | 0 | 397.72 | 503.04 | 4.61 | 527 | 99.44 | 25.566 |
|  Microgateway-Passthrough-OAuth2 | 4G | 200 | 50 | 1000 | 0 | 199.34 | 1002.48 | 3.79 | 1015 | 99.76 | 25.566 |
|  Microgateway-Passthrough-OAuth2 | 4G | 200 | 1024 | 0 | 0 | 2040.61 | 97.9 | 47.39 | 233 | 94.97 | 121.649 |
|  Microgateway-Passthrough-OAuth2 | 4G | 200 | 1024 | 30 | 0 | 2014.06 | 99.18 | 30.91 | 197 | 94.94 | 138.693 |
|  Microgateway-Passthrough-OAuth2 | 4G | 200 | 1024 | 500 | 0 | 397.58 | 503.09 | 5.34 | 531 | 99.39 | 25.573 |
|  Microgateway-Passthrough-OAuth2 | 4G | 200 | 1024 | 1000 | 0 | 199.35 | 1002.54 | 3.48 | 1015 | 99.75 | 25.566 |
|  Microgateway-Passthrough-OAuth2 | 4G | 200 | 10240 | 0 | 0 | 1416.88 | 141.01 | 61.41 | 315 | 95.36 | 25.567 |
|  Microgateway-Passthrough-OAuth2 | 4G | 200 | 10240 | 30 | 0 | 1396.39 | 143.06 | 47.93 | 289 | 95.06 | 25.565 |
|  Microgateway-Passthrough-OAuth2 | 4G | 200 | 10240 | 500 | 0 | 397.09 | 503.67 | 5.04 | 531 | 99.38 | 25.567 |
|  Microgateway-Passthrough-OAuth2 | 4G | 200 | 10240 | 1000 | 0 | 199.22 | 1003.49 | 4.85 | 1031 | 99.74 | 25.564 |
|  Microgateway-Passthrough-OAuth2 | 4G | 300 | 50 | 0 | 0 | 1992.28 | 150.48 | 63.5 | 355 | 94.32 | 80.057 |
|  Microgateway-Passthrough-OAuth2 | 4G | 300 | 50 | 30 | 0 | 2060.87 | 145.46 | 50.78 | 297 | 94.73 | 107.614 |
|  Microgateway-Passthrough-OAuth2 | 4G | 300 | 50 | 500 | 0 | 594.5 | 504.77 | 8.08 | 547 | 98.87 | 25.565 |
|  Microgateway-Passthrough-OAuth2 | 4G | 300 | 50 | 1000 | 0 | 298.98 | 1002.7 | 4.28 | 1023 | 99.55 | 25.568 |
|  Microgateway-Passthrough-OAuth2 | 4G | 300 | 1024 | 0 | 0 | 1965.68 | 152.49 | 63.68 | 353 | 94.65 | 136.44 |
|  Microgateway-Passthrough-OAuth2 | 4G | 300 | 1024 | 30 | 0 | 2053.01 | 146 | 49.93 | 295 | 94.92 | 73.55 |
|  Microgateway-Passthrough-OAuth2 | 4G | 300 | 1024 | 500 | 0 | 594.38 | 504.82 | 8.2 | 547 | 98.85 | 25.565 |
|  Microgateway-Passthrough-OAuth2 | 4G | 300 | 1024 | 1000 | 0 | 299.06 | 1002.5 | 3.66 | 1019 | 99.55 | 25.564 |
|  Microgateway-Passthrough-OAuth2 | 4G | 300 | 10240 | 0 | 0 | 1373.28 | 218.4 | 87.71 | 489 | 95.16 | 25.565 |
|  Microgateway-Passthrough-OAuth2 | 4G | 300 | 10240 | 30 | 0 | 1405.37 | 213.39 | 70.8 | 411 | 95.14 | 25.569 |
|  Microgateway-Passthrough-OAuth2 | 4G | 300 | 10240 | 500 | 0 | 595.82 | 503.65 | 5.49 | 531 | 98.82 | 25.566 |
|  Microgateway-Passthrough-OAuth2 | 4G | 300 | 10240 | 1000 | 0 | 298.84 | 1002.71 | 3.98 | 1019 | 99.53 | 25.569 |
|  Microgateway-Passthrough-OAuth2 | 4G | 500 | 50 | 0 | 0 | 2062.05 | 242.49 | 86.9 | 505 | 94.42 | 104.069 |
|  Microgateway-Passthrough-OAuth2 | 4G | 500 | 50 | 30 | 0 | 2045.79 | 244.4 | 78.38 | 489 | 94.6 | 83.951 |
|  Microgateway-Passthrough-OAuth2 | 4G | 500 | 50 | 500 | 0 | 990 | 505.04 | 10.88 | 567 | 97.24 | 25.564 |
|  Microgateway-Passthrough-OAuth2 | 4G | 500 | 50 | 1000 | 0 | 498.14 | 1003.09 | 8.41 | 1039 | 98.89 | 72.7 |
|  Microgateway-Passthrough-OAuth2 | 4G | 500 | 1024 | 0 | 0 | 1934.46 | 258.51 | 93.1 | 527 | 94.66 | 133.536 |
|  Microgateway-Passthrough-OAuth2 | 4G | 500 | 1024 | 30 | 0 | 1986.3 | 251.7 | 79.88 | 489 | 94.52 | 93.229 |
|  Microgateway-Passthrough-OAuth2 | 4G | 500 | 1024 | 500 | 0 | 990.2 | 504.82 | 11.76 | 567 | 97.24 | 98.583 |
|  Microgateway-Passthrough-OAuth2 | 4G | 500 | 1024 | 1000 | 0 | 498.07 | 1002.88 | 5.62 | 1039 | 98.97 | 25.566 |
|  Microgateway-Passthrough-OAuth2 | 4G | 500 | 10240 | 0 | 0 | 1360.3 | 367.67 | 134.68 | 775 | 94.83 | 25.565 |
|  Microgateway-Passthrough-OAuth2 | 4G | 500 | 10240 | 30 | 0 | 1391.91 | 359.31 | 117.11 | 699 | 94.83 | 129.508 |
|  Microgateway-Passthrough-OAuth2 | 4G | 500 | 10240 | 500 | 0 | 985.39 | 507.42 | 17.87 | 575 | 97.01 | 109.543 |
|  Microgateway-Passthrough-OAuth2 | 4G | 500 | 10240 | 1000 | 0 | 497.65 | 1003.43 | 6.02 | 1039 | 98.92 | 25.566 |
|  Microgateway-Passthrough-OAuth2 | 4G | 1000 | 50 | 0 | 0 | 1958.2 | 510.8 | 151.84 | 975 | 92.73 | 152.076 |
|  Microgateway-Passthrough-OAuth2 | 4G | 1000 | 50 | 30 | 0 | 2018.1 | 495.55 | 144.66 | 919 | 92.54 | 178.811 |
|  Microgateway-Passthrough-OAuth2 | 4G | 1000 | 50 | 500 | 0 | 1869.63 | 534.59 | 33.43 | 623 | 94.3 | 125.256 |
|  Microgateway-Passthrough-OAuth2 | 4G | 1000 | 50 | 1000 | 0 | 992.54 | 1006.26 | 13.32 | 1079 | 96.9 | 80.345 |
|  Microgateway-Passthrough-OAuth2 | 4G | 1000 | 1024 | 0 | 0 | 1945.21 | 514.14 | 165.4 | 995 | 92.76 | 158.309 |
|  Microgateway-Passthrough-OAuth2 | 4G | 1000 | 1024 | 30 | 0 | 1986.15 | 503.51 | 145.12 | 919 | 92.85 | 104.234 |
|  Microgateway-Passthrough-OAuth2 | 4G | 1000 | 1024 | 500 | 0 | 1831.14 | 546.05 | 36.88 | 663 | 93.96 | 102.693 |
|  Microgateway-Passthrough-OAuth2 | 4G | 1000 | 1024 | 1000 | 0 | 991.92 | 1007.13 | 13.76 | 1079 | 96.88 | 85.293 |
|  Microgateway-Passthrough-OAuth2 | 4G | 1000 | 10240 | 0 | 0 | 1379.91 | 724.42 | 237.65 | 1407 | 94.56 | 161.239 |
|  Microgateway-Passthrough-OAuth2 | 4G | 1000 | 10240 | 30 | 0 | 1401.32 | 713.4 | 211.2 | 1303 | 94.98 | 98.427 |
|  Microgateway-Passthrough-OAuth2 | 4G | 1000 | 10240 | 500 | 0 | 1369.76 | 729.64 | 102.84 | 999 | 95.09 | 89.733 |
|  Microgateway-Passthrough-OAuth2 | 4G | 1000 | 10240 | 1000 | 0 | 958.5 | 1041.56 | 50.85 | 1199 | 96.13 | 138.941 |
