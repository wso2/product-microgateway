# WSO2 API Microgateway

The WSO2 API Microgateway is a toolkit designed to create a specialized, lightweight, gateway distribution (WSO2 API Microgateway) by including a single API or a group of APIs. The WSO2 API Microgateway is able to serve the included APIs as soon as it is up and running.

In summary, the WSO2 API Microgateway is a specialized form of the WSO2 API Gateway with characteristics below:

1. Able to execute in isolation without mandatory connections to other components (Secure Token Service, Rate limiting component , Analytics).
1. Capable of exposing micro services directly from Open API definitions
1. Able to host a subset of APIs of choice (defined on the WSO2 API Manager's API Publisher) instead of all.
1. Immutable. The gateway runtime is immutable. If APIs or Policies change after the WSO2 API Microgateway has been built, a rebuild process is required to capture the changes.
1. Seamless integration with deployment automation tools and techniques.
1. Easy integration with CI/CD processes.

WSO2 API Microgateway acts as a proxy that is capable of performing security validations (Signed JWT, OAuth), in-memory (local) rate limiting and Analytics.

#### Architecture

The following diagram illustrates how the WSO2 API Microgateway expose micro services using Open API defintion.

![Alt text](architecture-new.png?raw=true "Title")

###### Dev Phase

* API developer creates a WSO2 API Microgateway project using a WSO2 API Microgateway controller(toolkit)
* Adds the open API definitions of microservices into the project
* Developer defines endpoints and interceptors for the api/resources using the definition.yaml inside the project
* Builds the project and generates executables, images and k8s artifacts

#### Running the microgateway

Running the WSO2 API Microgateway is a 3 step process. The first two steps are involved in building the runtime.

 1. Initializing a WSO2 API Microgateway project.
 1. Building the WSO2 API Microgateway project and creating a WSO2 API Microgateway distribution.
 1. Running the WSO2 API Microgateway distribution.

##### Initializing a WSO2 API Microgateway project

Initializing a WSO2 API Microgateway project creates the default folder structure at the location where the command is run.
Empty `api_definitions` folder will be created inside the main folder. API developer can add multiple open API definitions inside the
api_definitions file and define endpoints and interceptors for the resources  by adding open API extensions.
API developer can specify the  back end endpoint details, request and response interceptors, throttle policies, CORS config and etc using open API
vendor specific extensions.


##### Building the WSO2 API Microgateway project

Once the project has been created, the next step is to build the project sources.

#### WSO2 API Microgateway commands

Following are the set of commands included within the WSO2 API Microgateway.

Note: Before you execute any of the commands below you need to add the path to the <micro-gw-home>/bin directory to the PATH environment variable. Ex: /home/dev/wso2am-micro-gw/bin

##### Init

`$ micro-gw init`

The "micro-gw init" command is used to initialize a project structure with artifacts required in generating a WSO2 API Microgateway distribution. This will create a **api_definitions**  folder.

* **api_defintions** - API developer should copy all the open API definitions of microservices inside this folder

If the project already exists, a warning will be prompted requesting permission to override existing project.

Execute `micro-gw help setup` to get more detailed information regarding the setup command.

Example


    $ micro-gw init petstore-project

Lets see how we can expose the [petstore swagger](samples/petstore_swagger3.yaml) using the micro-gw
Lets define the basic microgateway open API extension in order to expose the API.


```
x-mgw-basePath: /petstore/v1
x-mgw-production-endpoints:
  urls:
  - https://petstore.swagger.io/v2

```

Sample for petstore open API definition with two resources and extensions can be found [here](samples/petstore_basic.yaml)

##### Build

`$ micro-gw build`

Upon execution of this command, the WSO2 API Microgateway CLI tool will build the micro gateway distribution for the specified project.

Execute `micro-gw help build` to get more detailed information regarding the build command.

Example

	$ micro-gw build petstore-project

#### Project Structure

Following is the structure of a project generated when running micro-gw init command.

```
petstore-project/
├── api_definitions
├── conf
│   └── deployment-config.toml
├── gen
│   ├── api_definitions
│   └── src
│       └── policies
├── interceptors
├── policies.yaml
└── target

```


#### How to run the microgateway distribution

Once the **init, build** commands are executed, a micro gateway distribution will be created under target folder.

```
../petstore-project/target$ ls
micro-gw-pizzashack-project.zip
```

* Unzip the micro-gw-petstore-project.zip and run the `gateway` script inside the bin folder of the extracted zip using below command.

`$ bash gateway `

```
micro-gw-internal/bin$ bash gateway
ballerina: initiating service(s) in '/home/user/Petstore-Project/target/micro-gw-pizzashack-project/exec/internal.balx'
ballerina: started HTTPS/WSS endpoint localhost:9095
ballerina: started HTTP/WS endpoint localhost:9090
ballerina: started HTTPS/WSS endpoint localhost:9096
```

### Micro gateway open API extension usages
#### 1. Override endpoint per API resource
API developer can specify endpoints per resource by adding the **x-mgw-production-endpoints** extension under the respective resource in open API definition.
If a specific resource have an endpoint which requires different back end rather than the global back end defined for the API, then it can be overridden as below.

In following example `/pet/findByStatus` resource endpoint is overridden with load balance endpoint and `pet/{petId}` resource overridden with another http endpoint

```

"/pet/findByStatus":
 get:
   tags:
   - pet
   summary: Finds Pets by status
   description: Multiple status values can be provided with comma separated strings
   operationId: findPetsByStatus
   parameters:
   - name: status
     in: query
     description: Status values that need to be considered for filter
     required: true
     explode: true
     schema:
       type: array
       items:
         type: string
         enum:
         - available
         - pending
         - sold
         default: available
   x-mgw-production-endpoints:
     urls:
     - http://www.mocky.io/v2/5cd28cd73100008628339802
     - https://petstore.swagger.io/v2


"/pet/{petId}":
 get:
   tags:
   - pet
   summary: Find pet by ID
   description: Returns a single pet
   operationId: getPetById
   parameters:
   - name: petId
     in: path
     description: ID of pet to return
     required: true
     schema:
       type: integer
       format: int64
   x-mgw-production-endpoints:
     urls:
     - http://www.mocky.io/v2/5cd28b9a310000bf293397f9

```

Complete sample can be found [here](samples/per_resource_endpoint.yaml)

#### 2. Add API/resource level request and response interceptors
Interceptors can be used to do request and response transformations and mediation. Request interceptors are engaged before sending the request to the back end and
response interceptors are engaged before responding to the client.
API developer can write his own request and response interceptors using ballerina and add it to the project and define them in the open API definition using extensions

In the sample below user can write the validateRequest and validateResponse methods in ballerina and add it to the `interceptors` folder inside the project. This interceptors will only be enagged for that particular resource only
```
paths:
  "/pet/findByStatus":
    get:
      tags:
      - pet
      summary: Finds Pets by status
      description: Multiple status values can be provided with comma separated strings
      operationId: findPetsByStatus
      x-mgw-request-interceptor: validateRequest
      x-mgw-response-interceptor: validateResponse
      parameters:
      - name: status
        in: query
        description: Status values that need to be considered for filter
        required: true
        explode: true
        schema:
          type: array
          items:
            type: string
            enum:
            - available
            - pending
            - sold
            default: available
```

Sample validateRequest method can be implemented as below.
```
import ballerina/io;
import ballerina/http;

public function validateRequest (http:Caller outboundEp, http:Request req) {
    io:println("Request is intercepted.");
}
```

Sample validateResponse method can be implemented as below.
```
import ballerina/io;
import ballerina/http;

public function validateResponse (http:Caller outboundEp, http:Response res) {
    io:println("Response is intercepted.");
}
```

Similarly the API developer can add interceptors globally at the API level as well
```
openapi: 3.0.0
  version: 1.0.0
  title: Swagger Petstore New
  termsOfService: http://swagger.io/terms/
x-mgw-basePath: /petstore/v1
x-mgw-request-interceptor: validateRequest
x-mgw-response-interceptor: validateResponse
x-mgw-production-endpoints:
  urls:
  - https://petstore.swagger.io/v2
```

Sample open API definition for interceptors can be found [here](samples/interceptors_sample.yaml).
#### 3. Add API/resource level throttling policies
API developer can specify the rate limiting policies for each resource or globally for the API. These policies should be defined in the policies.yaml file in the project directory
By default set of policies are available, but user can add more policies to the file and later refer them by name in the open API definition
Following samples show how throttling policies can be added to API level
```
x-mgw-basePath: /petstore/v1
x-mgw-throttling-tier: 10kPerMin
x-mgw-production-endpoints:
  urls:
  - https://petstore.swagger.io/v2
```
The throttle policy "10kPerMin" is defined in the policies.yaml of the project as below.
```
- 10kPerMin:
     count: 10000
     unitTime: 1
     timeUnit: min
```

Resource level throttle policies also can be defined as well.
```
paths:
  "/pet/findByStatus":
    get:
      tags:
      - pet
      summary: Finds Pets by status
      description: Multiple status values can be provided with comma separated strings
      operationId: findPetsByStatus
      x-mgw-throttling-tier: 20kPerMin
```
Complete sample can be found [here](samples/policies_sample.yaml)

#### 4. Add API level CORS configuration

CORS configurations can be added to each API using the open API extension **x-mgw-cors**
```
x-mgw-basePath: /petstore/v1
x-mgw-production-endpoints:
  urls:
  - https://petstore.swagger.io/v2
x-mgw-cors:
  access-control-allow-origins:
      - test.com
      - example.com
  access-control-allow-headers:
      - Authorization
      - Content-Type
  access-control-allow-methods:
      - GET
      - PUT
      - POST
```
Complete sample can be found [here](samples/cors_sample.yaml)