# WSO2 Choreo Connect Performance Test Results (Router CPUs = 2 , Router concurrency level = 4)

During each release, we execute various automated performance test scenarios and publish the results.

| Test Scenarios | Description |
| --- | --- |
| Invoke an API deployed in the API Manager via Choreo Connect. | A secured API, which directly invokes the backend through Choreo Connect using JWT tokens considering different user counts and message payload sizes. |

Our test client is [Apache JMeter](https://jmeter.apache.org/index.html). We test each scenario for a fixed duration of
time (15 minutes). We split the test results into warmup (5 minutes) and measurement parts and use the measurement part (test results after 5 minutes) to compute the
performance metrics.

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
When doing below test scenarios, `--cpus` option is provided as 2 and concurrency level for the router provided as 4. Below table includes
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
|10              |50B                 |1414878       |3                         |2357.4                   |0      |0          |7.0722                   |4                                    |5                                    |12                                   |
|50              |50B                 |2212824       |12                        |3687                     |0      |0          |44.244                   |35                                   |39                                   |47                                   |
|100             |50B                 |2572424       |22                        |4286.1                   |0      |0          |94.2942                  |45                                   |51                                   |62                                   |
|200             |50B                 |2937304       |40                        |4893.6                   |0      |0          |195.744                  |65                                   |72                                   |87                                   |
|500             |50B                 |3005702       |98                        |5007.4                   |0      |0          |490.7252                 |136                                  |156                                  |181                                  |
|1000            |50B                 |2942082       |203                       |4875.5                   |0      |0          |989.7265                 |269                                  |287                                  |327                                  |
|10              |1KiB                |1364171       |3                         |2273                     |0      |0          |6.819                    |5                                    |6                                    |12                                   |
|50              |1KiB                |2155348       |13                        |3591                     |0      |0          |46.683                   |35                                   |40                                   |48                                   |
|100             |1KiB                |2516705       |23                        |4193.2                   |0      |0          |96.4436                  |46                                   |53                                   |68                                   |
|200             |1KiB                |2849183       |41                        |4746.9                   |0      |0          |194.6229                 |68                                   |78                                   |99                                   |
|500             |1KiB                |2965831       |99                        |4968.8                   |0      |0          |491.9112                 |134                                  |153                                  |179                                  |
|1000            |1KiB                |2946062       |203                       |4877.3                   |0      |0          |990.0919                 |268                                  |286                                  |323                                  |
|10              |10KiB               |1269254       |4                         |2114.7                   |0      |0          |8.4588                   |5                                    |6                                    |11                                   |
|50              |10KiB               |1942673       |14                        |3236.8                   |0      |0          |45.3152                  |34                                   |39                                   |47                                   |
|100             |10KiB               |2248710       |26                        |3746.3                   |0      |0          |97.4038                  |47                                   |54                                   |68                                   |
|200             |10KiB               |2507948       |47                        |4178.5                   |0      |0          |196.3895                 |72                                   |81                                   |99                                   |
|500             |10KiB               |2502086       |119                       |4165.9                   |0      |0          |495.7421                 |171                                  |187                                  |215                                  |
|1000            |10KiB               |2531218       |236                       |4187.1                   |0      |0          |988.1556                 |307                                  |330                                  |377                                  |
|10              |100KiB              |667168        |8                         |1111.6                   |0      |0          |8.8928                   |11                                   |12                                   |20                                   |
|50              |100KiB              |917388        |32                        |1528.5                   |0      |0          |48.912                   |51                                   |59                                   |79                                   |
|100             |100KiB              |885349        |67                        |1474.8                   |0      |0          |98.8116                  |128                                  |156                                  |207                                  |
|200             |100KiB              |905439        |131                       |1508.4                   |0      |0          |197.6004                 |260                                  |301                                  |393                                  |
|500             |100KiB              |894891        |334                       |1490.3                   |0      |0          |497.7602                 |505                                  |568                                  |739                                  |
|1000            |100KiB              |929809        |645                       |1531                     |0      |0          |987.495                  |796                                  |886                                  |1118                                 |

