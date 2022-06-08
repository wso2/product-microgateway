# Microservice

## Prerequisites

- Docker
- Ballerina 2201.1.0 (Swan Lake Update 1)

## Run in Local

1.  Run services.

    ```sh
    bal run trains-service
    ```

    ```sh
    bal run schedules-service
    ```

2.  Invoke services.

    ```sh
    curl http://localhost:8080/train-service/v1/trains
    curl http://localhost:8080/train-service/v1/trains -d '{"trainId":"2", "numberOfCarriage":10, "imageURL":"foo.com", "engineModel":"F", "facilities":"WiFi"}'
    curl http://localhost:8080/train-service/v1/trains/2
    ```

## Build and Test the sample

1.  Build the backend service.
    ```sh
    bal build --cloud=docker trains-service
    ```

2.  Test the backend service.
    ```sh
    docker run --rm --name trains-service -p 8080:8080 wso2am/cc-trains-service:1.0.0
    ```
   
   In another shell
    ```sh
    curl -X POST http://localhost:9080/books \
      -d '<name>The Prisoner</name>' \
      -H 'x-user: admin' -v
    ```
