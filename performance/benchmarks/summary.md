# WSO2 API Microgateway Performance Test Results

During each release, we execute various automated performance test scenarios and publish the results.

| Test Scenarios | Description |
| --- | --- |
| Microgateway-Passthrough-JWT (W/O APIM) | A secured API, which directly invokes the backend through Microgateway using JWT tokens |

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
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 50 | 50 | 0 | 0 | 2474.79 | 20.14 | 3.57 | 33 | 98.45 | 18.478 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 50 | 50 | 30 | 0 | 1570.55 | 31.77 | 1.31 | 37 | 99.12 | 18.474 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 50 | 50 | 500 | 0 | 99.41 | 502.94 | 1.73 | 509 | 99.83 | 18.508 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 50 | 50 | 1000 | 0 | 49.81 | 1002.39 | 1.32 | 1007 | 99.85 | 18.508 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 50 | 1024 | 0 | 0 | 2423.22 | 20.56 | 3.85 | 34 | 98.48 | 18.483 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 50 | 1024 | 30 | 0 | 1566.91 | 31.84 | 1.24 | 36 | 99.13 | 18.511 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 50 | 1024 | 500 | 0 | 99.49 | 502.98 | 1.68 | 509 | 99.83 | 18.479 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 50 | 1024 | 1000 | 0 | 49.77 | 1002.71 | 1.69 | 1011 | 99.86 | 18.512 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 50 | 10240 | 0 | 0 | 1932.55 | 25.79 | 8.54 | 52 | 98.75 | 18.474 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 50 | 10240 | 30 | 0 | 1486.57 | 33.55 | 2.15 | 42 | 99.08 | 18.588 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 50 | 10240 | 500 | 0 | 99.39 | 503.44 | 1.74 | 511 | 99.83 | 18.484 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 50 | 10240 | 1000 | 0 | 49.81 | 1003.14 | 1.94 | 1007 | 99.86 | 18.475 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 100 | 50 | 0 | 0 | 2495.56 | 39.99 | 5.38 | 57 | 97.56 | 18.475 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 100 | 50 | 30 | 0 | 2457.92 | 40.61 | 4.09 | 55 | 97.83 | 18.509 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 100 | 50 | 500 | 0 | 198.83 | 503.02 | 1.92 | 511 | 99.67 | 18.509 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 100 | 50 | 1000 | 0 | 99.71 | 1002.59 | 1.71 | 1011 | 99.76 | 18.481 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 100 | 1024 | 0 | 0 | 2444.3 | 40.83 | 5.86 | 59 | 97.58 | 18.479 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 100 | 1024 | 30 | 0 | 2392.63 | 41.71 | 4.43 | 57 | 97.84 | 18.478 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 100 | 1024 | 500 | 0 | 198.98 | 502.9 | 1.72 | 509 | 99.67 | 18.508 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 100 | 1024 | 1000 | 0 | 99.64 | 1002.78 | 2.05 | 1011 | 99.75 | 18.508 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 100 | 10240 | 0 | 0 | 1955.89 | 51.03 | 15.16 | 94 | 98.08 | 18.479 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 100 | 10240 | 30 | 0 | 1899.32 | 52.56 | 9.34 | 80 | 98.13 | 18.508 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 100 | 10240 | 500 | 0 | 198.77 | 503.38 | 2.04 | 515 | 99.66 | 18.508 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 100 | 10240 | 1000 | 0 | 99.62 | 1002.93 | 2.17 | 1011 | 99.74 | 18.473 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 200 | 50 | 0 | 0 | 2437.18 | 81.97 | 8.8 | 105 | 95.9 | 18.478 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 200 | 50 | 30 | 0 | 2473.24 | 80.76 | 9.87 | 106 | 96.12 | 18.485 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 200 | 50 | 500 | 0 | 397.62 | 503.28 | 2.54 | 515 | 99.29 | 18.479 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 200 | 50 | 1000 | 0 | 199.19 | 1002.77 | 2.41 | 1015 | 99.54 | 18.508 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 200 | 1024 | 0 | 0 | 2402.91 | 83.14 | 9.29 | 108 | 95.88 | 18.508 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 200 | 1024 | 30 | 0 | 2424.46 | 82.39 | 10.06 | 108 | 96.06 | 18.478 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 200 | 1024 | 500 | 0 | 397.63 | 503.32 | 2.59 | 515 | 99.29 | 18.509 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 200 | 1024 | 1000 | 0 | 199.29 | 1003.29 | 2.95 | 1019 | 99.54 | 18.508 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 200 | 10240 | 0 | 0 | 1941.85 | 102.88 | 23.42 | 166 | 96.78 | 18.478 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 200 | 10240 | 30 | 0 | 1915.24 | 104.31 | 20.98 | 160 | 96.84 | 18.513 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 200 | 10240 | 500 | 0 | 396.91 | 504.03 | 2.9 | 515 | 99.24 | 18.512 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 200 | 10240 | 1000 | 0 | 199.12 | 1004.1 | 3.55 | 1019 | 99.52 | 18.513 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 300 | 50 | 0 | 0 | 2432.19 | 123.23 | 12.16 | 152 | 94.1 | 18.48 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 300 | 50 | 30 | 0 | 2470.72 | 121.3 | 14.09 | 151 | 94.24 | 18.474 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 300 | 50 | 500 | 0 | 596.32 | 502.9 | 2.93 | 519 | 98.73 | 18.484 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 300 | 50 | 1000 | 0 | 298.96 | 1003.01 | 3.04 | 1019 | 99.25 | 18.587 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 300 | 1024 | 0 | 0 | 2397.05 | 125.04 | 13.11 | 157 | 94.07 | 18.476 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 300 | 1024 | 30 | 0 | 2414.01 | 124.16 | 14.21 | 157 | 94.44 | 18.485 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 300 | 1024 | 500 | 0 | 596.6 | 503 | 2.81 | 515 | 98.71 | 18.474 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 300 | 1024 | 1000 | 0 | 299.02 | 1003.09 | 3.05 | 1019 | 99.24 | 18.478 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 300 | 10240 | 0 | 0 | 1911.07 | 156.86 | 30.73 | 237 | 95.36 | 18.512 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 300 | 10240 | 30 | 0 | 1901.37 | 157.65 | 28.37 | 229 | 95.57 | 18.472 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 300 | 10240 | 500 | 0 | 596.48 | 503.1 | 2.47 | 515 | 98.62 | 18.592 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 300 | 10240 | 1000 | 0 | 298.72 | 1003.08 | 3 | 1019 | 99.21 | 18.476 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 500 | 50 | 0 | 0 | 2276.89 | 219.47 | 28.18 | 365 | 88.53 | 70.032 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 500 | 50 | 30 | 0 | 2258.54 | 221.24 | 24.18 | 359 | 89.15 | 68.885 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 500 | 50 | 500 | 0 | 971.2 | 514.55 | 17.3 | 563 | 96.85 | 18.509 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 500 | 50 | 1000 | 0 | 497.96 | 1003.65 | 5.47 | 1031 | 98.34 | 18.475 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 500 | 1024 | 0 | 0 | 2255.93 | 221.51 | 27.15 | 365 | 88.94 | 70.302 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 500 | 1024 | 30 | 0 | 2261.71 | 220.95 | 25.42 | 359 | 89.1 | 69.524 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 500 | 1024 | 500 | 0 | 965.68 | 517.8 | 17.1 | 559 | 96.91 | 18.475 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 500 | 1024 | 1000 | 0 | 498.27 | 1002.98 | 4.2 | 1031 | 98.33 | 18.507 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 500 | 10240 | 0 | 0 | 1857.2 | 269.35 | 46.73 | 399 | 92.69 | 67.985 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 500 | 10240 | 30 | 0 | 1832.29 | 273.04 | 44.3 | 391 | 92.79 | 67.631 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 500 | 10240 | 500 | 0 | 992.67 | 503.82 | 5.61 | 527 | 96.68 | 59.786 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 500 | 10240 | 1000 | 0 | 498.12 | 1003.02 | 3.39 | 1023 | 98.21 | 18.474 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 1000 | 50 | 0 | 0 | 2045.37 | 489.05 | 86.02 | 731 | 80.86 | 120.152 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 1000 | 50 | 30 | 0 | 2051.35 | 487.64 | 85.69 | 747 | 80.81 | 119.447 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 1000 | 50 | 500 | 0 | 1794.51 | 557.2 | 57.66 | 751 | 84.04 | 109.038 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 1000 | 50 | 1000 | 0 | 954.73 | 1046.28 | 50.05 | 1255 | 91.28 | 108.684 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 1000 | 1024 | 0 | 0 | 2025.05 | 493.88 | 87.54 | 747 | 81.53 | 121.203 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 1000 | 1024 | 30 | 0 | 2000.17 | 500.14 | 86.6 | 759 | 81.37 | 120.885 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 1000 | 1024 | 500 | 0 | 1810.72 | 552.15 | 53.78 | 723 | 83.82 | 110.887 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 1000 | 1024 | 1000 | 0 | 993.41 | 1005.61 | 16.75 | 1055 | 91.06 | 110.549 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 1000 | 10240 | 0 | 0 | 1657.55 | 603.28 | 110.39 | 919 | 85.4 | 118.8 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 1000 | 10240 | 30 | 0 | 1655.05 | 604.19 | 107.18 | 915 | 85.45 | 118.884 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 1000 | 10240 | 500 | 0 | 1433.07 | 697.48 | 118.99 | 1023 | 87.14 | 115.796 |
|  Microgateway-Passthrough-JWT (W/O APIM) | 512M | 1000 | 10240 | 1000 | 0 | 972.94 | 1026.28 | 41.79 | 1239 | 91.17 | 114.967 |
