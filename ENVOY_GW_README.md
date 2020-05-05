# WSO2 Envoy Gateway
Experimental control plane server for envoy


## Steps to Start the server

1. ```go mod vendor```
2. ```go run main.go```

#### Start envoy

```docker-compose up```

### Usage
1. Put the swagger yaml content to the ```test.yaml``` file in ```resource/apis``` directory. It will be automatically deployed
 in envoy.
 
 
 ### TODO
 1. Implement the REST API to add, update and undeploy apis by providing the swagger file. 
 2. Define common configurations and implement them in the server.
 3. Support individual api configurations (add/ remove endpoints, resources etc)
 
 ...