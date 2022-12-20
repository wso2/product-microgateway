# Rate Limiter

## Dev Guid

### Health Check

Execute the following command to check the health of the rate limiter service.

```sh
curl http://localhost:8090/healthcheck
```

### Config Dump

Execute the following command to get the config dump of the rate limit service.

```sh
curl http://localhost:6070/rlconfig
```

### Do changes to the Rate Limiter service

1.  If changes are applicable to the upstream, please create a pull request on https://github.com/envoyproxy/ratelimit/.
2.  Create a pull request on the folk https://github.com/renuka-fernando/ratelimit/tree/choreo (choreo branch).
3.  Update git sub module.
    ```sh
    cd rate-limiter-service
    git pull <folk_repo> choreo
    git commit -m <commit_message>
    ```
4.  Create a pull request on https://github.com/wso2/product-microgateway

