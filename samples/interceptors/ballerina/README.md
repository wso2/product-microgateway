# Sample Interceptor - Ballerina

## Prerequisites
- Docker
- Ballerina Swan Lake Beta 3

## Build and Test the sample

1. Build the sample backend.

    ```sh
    bal build --cloud=docker cc-sample-legacy-xml-backend/
    ```
2. Create the interceptor certs

    ```sh
    bash ./create-interceptor-certs.sh
    ```

3. Build the sample interceptor service.

    ```sh
    bal build --cloud=docker cc-sample-xml-interceptor/
    ```
   Here, the requestBody is Base64 encoded.

4. Start the services.

    ```sh
    docker-compose up
    ```

5. Test the backend service.

    ```sh
    curl -X POST http://localhost:9080/books -d '<name>The Prisoner</name>' -H 'password: admin' -v
    ```

6. Test the interceptor service.

    ```sh
    curl https://localhost:9081/api/v1/handle-request \
      -d '{"requestBody": "eyJuYW1lIjoiVGhlIFByaXNvbmVyIn0K"}' \
      --cert certs/mg.pem \
      --key ../../resources/security/mg.key \
      --cacert certs/interceptor.crt
    ```