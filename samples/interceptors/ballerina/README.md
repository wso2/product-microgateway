# Sample Interceptor - Ballerina

The interceptor service is generated using the interceptor service Open API definition and `bal openapi` command.
```sh
bal new <SERVICE_NAME>
bal openapi -i <INTERCEPTOR_SERVICE_OPEN_API> -o <SERVICE_NAME> --mode service
```

To learn about interceptors and for instructions on how to run it please head over to the
[home directory of interceptor samples](../README.md#samples).

## Prerequisites
- Docker
- Ballerina Swan Lake Beta 3

## Build the sample from source

1. Build the sample interceptor service.
   ```sh
   bal build --cloud=docker cc-sample-xml-interceptor/
   ```
   Here, the requestBody is Base64 encoded.

2. Start the interceptor service.
   ```sh
   docker run --name interceptor-bal -p 9081:9081 wso2am/cc-sample-xml-interceptor-ballerina:v1.0.0
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
   {"headersToAdd":{"x-user":"admin"}, "headersToReplace":{"content-type":"application/xml"}, "body":"PG5hbWU+VGhlIFByaXNvbmVyPC9uYW1lPg=="}
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
   docker rm -f interceptor-bal
   ```
