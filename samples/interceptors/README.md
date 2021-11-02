# Interceptors Example

To learn about interceptors and for instructions on how to run it please head over to the
[Choreo-Connect docs](https://apim.docs.wso2.com/en/latest/deploy-and-publish/deploy-on-gateway/choreo-connect/message-transformation/message-transformation-overview/).

## Build and Test the sample

Build services.

```shell
bal build --cloud=docker cc-sample-legacy-xml-backend/
bal build --cloud=docker cc-sample-xml-interceptor/
```

Test services.

```shell
curl http://localhost:9080/books -d '<foo/>' -H 'user: john' -v
curl https://localhost:9081/api/v1/handle-request \
  -d '{"requestBody": "eyJuYW1lIjoiVGhlIFByaXNvbmVyIn0K"}' \
  --cert certs/mg.pem \
  --key ../../resources/security/mg.key \
  --cacert certs/interceptor.crt
```
