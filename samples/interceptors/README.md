## Choreo Connect
CD to CC
```sh
docker compose up
```

CD to here
```sh
docker compose up
```

```sh
apictl init bookstore --oas api.yaml
mkdir -p bookstore/Endpoint-certificates/interceptors/
cp certs/interceptor.crt Endpoint-certificates/interceptors/
apictl mg login dev -u admin -p admin -k
apictl mg deploy api -f bookstore -e dev -k

TOKEN=$(curl -X POST "https://localhost:9095/testkey" -d "scope=read:pets" -H "Authorization: Basic YWRtaW46YWRtaW4=" -k -v)
```

```sh
curl "https://localhost:9095/abc-stores/books" -H "accept: application/xml" -H "Authorization:Bearer $TOKEN" -H 'user: john' -d '{"name":"The Prisoner"}' -k
```

```sh
apictl mg undeploy api -n Book-Store -v v1 -e dev -k -g Default
```

## Build and Test

bal build --offline --cloud=docker cc-legacy-xml-backend-sample/
bal build --offline --cloud=docker cc-xml-interceptor-sample/


curl http://localhost:9080/books -d '<foo/>' -H 'user: john' -v
curl https://localhost:9081/api/v1/handle-request -d '{"requestBody": "eyJuYW1lIjoiVGhlIFByaXNvbmVyIn0K"}' --cert certs/mg.pem --key /Users/renuka/git/product-microgateway/resources/security/mg.key --cacert certs/interceptor.crt
