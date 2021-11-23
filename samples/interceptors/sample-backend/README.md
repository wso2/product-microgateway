# Sample Backend

This sample backend represent a Legacy XML Backend which requires a header `x-user` with the value `admin` for
successfully process the request. Otherwise, it responds with `401` status code.

When it successfully inserted a book it responds with `200` status code with a `text/plain` content. 

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
      -H 'x-user: admin' -v
    ```
   
   Remove the container
   ```shell
   docker rm -f lagacy-backend
   ```
