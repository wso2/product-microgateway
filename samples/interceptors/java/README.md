# Sample Interceptor - Java Spring

The interceptor service is generated using the interceptor service Open API definition.
[Swagger Editor](https://editor.swagger.io/) is used to generate the spring service.

To learn about interceptors and for instructions on how to run it please head over to the
[home directory of interceptor samples](../README.md#samples).

## Prerequisites
- Docker
- Java 11
- Maven

## Build the sample from source

1. Build the sample interceptor service.
   ```sh
   ./build.sh
   ```
   Here, the requestBody is Base64 encoded.

2. Start the interceptor service.
   ```sh
   docker run --name interceptor-java -p 9081:9081 wso2am/cc-sample-xml-interceptor-java:v1.0.0
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
   {"directRespond":null,"responseCode":null,"dynamicEndpoint":null,"headersToAdd":{"x-user":"admin"},"headersToReplace":{"content-type":"application/xml"},"headersToRemove":null,"trailersToAdd":null,"trailersToReplace":null,"trailersToRemove":null,"body":"PG5hbWU+VGhlIFByaXNvbmVyPC9uYW1lPg==","interceptorContext":null}
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
   {"responseCode":201,"headersToAdd":null,"headersToReplace":null,"headersToRemove":null,"trailersToAdd":null,"trailersToReplace":null,"trailersToRemove":null,"body":null}
   ```

4. Remove container
   ```sh
   docker rm -f interceptor-java
   ```
