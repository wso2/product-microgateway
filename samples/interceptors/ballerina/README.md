# Sample Interceptor - Ballerina

## Prerequisites
- Docker
- Ballerina Swan Lake Beta 3

## Build and Test the sample

1. Build the sample interceptor service.
   ```sh
   bal build --cloud=docker cc-sample-xml-interceptor/
   ```
   Here, the requestBody is Base64 encoded.

2. Start the interceptor service.
   ```sh
   docker run --name bal-interceptor -p 9081:9081 wso2am/cc-sample-xml-interceptor:v1.0.0
   ```

3. Test the interceptor service.
   ```sh
   curl https://localhost:9081/api/v1/handle-request \
      -d '{"requestBody": "eyJuYW1lIjoiVGhlIFByaXNvbmVyIn0K"}' \
      --cert ../resources/certs/mg.pem \
      --key ../resources/mg.key \
      --cacert ../resources/certs/interceptor.pem 
   ```
   Sample response
   ```json
   {"headersToReplace":{"content-type":"application/xml"}, "body":"PG5hbWU+VGhlIFByaXNvbmVyPC9uYW1lPg=="}
   ```