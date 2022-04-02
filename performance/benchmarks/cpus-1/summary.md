# WSO2 Choreo Connect Performance Test Results (Router CPU = 1 , Router concurrency level = 2)

During each release, we execute various automated performance test scenarios and publish the results.

| Test Scenarios | Description |
| -------------- | ----------- |
| Invoke an API deployed in the API Manager via Choreo Connect. | A secured API, which directly invokes the backend through Choreo Connect using JWT tokens considering different user counts and message payload sizes. |

Our test client is [Apache JMeter](https://jmeter.apache.org/index.html). We test each scenario for a fixed duration of
time (15 minutes). We split the test results into warmup (5 minutes) and measurement parts and use the measurement part (test results after 5 minutes) to compute the
performance metrics. Below diagram shows the test setup.

Test scenarios use a [Netty](https://netty.io/) based back-end service which echoes back any request
posted to it.

Below diagram shows the test setup.
![picture](images/diagram.png)

| Name                          | EC2 Instance Type | vCPU | Mem(GiB) |
| ----------------------------- | ----------------- | ---- | -------- |
| Apache JMeter Client          | c5.large          | 2    | 4        |
| Apache JMeter Server 01       | c5.xlarge         | 4    | 8        |
| Apache JMeter Server 02       | c5.xlarge         | 4    | 8        |
| AWS EKS cluster (three nodes) | c5.xlarge         | 4    | 8        |

We executed tests for different numbers of concurrent users and message sizes (payloads).

The main performance metrics:

1. **Throughput**: The number of requests that the WSO2 Choreo Connect processes during a specific time interval (e.g. per second).
2. **Response Time**: The end-to-end latency for an operation of invoking an API. The complete distribution of response times was recorded.

In addition to the above metrics, we measure the load average and several other API request related metrics.

The following are the test parameters.

| Test Parameter       | Description                                                     | Values                      |
| -------------------- | --------------------------------------------------------------- | --------------------------- |
| Scenario Name        | The name of the test scenario.                                  | Refer to the above table.   |
| Heap Size            | The amount of memory allocated to the application               | 1024M                       |
| Concurrent Users     | The number of users accessing the application at the same time. | 10, 50, 100, 200, 500, 1000 |
| Message Size (Bytes) | The request payload size in Bytes.                              | 50, 1024, 10240, 102400     |
| Back-end Delay (ms)  | The delay added by the back-end service.                        | 0                           |

The duration of each test is **900 seconds**. The warm-up period is **300 seconds**.
The measurement results are collected after the warm-up period.

[AWS EKS cluster with **c5.xlarge** Amazon EC2 instances](https://aws.amazon.com/eks/?nc2=type_a) were used to deploy WSO2 Choreo Connect.
When doing below test scenarios, `--cpus` option is provided as 1 and concurrency level for the router provided as 2. Replica count for the
Choreo Connect deployment was one. Below table includes configuration details relevant to the Choreo Connect deployment.

|Container Name|Requesting Memory Amount (Mi)|Requesting CPU Amount (m)|Limiting Memory Amount (Mi)|Limiting CPU Amount (m)|
|--------------|-----------------------------|-------------------------|---------------------------|-----------------------|
|Adapter       |500                          |500                      |500                        |500                    |
|Enforcer      |1000                         |1000                     |1000                       |1000                   |
|Router        |500                          |1000                     |500                        |1000                   |
|Netty backend |4096                         |2000                     |6114                       |2000                   |


The jmeter is configured such that the maximum waiting time for receiving a response to be 20 seconds.

The following figures shows how the Throughput changes for different number of concurrent users with different payload sizes.
![picture](images/throughput.png)

The following figures shows how the Average Response Time changes for different number of concurrent users with different payload sizes.
![picture](images/response_time_0ms.png)

Let’s look at the 90th, 95th, and 99th Response Time percentiles for 0ms backend delay.
This is useful to measure the percentage of requests that exceeded the response time value for a given percentile.
A percentile can also tell the percentage of requests completed below the particular response time value.
![picture](images/percentile.png)

The following are the measurements collected from each performance test conducted for a given combination of
test parameters.

| Measurement | Description |
| ----------- | ----------- |
| Error % | Percentage of requests with errors |
| Total requests | Number of requests happened during the testing period |
| Average Response Time (ms) | The average response time of a set of results |
| 90th Percentile of Response Time (ms) | 90% of the requests took no more than this time. The remaining samples took at least as long as this |
| 95th Percentile of Response Time (ms) | 95% of the requests took no more than this time. The remaining samples took at least as long as this |
| 99th Percentile of Response Time (ms) | 99% of the requests took no more than this time. The remaining samples took at least as long as this |
| Throughput (Requests/sec) | The throughput measured in requests per second. |
| Little's Law Verification |  (Throughput) x (Average Response Time) / 1000|

The following is the summary of performance test results collected for the measurement period.

|Concurrent Users|Message Size (Bytes)|Total requests|Average Response Time (ms)|Throughput (Requests/sec)|Error %|Error Count|Little's law verification|90th Percentile of Response Time (ms)|95th Percentile of Response Time (ms)|99th Percentile of Response Time (ms)|
|----------------|--------------------|--------------|--------------------------|-------------------------|-------|-----------|-------------------------|-------------------------------------|-------------------------------------|-------------------------------------|
|10              |50B                 |2073910       |3                         |2303.8                   |0      |0          |6.9114                   |5                                    |5                                    |8                                    |
|50              |50B                 |2709454       |16                        |3010.2                   |0      |0          |48.1632                  |52                                   |56                                   |60                                   |
|100             |50B                 |2774645       |31                        |3082.4                   |0      |0          |95.5544                  |68                                   |70                                   |75                                   |
|200             |50B                 |2763400       |64                        |3069.1                   |0      |0          |196.4224                 |101                                  |105                                  |149                                  |
|500             |50B                 |2785153       |160                       |3093.3                   |0      |0          |494.928                  |197                                  |203                                  |254                                  |
|1000            |50B                 |2751126       |326                       |3055.3                   |0      |0          |996.0278                 |380                                  |389                                  |408                                  |
|10              |1KiB                |2091911       |3                         |2323.7                   |0      |0          |6.9711                   |5                                    |5                                    |8                                    |
|50              |1KiB                |2793064       |15                        |3102.9                   |0      |0          |46.5435                  |51                                   |55                                   |59                                   |
|100             |1KiB                |2843694       |31                        |3158.8                   |0      |0          |97.9228                  |66                                   |69                                   |73                                   |
|200             |1KiB                |3665465       |62                        |3162.9                   |0      |0          |196.0998                 |88                                   |91                                   |98                                   |
|500             |1KiB                |2853269       |157                       |3168.8                   |0      |0          |497.5016                 |194                                  |200                                  |220                                  |
|1000            |1KiB                |2837340       |316                       |3149.5                   |0      |0          |995.242                  |371                                  |382                                  |402                                  |
|10              |10KiB               |1813196       |4                         |2014                     |0      |0          |8.056                    |5                                    |6                                    |9                                    |
|50              |10KiB               |2292107       |19                        |2546.3                   |0      |0          |48.3797                  |55                                   |58                                   |63                                   |
|100             |10KiB               |2283122       |38                        |2532.6                   |0      |0          |96.2388                  |72                                   |75                                   |81                                   |
|200             |10KiB               |2325698       |76                        |2583.4                   |0      |0          |196.3384                 |102                                  |107                                  |150                                  |
|500             |10KiB               |2389789       |187                       |2654.5                   |0      |0          |496.3915                 |222                                  |251                                  |275                                  |
|1000            |10KiB               |2446750       |367                       |2717.2                   |0      |0          |997.2124                 |416                                  |439                                  |481                                  |
|10              |100KiB              |1039416       |8                         |1154.6                   |0      |0          |9.2368                   |11                                   |13                                   |17                                   |
|50              |100KiB              |1202718       |36                        |1336.1                   |0      |0          |48.0996                  |60                                   |68                                   |83                                   |
|100             |100KiB              |1225628       |72                        |1361.4                   |0      |0          |98.0208                  |109                                  |117                                  |140                                  |
|200             |100KiB              |1166856       |153                       |1296.1                   |0      |0          |198.3033                 |214                                  |232                                  |271                                  |
|500             |100KiB              |1619551       |365                       |1349.1                   |0      |0          |492.4215                 |363                                  |446                                  |473                                  |
|1000            |100KiB              |1167844       |770                       |1296.6                   |0      |0          |998.382                  |863                                  |897                                  |979                                  |
