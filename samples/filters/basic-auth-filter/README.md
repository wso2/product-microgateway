# Sample for a Basic Authentication Filter

Filters is a set of execution points in the request flow that intercept the request before it goes to the
backend service. They are engaged while the request is processed within the enforcer. The defined set of filters
are applied to all the APIs deployed in the Choreo-Connect. And these filters are engaged inline and if the request
fails at a certain filter, the request will not be forwarded to the next filter and the backend.
The inbuilt set of filters are the authentication filter and the throttling filter.

In this sample, it would read properties called (`HeaderName`), (`Username`) and (`Password`) set at the config.toml under the filter in which the HeaderName parameter will be used to obtain contents in the relevant header and the Username and Password parameters will be authenticated against the retrieved header contents which contains the Base64 encoded username and password.

# Building the Sample

Maven and JDK11 is required to build the project. 
```
mvn clean install
```

If you are using the docker-compose setup, copy the jar file to the `docker-compose/resources/enforcer/dropins` directory,
inside the distribution. If you are using kubernetes, make sure that this jar is mounted to the `/home/wso2/lib/dropins`
directory of the enforcer container.

Update the config.toml.

```toml
[[enforcer.filters]]
# ClassName of the filter
className = "org.example.tests.BasicAuth"
# Position of the filter within final filter-chain
position = 1
# Custom Configurations
[enforcer.filters.configProperties]
HeaderName = "basicauth"
Username = "admin"
Password = "admin"
```
The filter will obtain the contents of the (`HeaderName`) parameter, retrieve the header with that particular name which will be authenticated against the (`Username`) parameter and the (`Password`) parameter stated above.

( i.e In this sample, the filter will obtain a header named `basicauth`, and it's contents will be authenticated against the username `admin` and password `admin`)


Deploy the choreo-connect distribution and the filter would be engaged in Runtime. (Execute `docker-compose up -d` from
either `docker-compose/choreo-connect` directory or `docker-compose/choreo-connect-with-apim` directory)

When Invoking an API add the Base64 encoded username and password in the format `username:password` to the header `basicauth`. 

eg : curl -X GET "https://localhost:9095/v2/pet/findByStatus?status=available" -H "accept: application/xml" -H "Authorization:Bearer $TOKEN" -H "basicauth: YWRtaW46YWRtaW4=" -k

(The Header which must be added to the required curl request is `-H "basicauth: YWRtaW46YWRtaW4="`. Here the string `admin:admin` encoded in Base64 is `YWRtaW46YWRtaW4=` which is included in the `basicauth` header)

