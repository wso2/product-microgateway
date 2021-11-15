# Sample Interceptor - Go

The interceptor service is generated using the interceptor service Open API definition.
[Swagger Editor](https://editor.swagger.io/) is used to generate the spring service.

To learn about interceptors and for instructions on how to run it please head over to the
[home directory of interceptor samples](../README.md#samples).

## Prerequisites
- Docker

## Build the sample from source

1. Build the sample interceptor service.
   ```sh
   ./build.sh
   ```
   Here, the requestBody is Base64 encoded.

2. Start the interceptor service.
   ```sh
   docker run --name interceptor-go -p 9081:9081 wso2am/cc-sample-xml-interceptor-go:v1.0.0
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
   ```json
   {"headersToAdd":{"x-user":"admin"},"headersToReplace":{"content-type":"application/xml"},"body":"PG5hbWU+VGhlIFByaXNvbmVyPC9uYW1lPg=="}
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
   ```json
   {"responseCode":201}
   ```

4. Remove container
   ```sh
   docker rm -f interceptor-go
   ```
