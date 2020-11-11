# WSO2 API Microgateway
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![License](https://img.shields.io/badge/slack-microgateway-blueviolet)](https://join.slack.com/t/wso2-apim/shared_invite/enQtNzEzMzk5Njc5MzM0LTgwODI3NmQ1MjI0ZDQyMGNmZGI4ZjdkZmI1ZWZmMjNkY2E0NmY3ZmExYjkxYThjNzNkOTU2NWJmYzM4YzZiOWU)

The WSO2 API Microgateway is a Cloud Native API Gateway which can be used to expose one or many microservices as APIs.


## Why WSO2 API Microgateway
Microservices have become the norm for modern application architecture. Workloads of modern applications are spread 
across many groups of microservices, cloud services and legacy services. The characteristics and behaviors of such 
heterogeneous services have a massive diversity. Such as authentication mechanisms, message formats, high availability 
factors and so on.
The WSO2 API Microgateway is designed to expose heterogeneous microservices as APIs to end consumers using a common API 
interface based on the Open API Specification. This helps expose microservices using a unified interface to external 
consumers, internal consumers and partners. It applies the common quality of service attributes on API requests such as 
security, rate limiting and analytics and also offers a wide range of features which helps organizations to deploy APIs 
microservice architectures efficiently.

## Microgateway quick start

*Prerequisites*
    - Make sure you have installed *docker* on your machine.
    - Make sure you have installed the *docker-compose* on your machine

Let's host our first API on a Microgateway. We will be exposing the publicly available [petstore services](https://petstore.swagger.io/) via  microgateway

1. First download the CLI tool(APICTL) and the microgateway distribution from the  
[github release page](https://github.com/wso2/product-microgateway/releases)
and extract them to a folder of your choice.
  * [CLI (APICTL)](https://github.com/wso2/product-microgateway/releases)
  * [Microgateway Distribution](https://github.com/wso2/product-microgateway/releases)
  
CLI tool extracted location will be referred as `CLI_HOME` and Microgateway distribution extracted location would be 
referred as `MG_HOME`.

2. Using your command line client tool add the 'CLI_HOME' folder to your PATH variable.
```
export PATH=$PATH:<CLI_HOME>
```

3. Let's create our first project with name "petstore" by adding the [open API definition](https://petstore.swagger.io/v2/swagger.json) of the petstore . You can do that by executing the following command using your command line tool.
```
apictl init petstore -oas https://petstore.swagger.io/v2/swagger.json
```

4. The project is now initialized. You should notice a directory with name "petstore" being created in the location 
where you executed the command. 

 
5. Now lets deploy the microgateway on docker by executing the docker compose script inside the `MG_HOME`. Navigate to `MG_HOME` and execute the following command
```
docker-compose up -d
```

Once containers are up and running, we can monitor the status of the containers using the following command

```
docker ps | grep mg-
```

6. Now let's deploy our first API to Microgateway using the project created in the step 3. Navigate to the location where the petstore project was initialized.
Zip the petstore project and create the file petstore.zip
Execute the following command to deploy the API in the microgateway

```
apictl mg deploy --host https://localhost:9095 --file "/Users/viraj/Documents/petstore.zip"  -u admin -p admin
```

The user credentials can be configured in the configurations of the `MG_HOME` distribution. `admin:admin` is the default accepted credentials by the 
microgateway control plane..

7. The next step would be to invoke the API using a REST tool. Since APIs on the Microgateway are by default secured. We need a valid token in order to invoke the API. 
Use the following sample token accepted by the microgateway to access the API. Lets set the token to command line as a variable


```
TOKEN=eyJ4NXQiOiJNell4TW1Ga09HWXdNV0kwWldObU5EY3hOR1l3WW1NNFpUQTNNV0kyTkRBelpHUXpOR00wWkdSbE5qSmtPREZrWkRSaU9URmtNV0ZoTXpVMlpHVmxOZyIsImtpZCI6Ik16WXhNbUZrT0dZd01XSTBaV05tTkRjeE5HWXdZbU00WlRBM01XSTJOREF6WkdRek5HTTBaR1JsTmpKa09ERmtaRFJpT1RGa01XRmhNelUyWkdWbE5nX1JTMjU2IiwiYWxnIjoiUlMyNTYifQ==.eyJhdWQiOiJBT2syNFF6WndRXzYyb2QyNDdXQnVtd0VFZndhIiwic3ViIjoiYWRtaW5AY2FyYm9uLnN1cGVyIiwibmJmIjoxNTk2MDA5NTU2LCJhenAiOiJBT2syNFF6WndRXzYyb2QyNDdXQnVtd0VFZndhIiwic2NvcGUiOiJhbV9hcHBsaWNhdGlvbl9zY29wZSBkZWZhdWx0IiwiaXNzIjoiaHR0cHM6Ly9sb2NhbGhvc3Q6OTQ0My9vYXV0aDIvdG9rZW4iLCJrZXl0eXBlIjoiUFJPRFVDVElPTiIsImV4cCI6MTYyNzU0NTU1NiwiaWF0IjoxNTk2MDA5NTU2LCJqdGkiOiIyN2ZkMWY4Ny01ZTI1LTQ1NjktYTJkYi04MDA3MTFlZTJjZWMifQ==.otDREOsUUmXuSbIVII7FR59HAWqtXh6WWCSX6NDylVIFfED3GbLkopo6rwCh2EX6yiP-vGTqX8sB9Zfn784cIfD3jz2hCZqOqNzSUrzamZrWui4hlYC6qt4YviMbR9LNtxxu7uQD7QMbpZQiJ5owslaASWQvFTJgBmss5t7cnurrfkatj5AkzVdKOTGxcZZPX8WrV_Mo2-rLbYMslgb2jCptgvi29VMPo9GlAFecoMsSwywL8sMyf7AJ3y4XW5Uzq7vDGxojDam7jI5W8uLVVolZPDstqqZYzxpPJ2hBFC_OZgWG3LqhUgsYNReDKKeWUIEieK7QPgjetOZ5Geb1mA==
``` 

8. We can now invoke the API running on the microgateway using cURL as below.
```
curl -X GET "https://localhost:9095/v2/pet/1" -H "accept: application/json" -H "Authorization:Bearer $TOKEN" -k
```


#### Microgateway Components
- **APICTL** : The APICTL is used to initiate Microgateway projects as well as to deploy APIs in to Microgateway. This is a developer tool used
 to deploy APIs into Microgateway

- **Proxy** : The client facing component of the Microgateway. The downstream request will reach the proxy component and it will route the request 
to the desired destination.

- **Filter Chain** : This component will intercept the request going through the proxy and applies security, rate limiting, publish analytics data and etc.
Proxy will forward the request to this component in order to validate and to add additional QoS.

- **Controller** : The component configures the proxy and the filter chain components dynamically during the runtime upon receiving an event for API 
creation or update.
#### Architecture

The following diagram illustrates how the WSO2 API Microgateway expose micro services using Open API definition as well 
as exposing APIs from [WSO2 API Manager](https://wso2.com/api-management/).

![Alt text](Architecture.png?raw=true "Title")


#### WSO2 API Microgateway APICTL commands

Following are the basic commands in APICTL which is used to deploy/update APIs in Microgateway

Note: Before you execute any of the commands below you need to add the path to the `<CLI_HOME` directory to the PATH environment variable. Ex: /home/dev/wso2am-micro-gw/bin

##### Init

`$ apictl init <project_name> --oas <filePathToOpenAPI_or_openAPIUrl`

The "apictl init" command is used to initialize a project structure with artifacts required to deploy API in Microgateway. This will create a **api_definitions**  directory.

Execute `apictl help init` to get more detailed information regarding the setup command.

Example

    $ apictl init petstore --oas https://petstore.swagger.io/v2/swagger.json

Let's see how we can expose the [petstore swagger](samples/petstore_swagger3.yaml) using the micro-gw.

##### Deploy

`$ apictl mg deploy --host <url_Of_ControlPlane> --file <Zipped_project_initiated_from_apictl>  --username <Username> --password <Password>`

Upon execution of this command, CLI tool deploy the API described with open API in the Microgateway.
```
 --host - Service url in which the Microgateway control plane is exposed.
 --file - File path of the zip file which is the comresseed  project intitiated from apictl tool.
 --username - A valid username in order to communicate with the control plane (ex: admin)
 --password - The password of the user.
```
Example

	$ apictl mg deploy --host https://localhost:9095 --file petstore.zip  --username admin --password admin


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
| x-wso2-transports              | Specify the transport security for the API(http, https and mutual ssl)                                       | Not Required -> API level only
| x-wso2-application-security    | Specify application security (basic_auth, api_key, oauth2)                                                   | Not Required -> API/Resource level 


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

