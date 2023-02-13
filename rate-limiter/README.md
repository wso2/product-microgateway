# Rate Limiter

## Dev Guide

### Health Check

Execute the following command to check the health of the rate limiter service.

```sh
curl http://localhost:8090/healthcheck
```

### Config Dump

Execute the following command to get the config dump of the rate limit service.

```sh
curl http://localhost:6070/rlconfig -i
```

### Do changes to the Rate Limiter service

1.  If changes are applicable to the upstream, please create a pull request on https://github.com/envoyproxy/ratelimit/.
2.  After merging above created pull request create another pull request on https://github.com/wso2/product-microgateway considering the latest envoyproxy/ratelimit:<hashValue> docker image.
