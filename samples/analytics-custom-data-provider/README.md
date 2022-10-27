# Custom Data Provider for Choreo Connect

This sample will allow you to add custom analytics data to the existing event schema.

__Steps to add custom analytics data:__

- Add the necessary component versions for `carbon.apimgt.version` in pom file in the root directory.

- Build the project using Maven:

        mvn clean install


- Copy the generated JAR file from the target folder and place it in `<CC-HOME>/docker-compose/resources/enforcer/dropins`.

  - Edit configurations in the `config.toml with following.


     [analytics.adapter.customProperties]
         enabled = true
         requestHeadersToLog = ["request_headers_to_pass_from_router_to_enforcer"]
         responseHeadersToLog = ["response_headers_to_pass_from_router_to_enforcer"]
         responseTrailersToLog = ["response_trailers_to_pass_from_router_to_enforcer"]

     [analytics.enforcer]
         [analytics.enforcer.configProperties]
            "publisher.reporter.class" = " fully qualified class name relevant to the customReporter"
            "publisher.custom.data.provider.class" = "fully qualified class name relevant to the customDataProvider"

- Update `log4j.properties` file's rootLogger section with following.


     logger.org-wso2-analytics-publisher.name = org.wso2.am.analytics.publisher
     logger.org-wso2-analytics-publisher.level = TRACE
     logger.org-wso2-analytics-publisher.appenderRef.CARBON_TRACE_LOGFILE.ref = ENFORCER_ACCESS_LOG

- Add org-wso2-analytics-publisher to the loggers.
- Enable Choreo Connect analyitcs.

Once this is successfully deployed you can check whether this is working by following the below steps:
1. [Enable trace logs](https://apim.docs.wso2.com/en/4.2.0/administer/logging-and-monitoring/logging/configuring-logging/#enabling-logs-for-a-component) for the component: `org.wso2.am.analytics.publisher`
1. Follow this sample configurations,

        logger.org-wso2-analytics-publisher.name = org.wso2.am.analytics.publisher
        logger.org-wso2-analytics-publisher.level = TRACE
        logger.org-wso2-analytics-publisher.appenderRef.CARBON_TRACE_LOGFILE.ref = CARBON_TRACE_LOGFILE

 Now you can trigger and event and check the Enforcer logs. There will be the required header under the properties field.


      choreo-connect-with-apim-enforcer-1  | ... Info -
      {
         "apiName":"G",
         "proxyResponseCode":401,
         "errorType":"AUTH",
         .
         .
         .
         "errorMessage":"AUTHENTICATION_FAILURE",
         "eventType":"fault",
         "regionId":"UNKNOWN",
         "applicationId":"UNKNOWN",
         "apiType":"HTTP",
         "properties":{
            "user-agent":"curl/7.83.0"
         }
      }