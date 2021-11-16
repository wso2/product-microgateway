# Sample for Custom Filters

Filters is a set of execution points in the request flow that intercept the request before it goes to the
backend service. They are engaged while the request is processed within the enforcer. The defined set of filters
are applied to all the APIs deployed in the Choreo-Connect. And these filters are engaged inline and if the request
fails at a certain filter, the request will not be forwarded to the next filter and the backend.
The inbuilt set of filters are the authentication filter and the throttling filter.

In this sample, it would read a property (`CustomProperty`) set at the config.toml under the filter and 
set the value as a header for each request.

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
className = "org.example.tests.CustomFilter"
# Position of the filter within final filter-chain
position = 3
# Custom Configurations
[enforcer.filters.configProperties]
CustomProperty = "foo"
```

Deploy the choreo-connect distribution and the filter would be engaged in Runtime. (Execute `docker-compose up -d` from
either `docker-compose/choreo-connect` directory or `docker-compose/choreo-connect-with-apim` directory)
