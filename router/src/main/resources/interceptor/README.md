# Development Guide

1. Build the Choreo Connect from the root directory.
2. Extract the distribution and change dir to docker-compose dir.
3. Open an interceptor sample (e.g. NodeJS sample) do required changes, before build the sample append "-test" to the docker tag (to keep the original docker image).
4. Append following to the docker-compose file.
    ```yaml
    xml-interceptor:
    image: wso2am/cc-sample-xml-interceptor-nodejs:v1.0.0-test
    ports:
      - "9081:9081"
    ```
5. Add the volume mount to the router.
   ```yaml
   <ROOD_DIR>/router/src/main/resources/interceptor:/home/wso2/interceptor
   ```
   
6. Enable debug logs in Router. Add following to environment variables in Router.
   ```
   - TRAILING_ARGS=--component-log-level lua:debug
   ```
7. Add following x-wso2 extensions to the Open API file.
    ```yaml
    x-wso2-production-endpoints:
      urls:
      - https://httpbin.org/anything
    x-wso2-request-interceptor:
      includes:
      - request_body
      - request_headers
      - invocation_context
      serviceURL: https://xml-interceptor:9081
    x-wso2-response-interceptor:
      includes:
      - request_body
      - request_headers
      - invocation_context
      serviceURL: https://xml-interceptor:9081
    ```
8. Copy interceptor certs to the APICTL project (i.e. Endpoint-certificates/interceptors).
9. Start the docker-compose file.
