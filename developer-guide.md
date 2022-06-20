# Developer Guide

This provides the information to how to work with the current microgateway setup.

## Prerequisites

- docker & docker-compose
- jdk-11
- maven 3.6
- Golang version 1.18

## Quick Start Guide

- `sh dev-scripts/remove-containers.sh` (If it is the first time this is not required to run.
The purpose is to remove the already created docker containers. Otherwise, the docker 
maven plugin would fail to create the new docker images)

- `mvn clean install` (make sure java-11 is set because the enforcer pom contains
 java-11 as the source)
  - For ARM-based processors, specify the platform using `--platform` flag before the 
  `adoptopenjdk/openjdk11:jre-11.0.11_9-alpine` base image name. This should be done in
  docker files relevant to the enforcer and mock-backend server.
  
    example:
  
    `FROM --platform=linux/x86_64 adoptopenjdk/openjdk11:jre-11.0.11_9-alpine`
 
- Navigate to distribution/target/.
 Then extract the zip file called `choreo-connect-<version>.zip`
 
 - Then execute `docker-compose up` to run the setup. This will start an envoy container,
 filter-core container and piot container. The mounted configurations can be found from
 docker-compose directory.
 
 - The `apictl` is required to add APIs to the microgateway. https://github.com/wso2/product-apim-tooling/tree/envoy-gw
 Then you need initialize a project using apictl `init` command. (help command will guide)
 Then the certificate (resources/certs/localhost.pem) needs to be copied to `~/.wso2apictl/certs` 
 directory. And finally execute `apictl mg deploy ...` command. (help command will guide through it)
 
 - Configurations
    - configurations under `[server]` refer to the rest API server which is used by
    the apictl.
    - configurations under `[envoy]` refer to the listener configurations related to envoy listener.
    Other than that the user can edit the `envoy.yaml` file located insider `<distribution>/resources/proxy`
    directory.
 - For now, to access envoy API listener you must first use `apictl mg deploy` and deploy an API
 - Use http**s** 9095 to access envoy API listener
 
 ### If the developer needs to build a separate component
 
 - In this case, first execute the `sh dev-scripts/remove-containers.sh`. If you need to keep the
 other container up and running please modify the script. If not (usually) you can simply
 execute the script as it is because this does not remove the docker image.
 
 - Make sure three containers up and running using `docker ps`.
 
 - See if the envoy configuration is correct by navigating to `localhost:9000`
 
 - Then navigate to the component and execute `mvn clean install`
 
 - Finally, navigate to your extracted distribution and run docker-compose up as it is.
 (make sure you change the configuration files mounted, it the change is relevant)
 to a configuration file change)
 
 ### If your changes does not reflect,
 
 - Check if the docker-containers are removed correctly. Using `docker ps -a`. 
 the `sh remove-containers.sh` may fail if you have changed the image name, image version.
 And if the containers are removed, docker-maven-plugin fails to recreate the image with the changes.
 
 - Make sure you make/change the configurations correctly when you are using the distribution
 zip file.
 
 - If you need to run the adaptor as a go executable, make sure you set MGW_HOME environment
 variable to point the directory where your configurations are located.
    - Ex. export MGW_HOME
    
## Error Logging in Choreo-Connect
Choreo Connect supports logging errors in formalized way. Which means an error log can be assigned with a severity level and a error code. At the moment this is only supported in `adapter` and `enforcer` components, as the `router` is a pure envoy container.

The severity levels should be assigned based on the following criteria.
| Severity level | Description                                                                                                                                                                                                                       |
| -------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Blocker        | Blocker level indicates that we need to attend to fix the error immediately, that means the component's life cycle will be affected by this. (ex: adapter will be killed, if this error is occured)                               |
| Critical       | Critical level indicates that this is important to fix like the Blocker. In this case the component's life cycle will not be affected, but the component's functionality is broken                                                  |
| Major, Minor   | These two represents, errors that we can't ignore and will not need immediate attention. With these errors, the component can continue to work without any issue.<br>Based on the priority, the Major and Minor will be assigned. |
| Trivial        | These are known errors, that we can ignore and component can use as usual even if we ignore the error. |

The error code ranges for `Adapter` and `Enforcer` as follows respectively.
- 1000 - 4999
- 5000 - 9999

In next sections you can find how the error logging can be done at `Adapter` and `Enforcer`.

### Error logging in Adapter

Importing following packages will allow you to use the formalized error logging methods at adapter.

```go
import (
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	"github.com/wso2/product-microgateway/adapter/pkg/logging"
)
```
Then log statement should follow this format.
```go
logger.<package_logger_name>.ErrorC(logging.ErrorLog{Message: "<message>", Severity: <Severity Level>, ErrorCode: <code number>})
```

Example:
```go
loggers.LoggerAPI.ErrorC(logging.ErrorDetails{
			Message:   fmt.Sprintf("Error while reading API artifacts during startup. %v", err.Error()),
			Severity:  logging.MAJOR,
			ErrorCode: 1206,
		})
```
Sample error logs in both formats(JSON & TEXT).
```
2022-06-10 13:30:27 ERRO [apis_impl.go:94] - [api.ProcessMountedAPIProjects] [-] Error while reading API artifacts during startup. open /home/wso2/artifacts/apis: no such file or directory [error_code=1206 severity=Major]
```
```
{"error_code":1206,"file":"apis_impl.go:94","func":"api.ProcessMountedAPIProjects","level":"error","msg":"Error while reading API artifacts during startup. open /home/wso2/artifacts/apis: no such file or directory","severity":"Major","time":"2022-06-10 13:37:59"}
```

### Error logging in Enforcer

Importing following package will allow you to use the formalized error logging methods at enforcer.

```java
import org.wso2.choreo.connect.enforcer.commons.logging.ErrorDetails;
```
Then the logger should be defined as follows,
```java
private static final Logger logger = LogManager.getLogger(<class name>.class);
```

Then log statement should follow this format.
```java
logger.error("<log msg>",ErrorDetails.errorLog(LoggingConstants.Severity.<severity level>, <error code number>));
```

Example:
```java
private static final Logger logger = LogManager.getLogger(AnalyticsFilter.class);

logger.error("Cannot publish the analytics event as analytics publisher is null.",
                    ErrorDetails.errorLog(LoggingConstants.Severity.CRITICAL, 5102));
```
Sample error logs in both formats(JSON & TEXT).
```
[2022-06-10 08:25:08,581][][] ERROR - {org.wso2.choreo.connect.enforcer.analytics.AnalyticsFilter} - Cannot publish the analytics event as analytics publisher is null. [severity:Major error_code:5102]
```
```
{"traceId":null,"severity":"Major","level":"ERROR","logger":"org.wso2.choreo.connect.enforcer.analytics.AnalyticsFilter","context":[],"error_code":5102,"message":"Cannot publish the analytics event as analytics publisher is null.","timestamp":"10-06-2022 08:20:06:318"}
```
