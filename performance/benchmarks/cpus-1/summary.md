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
When doing below test scenarios, `--cpus` option is provided as 1 and concurrency level for the router provided as 2. Below table includes
configuration details relevant to the Choreo Connect deployment.

|Container Name|Requesting Memory Amount (Mi)|Requesting CPU Amount (m)|Limiting Memory Amount (Mi)|Limiting CPU Amount (m)|
|--------------|-----------------------------|-------------------------|---------------------------|-----------------------|
|Adapter       |500                          |500                      |500                        |500                    |
|Enforcer      |1000                         |1000                     |1000                       |1000                   |
|Router        |500                          |1000                     |500                        |1000                   |


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
|10              |50B                 |1598397       |3                         |2303.5                   |0      |0          |6.9105                   |5                                    |5                                    |11                                   |
|50              |50B                 |1579559       |18                        |2631.4                   |0      |0          |47.3652                  |55                                   |58                                   |62                                   |
|100             |50B                 |1630033       |36                        |2715.5                   |0      |0          |97.758                   |71                                   |74                                   |79                                   |
|200             |50B                 |1398761       |71                        |2755.2                   |0      |0          |195.6192                 |95                                   |98                                   |106                                  |
|500             |50B                 |1734475       |172                       |2889                     |0      |0          |496.908                  |203                                  |211                                  |264                                  |
|1000            |50B                 |1518103       |339                       |2920.4                   |0      |0          |990.0156                 |393                                  |402                                  |455                                  |
|10              |1KiB                |1214277       |4                         |2023.1                   |0      |0          |8.0924                   |5                                    |6                                    |9                                    |
|50              |1KiB                |1634432       |17                        |2723.1                   |0      |0          |46.2927                  |52                                   |55                                   |60                                   |
|100             |1KiB                |1661783       |35                        |2768.5                   |0      |0          |96.8975                  |72                                   |75                                   |80                                   |
|200             |1KiB                |1669034       |71                        |2780.9                   |0      |0          |197.4439                 |104                                  |109                                  |157                                  |
|500             |1KiB                |1718444       |173                       |2862.6                   |0      |0          |495.2298                 |208                                  |226                                  |270                                  |
|1000            |1KiB                |1748347       |342                       |2900.2                   |0      |0          |991.8684                 |336                                  |405                                  |460                                  |
|10              |10KiB               |1081158       |5                         |1801.2                   |0      |0          |9.006                    |6                                    |6                                    |10                                   |
|50              |10KiB               |1385886       |21                        |2309.2                   |0      |0          |48.4932                  |56                                   |59                                   |63                                   |
|100             |10KiB               |1395324       |42                        |2324.8                   |0      |0          |97.6416                  |77                                   |80                                   |86                                   |
|200             |10KiB               |1404470       |84                        |2339.8                   |0      |0          |196.5432                 |102                                  |106                                  |149                                  |
|500             |10KiB               |1496117       |199                       |2491.8                   |0      |0          |495.8682                 |258                                  |270                                  |290                                  |
|1000            |10KiB               |1542416       |388                       |2553.4                   |0      |0          |990.7192                 |460                                  |477                                  |504                                  |
|10              |100KiB              |658531        |8                         |1097.1                   |0      |0          |8.7768                   |11                                   |13                                   |20                                   |
|50              |100KiB              |732394        |40                        |1220.9                   |0      |0          |48.836                   |67                                   |74                                   |89                                   |
|100             |100KiB              |749527        |79                        |1248.6                   |0      |0          |98.6394                  |125                                  |141                                  |170                                  |
|200             |100KiB              |751373        |159                       |1251.8                   |0      |0          |199.0362                 |233                                  |241                                  |277                                  |
|500             |100KiB              |759492        |394                       |1265.7                   |0      |0          |498.6858                 |484                                  |507                                  |553                                  |
|1000            |100KiB              |763591        |786                       |1263.5                   |0      |0          |993.111                  |892                                  |924                                  |996                                  |

