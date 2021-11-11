# Sample Backend

Sample Legacy XML Backend.

## Prerequisites
- Docker
- Ballerina Swan Lake Beta 3

## Build and Test the sample

1. Build the backend service.
    ```sh
    bal build --cloud=docker cc-sample-legacy-xml-backend/
    ```

2. Test the backend service.
   ```sh
   docker run --name lagacy-backend -p 9080:9080 wso2am/cc-sample-legacy-xml-backend:v1.0.0
   ```
   
   In another shell
    ```sh
    curl -X POST http://localhost:9080/books \
      -d '<name>The Prisoner</name>' \
      -H 'password: admin' -v
    ```
   
   Remove the container
   ```shell
   docker rm -f lagacy-backend
   ```
