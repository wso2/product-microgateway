# Sample Interceptor - WSO2 Micro Integrator

The interceptor service REST API is added using swagger definition file which generates the Synapse API configuration file in Micro Integrator.

To learn about interceptors and for instructions on how to run it please head over to the
[home directory of interceptor samples](../README.md#samples).

## Prerequisites
- [WSO2 Integration Studio](https://wso2.com/integration/integration-studio/) 8.1.0
- Docker
- Java 11
- Maven

## Build the sample from source

1. First you need to copy the required certs
   ```sh
   ./copy-certs.sh
   ```
   Here, the requestBody is Base64 encoded.

2. Then, Open the `InterceptorServiceWithMI` project to Integration Studio. You can view the sample service configuration in  following file.
   ```
   /InterceptorServiceWithMIConfigs/src/main/synapse-config/api/Choreo-Connect_Interceptor_Service.xml
   ```

3. Next, In the project explorer window, right click on `InterceptorServiceWithMIDockerExporter` and select `Build Docker Image` option. This will create the docker image we want to run.

2. Start the interceptor service.
   ```sh
   docker run --name interceptor-wso2-mi -p 9081:8253 wso2am/cc-sample-xml-interceptor-wso2-mi:v1.0.0
   ```

3. Test the interceptor service.
   ```sh
   curl https://localhost:9081/api/v1/handle-request \
      -H "content-type: application/json" \
      -H "accept: application/json" \
      -d '{"requestBody": "eyJuYW1lIjoiVGhlIFByaXNvbmVyIn0K"}' \
      --cert ../resources/certs/mg.pem \
      --key ../resources/mg.key \
      --cacert ../resources/certs/interceptor.pem
   ```
   Sample response
   ```
   { "headersToAdd": { "x-user": "admin" }, "headersToReplace": { "content-type": "application/xml" }, "body": "PG5hbWU+VGhlIFByaXNvbmVyPC9uYW1lPg==" }
   ```

   ```sh
   curl https://localhost:9081/api/v1/handle-response \
      -H "content-type: application/json" \
      -H "accept: application/json" \
      -d '{"responseCode": 200}' \
      --cert ../resources/certs/mg.pem \
      --key ../resources/mg.key \
      --cacert ../resources/certs/interceptor.pem
   ```
   Sample response
   ```
   { "responseCode": 201 }
   ```

4. Remove container
   ```sh
   docker rm -f interceptor-wso2-mi
   ```

When testing this interceptor service, you need to change the [bookstore-api.yaml](https://github.com/wso2/product-microgateway/blob/main/samples/interceptors/bookstore-api.yaml) file with the http ports as given below:

```yaml
x-wso2-request-interceptor:
  serviceURL: https://xml-interceptor:8253
  includes:
    - request_body
x-wso2-response-interceptor:
  serviceURL: https://xml-interceptor:8253
```
