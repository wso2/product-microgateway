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

    -   Trains Service
        ```sh
        curl http://localhost:8080/trains-service/v1/trains
        
        curl http://localhost:8080/trains-service/v1/trains -H "Content-Type:application/json" \
            -d '{
                    "numberOfCarriage": 10,
                    "imageURL": "https://abc.train.org/resources/image/98215.png",
                    "engineModel": "TigerJet",
                    "facilities": "WiFi"
                }'

        curl http://localhost:8080/trains-service/v1/trains/5 -X PUT -H "Content-Type:application/json" \
            -d '{
                    "numberOfCarriage": 10,
                    "imageURL": "https://abc.train.org/resources/image/98215-new-image-441152335.png",
                    "engineModel": "TigerJet",
                    "facilities": "WiFi"
                }'
        
        curl http://localhost:8080/trains-service/v1/trains/5
        ```

    -   Schedules Service
        ```sh
        curl http://localhost:8081/schedules-service/v1/schedules
        
        curl http://localhost:8081/schedules-service/v1/schedules/2
        ```

## Build Images

-   Trains Service
    ```sh
    bal build --cloud=docker trains-service
    ```

-   Schedules Service
    ```sh
    bal build --cloud=docker schedules-service
    ```
