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
When doing below test scenarios, `--cpus` option is provided as 2 and concurrency level for the router provided as 4. Replica count for the
Choreo Connect deployment was one. Below table includes configuration details relevant to the Choreo Connect deployment.

|Container Name|Requesting Memory Amount (Mi)|Requesting CPU Amount (m)|Limiting Memory Amount (Mi)|Limiting CPU Amount (m)|
|--------------|-----------------------------|-------------------------|---------------------------|-----------------------|
|Adapter       |500                          |500                      |500                        |500                    |
|Enforcer      |2000                         |1000                     |2000                       |1000                   |
|Router        |500                          |2000                     |500                        |2000                   |
|Netty backend |4096                         |2000                     |6114                       |2000                   |

For this four concurrency setup, enforcer's Java Virtual Machine's (JVM) memory allocation changed as below.
* `Xmx1500m (Maximum memory allocation for JVM)`
* `Xms1500m (Satrting memory allocation for JVM)`

Also, for the `enforcer.authService` below configurations were used.
```yaml
    [enforcer.authService]
      # Port of the Enforcer auth service
      port = 8081
      # Maximum message size in bytes
      maxMessageSize = 1000000000
      # Maximum header size in bytes
      maxHeaderLimit = 8192
      # Keep alive time in seconds for connection with the router via external authz service
      keepAliveTime = 600
      # Thread pool configurations of gRPC netty based server in Enforcer that handles the incoming requests in the Choreo Connect
      [enforcer.authService.threadPool]
        # Minimum number of workers to keep alive
        coreSize = 400
        # Maximum pool size
        maxSize = 1000
        # Timeout in seconds for idle threads waiting for work
        keepAliveTime = 600
        # Queue size of the worker threads
        queueSize = 2000
```

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
|10              |50B                 |2465235       |3                         |2738.5                   |0      |0          |8.2155                   |4                                    |5                                    |16                                   |
|50              |50B                 |3612288       |11                        |4012.5                   |0      |0          |44.1375                  |29                                   |35                                   |43                                   |
|100             |50B                 |4221994       |20                        |4689.8                   |0      |0          |93.796                   |44                                   |49                                   |65                                   |
|200             |50B                 |4715766       |37                        |5238.6                   |0      |0          |193.8282                 |59                                   |66                                   |84                                   |
|500             |50B                 |4921820       |90                        |5466.4                   |0      |0          |491.976                  |126                                  |143                                  |171                                  |
|1000            |50B                 |4617754       |194                       |5127.7                   |0      |0          |994.7738                 |252                                  |269                                  |300                                  |
|10              |1KiB                |2474194       |3                         |2748.4                   |0      |0          |8.2452                   |4                                    |5                                    |15                                   |
|50              |1KiB                |3608191       |11                        |4008.2                   |0      |0          |44.0902                  |29                                   |35                                   |42                                   |
|100             |1KiB                |4187712       |20                        |4656.9                   |0      |0          |93.138                   |44                                   |50                                   |68                                   |
|200             |1KiB                |4722928       |37                        |5246.3                   |0      |0          |194.1131                 |60                                   |67                                   |87                                   |
|500             |1KiB                |4962712       |90                        |5512.1                   |0      |0          |496.089                  |126                                  |143                                  |172                                  |
|1000            |1KiB                |4379559       |204                       |4862.5                   |0      |0          |991.95                   |260                                  |276                                  |306                                  |
|10              |10KiB               |2292961       |3                         |2547                     |0      |0          |7.641                    |4                                    |5                                    |10                                   |
|50              |10KiB               |3292426       |13                        |3657.4                   |0      |0          |47.5462                  |26                                   |31                                   |40                                   |
|100             |10KiB               |3726912       |23                        |4136.9                   |0      |0          |95.1487                  |44                                   |51                                   |68                                   |
|200             |10KiB               |3823400       |46                        |4246.9                   |0      |0          |195.3574                 |72                                   |81                                   |100                                  |
|500             |10KiB               |4213767       |106                       |4688.7                   |0      |0          |497.0022                 |155                                  |173                                  |206                                  |
|1000            |10KiB               |3773976       |237                       |4188.4                   |0      |0          |992.6508                 |301                                  |322                                  |365                                  |
|10              |100KiB              |1137663       |7                         |1263.8                   |0      |0          |8.8466                   |11                                   |12                                   |15                                   |
|50              |100KiB              |1312758       |33                        |1458.3                   |0      |0          |48.1239                  |60                                   |71                                   |93                                   |
|100             |100KiB              |1310828       |68                        |1456                     |0      |0          |99.008                   |127                                  |150                                  |198                                  |
|200             |100KiB              |1291683       |138                       |1434.8                   |0      |0          |198.0024                 |246                                  |282                                  |362                                  |
|500             |100KiB              |1260706       |356                       |1400.2                   |0      |0          |498.4712                 |469                                  |516                                  |681                                  |
|1000            |100KiB              |1280531       |701                       |1420.4                   |0      |0          |995.7004                 |827                                  |927                                  |1402                                 |

