# Guide to generate REST API Interface of the adapter

This guide provides information about introducing changes to the REST API Interface between the API Controller and Adapter. If the any API Level changes are required to be done, it should be added to the `adapter/resources/adapterAPI.yaml`. Then we autogenerate the code from go-swagger tool.

First, You should install the go-swagger in your developer environment.

https://goswagger.io/install.html

Navigate to the `adapter` directory. Execute the following command. 

```
swagger generate server -f resources/adapterAPI.yaml -A restapi -P models.Principal -s restserver -m models -t internal/api -r resources/license.txt --exclude-main
```

Once it is executed, `adapter/internal/api/models` and `adapter/internal/api/restserver` packages will be populated. Within the `restserver` directory, there is a file called `configure_restapi.go`. All the code level changes introduced should be included in here. (Authentication logic, additional validations, TLS level configurations etc.)

If you need to have the Main file (for referenece purposes), you should execute the above command without the `--exclude-main` flag.
