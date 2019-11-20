# WSO2 API Microgateway
[![Build Status](https://wso2.org/jenkins/job/products/job/product-microgateway/badge/icon)](https://wso2.org/jenkins/view/All%20Builds/job/products/job/product-microgateway)

The WSO2 API Microgateway is a Cloud Native API Gateway which can be used to expose one or many microservices as APIs.

Here is a short summary of the features it hosts.

1. Exposing one or more microservices as APIs using the Open API Specification.
1. Authentication and Authorization of API requests based on OAuth2.0 (opaque tokens and JWTs), Basic Auth and Mutual TLS.
1. Rate Limiting of API requests based on numerous policies.
1. Business Insights through API Analytics.
1. Service discovery features.
1. Request and Response transformations.
1. Load balancing, failover and circuit breaking capabilities of API requests.
1. Seamless integration with Docker and Kubernetes.
1. Integration with WSO2 API Manager to support design first APIs, API Analytics and shared rate limiting. 
1. Grouping APIs by labels. 

It also has the following characteristics that makes it a perfect fit for microservice architectures

1. Less than 1s startup time, allowing for faster scaling.
1. Built on a stateless architecture, allowing for infinite scaling.
1. Has an immutable runtime, making it heavily robust.
1. Easy integration with CI/CD processes and tools.
1. Runs in isolation with no dependencies to other components

#### Table of Contents


   * [Why WSO2 API Microgateway](#why-wso2-api-microgateway)
   * [Microgateway quick start](#microgateway-quick-start)
   * [Features](#features)
   * [Microgateway Components](#microgateway-components)
   * [Architecture](#architecture)
   * [Running the microgateway](#running-the-microgateway)
      * [Initializing a microgateway project](#initializing-a-microgateway-project)
      * [Building the microgateway project](#building-the-microgateway-project)
      * [Running the microgateway](#running-the-microgateway)
   * [WSO2 API Microgateway commands](#wso2-api-microgateway-commands)
      * [Init](#init)
      * [Build](#build)
   * [Project Structure](#project-structure)
   * [How to run the microgateway distribution](#how-to-run-the-microgateway-distribution)
   * [Invoke API exposed via microgateway](#invoke-api-exposed-via-microgateway)
   * [Microgateway supported open API extensions](#microgateway-supported-open-api-extensions)
   * [Microgateway open API extension usages](#microgateway-open-api-extension-usages)
      * [1. Override endpoint per API resource](#1-override-endpoint-per-api-resource)
      * [2. Add API/resource level request and response interceptors](#2-add-apiresource-level-request-and-response-interceptors)
      * [3. Add API/resource level throttling policies](#3-add-apiresource-level-throttling-policies)
      * [4. Add API level CORS configuration](#4-add-api-level-cors-configuration)
      * [5. Define backend security parameters](#5-define-backend-security-parameters)
      * [6. Override backend service connection URLS](#6-override-backend-service-connection-urls)
      * [7. Disable security for resources](#7-disable-security-for-resources)
      * [8. Override API Authorization Header](#8-override-api-authorization-header)
   * [Microgateway securing APIs](#microgateway-securing-apis)
   * [Import APIs from WSO2 API Manager](#import-apis-from-wso2-api-manager)


#### Why WSO2 API Microgateway
Microservices have become the norm for modern application architecture. Workloads of modern applications are spread 
across many groups of microservices, cloud services and legacy services. The characteristics and behaviors of such 
heterogeneous services have a massive diversity. Such as authentication mechanisms, message formats, high availability 
factors and so on.
The WSO2 API Microgateway is designed to expose heterogeneous microservices as APIs to end consumers using a common API 
interface based on the Open API Specification. This helps expose microservices using a unified interface to external 
consumers, internal consumers and partners. It applies the common quality of service attributes on API requests such as 
security, rate limiting and analytics and also offers a wide range of features which helps organizations to deploy APIs 
microservice architectures efficiently.

#### Microgateway quick start
Let's host our first API on a Microgateway.

1. First download the microgateway toolkit related to latest release from the product [release page](https://github.com/wso2/product-microgateway/releases) 
and extract it to a folder of your choice.

1. Using your command line client tool add the 'bin' directory of the extracted folder to your PATH variable.
```
export PATH=$PATH:<TOOLKIT_EXTRACTED_LOCATION>/bin
```

3. We are now ready to execute the Microgateway toolkit commands to initialize and build our Microgateway. Let's create 
our first project with name "petstore". You can do that by executing the following command using your command line tool.
```
micro-gw init petstore
```

4. The project is now initialized. You should notice a directory with name "petstore" being created in the location 
where you executed the command. Next, lets download and copy the OAS (Open API Specification) document of our Petstore 
API into our project directory. To do that, download the [open API definition](samples/petstore_basic.yaml) file and 
copy it to the **api_definitions** directory inside the "petstore" directory.

If you open the OAS document of the Petstore API using a text editor you will notice the resource (path) definitions 
of the API following the standard Open API Specification. You will also see the target server (back-end) URL of the API under 
the "x-wso2-production-endpoints" OAS vendor extension. We use this interface definition and the target server URL to 
generate a gateway proxy for our Petstore API.
 
5. Next, use your command line tool navigate back to where the project directory ("petstore") was created and execute 
the following command to build the project.
```
micro-gw build petstore
```

Once the build is successful the executable file of our project will be created inside the target directory of the 
petstore project.

6. We can now use the Docker image of our Microgateway runtime to run our project. You will need to have Docker 
installed to execute the steps below. In case you do not have Docker you can still run the Microgateway using the 
standard VM by following the steps mentioned in the section [How to run the microgateway distribution](#How-to-run-the-microgateway-distribution).

Execute the command below to run the Microgateway for our Petstore project.

```
docker run -d -v <PROJECT_TARGET_PATH>:/home/exec/ -p 9095:9095 -p 9090:9090 -e project="petstore"  wso2/wso2micro-gw:latest

<PROJECT_TARGET_PATH> - The path of the target directory created inside the project directory

Note: We actually need to mount the file created with .balx extension into the docker imgage. The target directory 
contains other generated files not required for docker image. So we can copy the .balx file to separate directory and 
mount that directory as well
```

The above will expose an https endpoint on port 9095. The context of the API will be "/petstore/v1".

7. The next step would be to invoke the API using a REST tool. Since APIs on the Microgateway are by default secured 
using OAuth2.0 we first need a valid OAuth 2.0 access token to invoke this API. Execute the command below to set a self 
contained OAuth2.0 access token in the JWT format as a variable on your terminal session. This token has been signed 
using the default private key of the WSO2 API Manager. When you are deploying the Microgateway in production note to 
change its default certificates.

```
TOKEN=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsIng1dCI6Ik5UQXhabU14TkRNeVpEZzNNVFUxWkdNME16RXpPREpoWldJNE5ETmxaRFUxT0dGa05qRmlNUSJ9.eyJhdWQiOiJodHRwOlwvXC9vcmcud3NvMi5hcGltZ3RcL2dhdGV3YXkiLCJzdWIiOiJhZG1pbiIsImFwcGxpY2F0aW9uIjp7ImlkIjoyLCJuYW1lIjoiSldUX0FQUCIsInRpZXIiOiJVbmxpbWl0ZWQiLCJvd25lciI6ImFkbWluIn0sInNjb3BlIjoiYW1fYXBwbGljYXRpb25fc2NvcGUgZGVmYXVsdCIsImlzcyI6Imh0dHBzOlwvXC9sb2NhbGhvc3Q6OTQ0M1wvb2F1dGgyXC90b2tlbiIsImtleXR5cGUiOiJQUk9EVUNUSU9OIiwic3Vic2NyaWJlZEFQSXMiOltdLCJjb25zdW1lcktleSI6Ilg5TGJ1bm9oODNLcDhLUFAxbFNfcXF5QnRjY2EiLCJleHAiOjM3MDMzOTIzNTMsImlhdCI6MTU1NTkwODcwNjk2MSwianRpIjoiMjI0MTMxYzQtM2Q2MS00MjZkLTgyNzktOWYyYzg5MWI4MmEzIn0=.b_0E0ohoWpmX5C-M1fSYTkT9X4FN--_n7-bEdhC3YoEEk6v8So6gVsTe3gxC0VjdkwVyNPSFX6FFvJavsUvzTkq528mserS3ch-TFLYiquuzeaKAPrnsFMh0Hop6CFMOOiYGInWKSKPgI-VOBtKb1pJLEa3HvIxT-69X9CyAkwajJVssmo0rvn95IJLoiNiqzH8r7PRRgV_iu305WAT3cymtejVWH9dhaXqENwu879EVNFF9udMRlG4l57qa2AaeyrEguAyVtibAsO0Hd-DFy5MW14S6XSkZsis8aHHYBlcBhpy2RqcP51xRog12zOb-WcROy6uvhuCsv-hje_41WQ==
``` 

8. We can now invoke the API running on the Microgateway using cURL as below.
```
curl -X GET "https://localhost:9095/petstore/v1/pet/findByStatus?status=available" -H "accept: application/xml" -H "Authorization:Bearer $TOKEN" -k


curl -X GET "https://localhost:9095/petstore/v1/pet/1" -H "accept: application/xml" -H "Authorization:Bearer $TOKEN" -k
```


#### Features
- **Authentication** : Supports mutual TLS, Oauth2(opaque tokens and JWT) and basic authentication
- **Rate limiting** : Throttle the request to the APIs based on the request count for a give time period
- **Transformations** : Manipulate API request and response using interceptor functions
- **Analytics** : Publish data to streams
- **Service Discovery** : Resolve endpoints using third party distributed key value stores like etcd
- **Cloud native** : A lightweight gateway that can be run on any platform(bare metal, docker and k8s)
- **Scalable** : Distributed nature allows to scale horizontally.

#### Microgateway Components
- **Toolkit** : The toolkit is used to initiate microgateway projects. Once the project is initialized API developer can
add(copy) open API definitions of the APIs to the  project or import APIs from WSO2 API Manager. Once the all the APIs are added the toolkit can be used
to build the project and create and executable file.

- **Runtime** : The gateway run time can expose the APIS and servees the API requests. The executable output of the toolkit should be provided as an input when running the microgateway runtime.
Then this run time will expose all the APIs which were included in the particular project which used to create the executable file

#### Architecture

The following diagram illustrates how the WSO2 API Microgateway expose micro services using Open API definition.

![Alt text](architecture-new.png?raw=true "Title")

###### Dev Phase

* API developer creates a WSO2 API Microgateway project using a WSO2 API Microgateway controller(toolkit)
* Adds the open API definitions of microservices into the project
* Developer defines endpoints and interceptors for the api/resources using the definition.yaml inside the project
* Builds the project and generates executables, images and k8s artifacts

#### Running the microgateway

Running the WSO2 API Microgateway is a 3 step process. The first two steps are involved in building the executable using the toolkit and the last
step is to run that executable file using the microgateway runtime component.

 1. Initializing a WSO2 API Microgateway project.
 1. Building the WSO2 API Microgateway project and creating an executable file
 1. Running the WSO2 API Microgateway distribution.

##### Initializing a microgateway project

Initializing a WSO2 API Microgateway project creates the default directory structure at the location where the command is run.
Empty `api_definitions` directory will be created inside the root project directory. API developer can add multiple open API definitions inside the
api_definitions file and define endpoints and interceptors for the resources  by adding open API extensions.
API developer can specify the  back end endpoint details, request and response interceptors, throttle policies, CORS config and etc using open API
vendor specific extensions.


##### Building the microgateway project

Once the project has been created, the next step is to build the project sources. This output of this operation is a
executable file(.balx) which later provided as an input to the runtime

##### Running the microgateway
The output(.balx file) of toolkit build process is used to run the microgateway runtime component.

#### WSO2 API Microgateway toolkit commands

Following are the set of commands included within the WSO2 API Microgateway.

Note: Before you execute any of the commands below you need to add the path to the <micro-gw-home>/bin directory to the PATH environment variable. Ex: /home/dev/wso2am-micro-gw/bin

##### Init

`$ micro-gw init <project_name>`

The "micro-gw init" command is used to initialize a project structure with artifacts required in generating a WSO2 API Microgateway distribution. This will create a **api_definitions**  directory.

* **api_defintions** - API developer should copy all the open API definitions of microservices inside this directory

If the project already exists, a warning will be prompted requesting permission to override existing project.

Execute `micro-gw help init` to get more detailed information regarding the setup command.

Example


    $ micro-gw init petstore

Let's see how we can expose the [petstore swagger](samples/petstore_swagger3.yaml) using the micro-gw.

Let's add the basic microgateway Open API extension to the petstore OAS file.


```
x-wso2-basePath: /petstore/v1
x-wso2-production-endpoints:
  urls:
  - https://petstore.swagger.io/v2

```

Sample for petstore OAS file with two resources and extensions can be found [here](samples/petstore_basic.yaml)

##### Build

`$ micro-gw build <project_name>`

Upon execution of this command, the WSO2 API Microgateway CLI tool will build the executable file for the specified project.

Execute `micro-gw help build` to get more detailed information regarding the build command.

Example

	$ micro-gw build petstore

#### Project Structure

Following is the structure of a project generated when running micro-gw init command.

```
petstore/
├── api_definitions
├── conf
│   └── deployment-config.toml
├── gen
│   ├── api_definitions
├── interceptors
├── policies.yaml
└── target

```


#### How to run the microgateway distribution

Once the **init, build** commands are executed, an executable file with extension .balx will be created under target directory inside the project.

```
../petstore/target$ ls
petstore.balx
```

Then use the microgateway runtime component to run this executable file.
* Go to the <MG_RUNTIME_HOME>/bin directory and execute the following command

`$ bash gateway <path_to_the_excutable_file>`

```
micro-gw-internal/bin$ bash gateway /home/user/petstore/target/petstore.balx
ballerina: initiating service(s) in '/home/user/petstore/target/petstore.balx'
ballerina: started HTTPS/WSS endpoint localhost:9095
ballerina: started HTTP/WS endpoint localhost:9090
ballerina: started HTTPS/WSS endpoint localhost:9096
```

#### Invoke API exposed via microgateway
Once APIs are exposed we can invoke API with a valid jwt token or an opaque access token.
In order to use jwt tokens microgateway should be presented with  a jwt signed by a trusted OAuth2 service. There are few ways we can get a jwt token

1. Any third party secure token service
The public certificate of the token service which used to sign the token should be added to the trust store of the microgateway.
The jwt should have the claims **sub, aud, exp** in order to validate with microgateway

1. Get jwt from WSO2 API Manager
Please refer the [documentation](https://docs.wso2.com/display/AM260/Generate+a+JWT+token+from+the+API+Store) on how to get a valid jwt

The following sample command can be used to invoke the "/pet/findByStatus" resource of the petstore API

```
curl -X GET "https://localhost:9095/petstore/v1/pet/findByStatus?status=available" -H "accept: application/xml" -H "Authorization:Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsIng1dCI6Ik5UQXhabU14TkRNeVpEZzNNVFUxWkdNME16RXpPREpoWldJNE5ETmxaRFUxT0dGa05qRmlNUSJ9.eyJhdWQiOiJodHRwOlwvXC9vcmcud3NvMi5hcGltZ3RcL2dhdGV3YXkiLCJzdWIiOiJhZG1pbiIsImFwcGxpY2F0aW9uIjp7ImlkIjoyLCJuYW1lIjoiSldUX0FQUCIsInRpZXIiOiJVbmxpbWl0ZWQiLCJvd25lciI6ImFkbWluIn0sInNjb3BlIjoiYW1fYXBwbGljYXRpb25fc2NvcGUgZGVmYXVsdCIsImlzcyI6Imh0dHBzOlwvXC9sb2NhbGhvc3Q6OTQ0M1wvb2F1dGgyXC90b2tlbiIsImtleXR5cGUiOiJQUk9EVUNUSU9OIiwic3Vic2NyaWJlZEFQSXMiOltdLCJjb25zdW1lcktleSI6Ilg5TGJ1bm9oODNLcDhLUFAxbFNfcXF5QnRjY2EiLCJleHAiOjM3MDMzOTIzNTMsImlhdCI6MTU1NTkwODcwNjk2MSwianRpIjoiMjI0MTMxYzQtM2Q2MS00MjZkLTgyNzktOWYyYzg5MWI4MmEzIn0=.b_0E0ohoWpmX5C-M1fSYTkT9X4FN--_n7-bEdhC3YoEEk6v8So6gVsTe3gxC0VjdkwVyNPSFX6FFvJavsUvzTkq528mserS3ch-TFLYiquuzeaKAPrnsFMh0Hop6CFMOOiYGInWKSKPgI-VOBtKb1pJLEa3HvIxT-69X9CyAkwajJVssmo0rvn95IJLoiNiqzH8r7PRRgV_iu305WAT3cymtejVWH9dhaXqENwu879EVNFF9udMRlG4l57qa2AaeyrEguAyVtibAsO0Hd-DFy5MW14S6XSkZsis8aHHYBlcBhpy2RqcP51xRog12zOb-WcROy6uvhuCsv-hje_41WQ==" -k
```
Please note that the jwt provided in the command is a jwt token retrieved from WSO2 API Manager with higher expiry time which can be used with any API not protected with scopes.
This token works with any API since by default the microgateway config uses the public certificate of WSO2 API Manager to validate the signature.

### Microgateway supported open API extensions
| Extension                     | Description                                               | Required/Not Required |
| -------------                 | -------------                                             | ----------------------|
| x-wso2-basePath                | Base path which gateway exposes the API                   | **Required** -> API level only
| x-wso2-production-endpoints    | Specify the actual back end of the service                | **Required** -> API level/ Not required -> Resource level
| x-wso2-sandbox-endpoints       | Specify the sandbox endpoint of the service if available  | Not Required -> API/Resource level
| x-wso2-throttling-tier         | Specify the rate limiting for the API or resource         | Not Required -> API/Resource level
| x-wso2-cors                    | Specify CORS configuration for the API                    | Not Required -> API level only
| x-wso2-endpoints               | Define endpoint configs globally which can be then referred with  x-wso2-production-endpoints or x-wso2-sandbox-endpoints extensions | Not Required
| x-wso2-disable-security        | Resource can be invoked without any authentication        | Not Required -> Resource level only
| x-wso2-request-interceptor     | Custom ballerina functions can be written in order to do transformations before dispatching the request      | Not Required -> API/Resource level
| x-wso2-response-interceptor    | Custom ballerina functions can be written in order to do transformations before dispatching the response     | Not Required -> API/Resource level
| x-wso2-auth-header             | Specify the authorization header for the API in which either bearer or basic token is sent                   | Not Required -> API level only


### Microgateway open API extension usages
#### 1. Override endpoint per API resource
API developer can specify endpoints per resource by adding the **x-wso2-production-endpoints** extension under the respective resource in open API definition.
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
   x-wso2-production-endpoints:
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
   x-wso2-production-endpoints:
     urls:
     - http://www.mocky.io/v2/5cd28b9a310000bf293397f9

```

Complete sample can be found [here](samples/per_resource_endpoint.yaml)

#### 2. Add API/resource level request and response interceptors
Interceptors can be used to do request and response transformations and mediation. Request interceptors are engaged before sending the request to the back end and
response interceptors are engaged before responding to the client.
API developer can write his own request and response interceptors using ballerina and add it to the project and define them in the open API definition using extensions

In the sample below user can write the validateRequest and validateResponse methods in ballerina and add it to the `interceptors` directory inside the project. This interceptor will only be engaged for that particular resource only
```
paths:
  "/pet/findByStatus":
    get:
      tags:
      - pet
      summary: Finds Pets by status
      description: Multiple status values can be provided with comma separated strings
      operationId: findPetsByStatus
      x-wso2-request-interceptor: validateRequest
      x-wso2-response-interceptor: validateResponse
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
x-wso2-basePath: /petstore/v1
x-wso2-request-interceptor: validateRequest
x-wso2-response-interceptor: validateResponse
x-wso2-production-endpoints:
  urls:
  - https://petstore.swagger.io/v2
```

Sample open API definition for interceptors can be found [here](samples/interceptors_sample.yaml).
#### 3. Add API/resource level throttling policies
API developer can specify the rate limiting policies for each resource or globally for the API. These policies should be defined in the policies.yaml file in the project directory
A default set of policies are already available, but users can add more policies to the file and later refer them by name in the open API definition
Following samples show how throttling policies can be added to an API
```
x-wso2-basePath: /petstore/v1
x-wso2-throttling-tier: 10kPerMin
x-wso2-production-endpoints:
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
      x-wso2-throttling-tier: 20kPerMin
```
Complete sample can be found [here](samples/policies_sample.yaml)

#### 4. Add API level CORS configuration

CORS configurations can be added to each API using the open API extension **x-wso2-cors**
```
x-wso2-basePath: /petstore/v1
x-wso2-production-endpoints:
  urls:
  - https://petstore.swagger.io/v2
x-wso2-cors:
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

#### 5. Define backend security parameters
There might be occasions where the actual back end service of the API might be protected using
basic authentication. In those scenarios we need to send the basic authentication parameters(username and password) to the back end service.
We can specify the endpoint security parameters in the openAPI definition using extensions.
The supported way to define endpoint security is to define endpoints under the **x-wso2-endpoints** parameter and then refer them in the API level or resource level endpoint.
When we define the endpoint under extension "x-wso2-endpoints" then endpoint should have a name. This name(myEndpoint in below sample) is used to pass the password when running the microgateway
Under the endpoint config we can define security parameters as below

```
securityConfig:
      type: basic
      username: rajith
```

```
x-wso2-basePath: /petstore/v1
x-wso2-production-endpoints: "#/x-wso2-endpoints/myEndpoint"
paths:
  "/pet/findByStatus":
    get:
      tags:
      - pet
      summary: Finds Pets by status
      description: Multiple status values can be provided with comma separated strings
      operationId: findPetsByStatus
      x-wso2-production-endpoints: "#/x-wso2-endpoints/myEndpoint3"
      .
      .
      .
      .


x-wso2-endpoints:
 - myEndpoint:
    urls:
    - https://petstore.swagger.io/v2
    - https://petstore.swagger.io/v5
    securityConfig:
      type: basic
      username: roshan
 - myEndpoint3:
    urls:
    - https://petstore.swagger.io/v3
    - https://petstore.swagger.io/v4
    securityConfig:
      type: basic
      username: rajith

```

Complete sample can be found [here](samples/endpoint_by_reference_sample.yaml)

When running the microgateway we can provide the password as an environment variable.
The variable format is **\<epName\>_\<epType\>_basic_password**
- epName : Name specified in the open API definition under x-wso2-endpoints
- epType : either prod or sand
So the complete command for the above sample is like
```
bash gateway -e myEndpoint3_prod_basic_password=123456 <path_to_the_excutable_file>
```

#### 6. Override backend service connection URLS
There can be use cases where we want to override the back end connection url provided in the open API definition
during the run time. We can override endpoints that are used as references similar to previous topic.
In order to override the endpoint url we need to define endpoint in **x-wso2-endpoints** extension and refer them in the API level or resource level.
Let's use the same example we have used in previous topic. So we can override the *myEndpoint3* url during the runtime as follows.
The variable format is **\<epName\>\_\<epType\>\_endpoint_\<epIndex\>**
- epName : Name specified in the open API definition under x-wso2-endpoints
- epType : either prod or sand
- epIndex : Index starting from 0. If there are many URLS(load balanced or fail over) we can override them using indexes 1,2,3 and etc
So the complete command for the above sample is like

```
bash gateway -e myEndpoint3_prod_endpoint_0=<new back end url> <path_to_the_excutable_file>
```

#### 7. Disable security for resources
By default the APIs and resources are protected via oauth2 in microgateway. API consumer need a valid oauth2 access token(jwt or opaque)
to invoke the APIs. But API developer can expose APIs without any authentication using the open API extension **x-wso2-disable-security**.
This extension is only supported at resource level only

```
paths:
  "/pet/findByStatus":
    get:
      tags:
      - pet
      summary: Finds Pets by status
      description: Multiple status values can be provided with comma separated strings
      operationId: findPetsByStatus
      x-wso2-disable-security: true
```

#### 8. Override API Authorization Header
By default the APIs exposed via microgateway requires a valid token sent with "Authorization" header. By specifying the
"x-wso2-auth-header" users can set custom header name for this.

```
x-wso2-auth-header: Authx
```
#### Microgateway securing APIs
The gateway supports the "securitySchemes" keyword in open API specifications.
Currently microgateway supports oauth2 and basic authentication for APIs which can be defined via open API extensions.
If none of the securitySchemes are defined the gateway by default applies oauth2 security.

1. Define scopes for the resources and API using oauth2 security type.
```
securityDefinitions:
  petstore_auth:
    type: oauth2
    authorizationUrl: 'https://petstore.swagger.io/oauth/authorize'
    flow: implicit
    scopes:
      'write:pets': modify pets in your account
      'read:pets': read your pets
```

We can define oauth2 security type, and add scopes and refer the security scheme in API level or resource level

```
security:
    - petstore_auth:
        - 'write:pets'
        - 'read:pets'
```

2. Define basic authentication for the API
```
securityDefinitions:
  petstore_basic:
    type: basic
```

Complete sample can be found [here](samples/security_sample.yaml)

#### Import APIs from WSO2 API Manager
The published apis from [WSO2 API Manager](https://wso2.com/api-management/) can be exposed via microgateway as well.
We can import API from WSO2 API Manager by specifying the API name and version.
The **import** command of the toolkit can be used to fetch APIs.

First initialize the project using the command below.
```
micro-gw init pizza-api
```

Then import the API. The toolkit will prompt for API manager url, username and password of a valid user in API manager, trust store location and password of toolkit.
If url, trust store location and password is not provided default values will be used

```
micro-gw import -a <API-NAME> -v <API_VERSION> <PROJECT_NAME>

ex: micro-gw import -a PizzaShackAPI -v 1.0.0 pizza-api

$ micro-gw import -a PizzaShackAPI -v 1.0.0 pizza-api
Enter Username:
admin
Enter Password for admin:

Enter APIM base URL [https://localhost:9443]:

You are using REST version - v0.14 of API Manager. (If you want to change this, go to <MICROGW_HOME>/conf/toolkit-config.toml)
Enter Trust store location: [lib/platform/bre/security/ballerinaTruststore.p12]

Enter Trust store password: [ use default? ]

ID for API PizzaShackAPI : 48776504-9479-48c0-abd2-711ea0263ac9

```

Once imported the auto generated swagger will be inside the gen directory of the project. Project structure will be as follows.
```
pizza-api/
├── api_definitions
├── conf
│   └── deployment-config.toml
├── extensions
│   ├── extension_filter.bal
│   └── token_revocation_extension.bal
├── gen
│   └── api_definitions
│       └── 30e623704c5c5479b7c0d9ab78e965df02c1610401e37cbd557e6353e3191c76swagger.json
├── interceptors
├── policies.yaml
└── target
    └── gen
        └── internal.conf

```

This project can then be built and run using the same approaches we have discussed above. 
