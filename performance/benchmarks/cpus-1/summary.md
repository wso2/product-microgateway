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
| ----------------------------- |-------------------| ---- | -------- |
| Apache JMeter Client          | c5.large          | 2    | 4        |
| Apache JMeter Server 01       | c5.xlarge         | 4    | 8        |
| Apache JMeter Server 02       | c5.xlarge         | 4    | 8        |
| AWS EKS cluster (three nodes) | c6i.xlarge        | 4    | 8        |

> For better throughput and performance, it is able to use computer optimized nodes for the cluster. For this test we have used c6i.xlarge EC2 instances.

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

[AWS EKS cluster with **c6i.xlarge** Amazon EC2 instances](https://aws.amazon.com/eks/?nc2=type_a) were used to deploy WSO2 Choreo Connect.
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
|10              |50B                 |2140425       |4.13                      |2374.69                  |0      |0          |9.81                     |5                                    |5                                    |12                                   |
|50              |50B                 |2899163       |15.44                     |3215.34                  |0      |0          |49.64                    |50                                   |54                                   |58                                   |
|100             |50B                 |2938040       |30.55                     |3260.78                  |0      |0          |99.62                    |66                                   |69                                   |74                                   |
|200             |50B                 |2822101       |63.72                     |3131.36                  |0      |0          |199.53                   |89                                   |92                                   |99                                   |
|500             |50B                 |2840020       |158.43                    |3150.38                  |0      |0          |499.11                   |194                                  |199                                  |220                                  |
|1000            |50B                 |2907110       |310.24                    |3218.49                  |0      |0          |998.50                   |371                                  |381                                  |401                                  |
|10              |1KiB                |2116976       |4.17                      |2350                     |0      |0          |9.80                     |5                                    |5                                    |12                                   |
|50              |1KiB                |2905703       |15.40                     |3225.4                   |0      |0          |49.67                    |48                                   |52                                   |57                                   |
|100             |1KiB                |2902109       |30.93                     |3220.87                  |0      |0          |99.62                    |66                                   |69                                   |73                                   |
|200             |1KiB                |2860810       |62.85                     |3174.2                   |0      |0          |199.50                   |89                                   |92                                   |99                                   |
|500             |1KiB                |2845235       |158.16                    |3154.25                  |0      |0          |498.88                   |196                                  |202                                  |257                                  |
|1000            |1KiB                |2838028       |317.68                    |3140.8                   |0      |0          |997.77                   |379                                  |391                                  |409                                  |
|10              |10KiB               |1938931       |4.55                      |2149.61                  |0      |0          |9.78                     |5                                    |5                                    |13                                   |
|50              |10KiB               |2385220       |18.77                     |2647.29                  |0      |0          |49.69                    |54                                   |57                                   |61                                   |
|100             |10KiB               |2425520       |37.00                     |2692.01                  |0      |0          |99.60                    |72                                   |75                                   |80                                   |
|200             |10KiB               |2381251       |75.54                     |2639.64                  |0      |0          |199.40                   |98                                   |102                                  |111                                  |
|500             |10KiB               |2489860       |180.85                    |2759                     |0      |0          |498.97                   |207                                  |217                                  |267                                  |
|1000            |10KiB               |2534124       |355.98                    |2801.6                   |0      |0          |997.31                   |405                                  |415                                  |469                                  |
|10              |100KiB              |1086595       |8.11                      |1205.67                  |0      |0          |9.78                     |10                                   |17                                   |23                                   |
|50              |100KiB              |1139416       |39.28                     |1264.48                  |0      |0          |49.67                    |74                                   |79                                   |88                                   |
|100             |100KiB              |1195828       |75.05                     |1326.94                  |0      |0          |99.59                    |116                                  |138                                  |163                                  |
|200             |100KiB              |1238292       |145.20                    |1373.8                   |0      |0          |199.48                   |205                                  |216                                  |241                                  |
|500             |100KiB              |1263209       |356.89                    |1399.63                  |0      |0          |499.51                   |409                                  |425                                  |459                                  |
|1000            |100KiB              |1251678       |719.45                    |1380.16                  |0      |0          |992.96                   |811                                  |831                                  |895                                  |
