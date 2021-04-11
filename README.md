# Choreo Connect

[![Build Status](https://wso2.org/jenkins/job/products/job/product-microgateway/badge/icon)](https://wso2.org/jenkins/job/products/job/product-microgateway/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![License](https://img.shields.io/badge/slack-microgateway-blueviolet)](https://join.slack.com/t/wso2-apim/shared_invite/enQtNzEzMzk5Njc5MzM0LTgwODI3NmQ1MjI0ZDQyMGNmZGI4ZjdkZmI1ZWZmMjNkY2E0NmY3ZmExYjkxYThjNzNkOTU2NWJmYzM4YzZiOWU)

The Choreo Connect is a Cloud Native API Gateway which can be used to expose one or many microservices as APIs.


## Why Choreo Connect
Microservices have become the norm for modern application architecture. Workloads of modern applications are spread 
across many groups of microservices, cloud services and legacy services. The characteristics and behaviors of such 
heterogeneous services have a massive diversity. Such as authentication mechanisms, message formats, high availability 
factors and so on.
The Choreo Connect is designed to expose heterogeneous microservices as APIs to end consumers using a common API 
interface based on the Open API Specification. This helps expose microservices using a unified interface to external 
consumers, internal consumers and partners. It applies the common quality of service attributes on API requests such as 
security, rate limiting and analytics and also offers a wide range of features which helps organizations to deploy APIs 
microservice architectures efficiently.

## Choreo Connect quick start

*Prerequisites*
    - Make sure you have installed *docker* on your machine.
    - Make sure you have installed the *docker-compose* on your machine

Let's host our first API on Choreo Connect. We will be exposing the publicly available [petstore services](https://petstore.swagger.io/) via  microgateway.

1. First download the latest CLI tool(APICTL) and the Choreo Connect distributions
and extract them to a folder of your choice.
  * [CLI (APICTL)](https://github.com/wso2/product-apim-tooling/releases/)
  * [Choreo Connect Distribution](https://github.com/wso2/product-microgateway/releases/)
  

CLI tool extracted location will be referred as `CLI_HOME` and Choreo Connect distribution extracted location would be 
referred as `CC_HOME`.

2. Using your command line client tool add the 'CLI_HOME' folder to your PATH variable.
    ```
    export PATH=$PATH:<CLI_HOME>
    ```

3. Let's create our first project with name "petstore" by adding the [open API definition](https://petstore.swagger.io/v2/swagger.json) of the petstore . You can do that by executing the following command using your command line tool.

    !!! NOTE:

    If you have used a previous version of apictl before, remember to delete the directories  .wso2apictl and .wso2apictl.local that are located in `/home/<your-pc-username>`. Deleting them will make the newer apictl create them again, with content compatible with the current version. You can backup the files before deleting them, in case you had to refer them later.

    ```
    apictl init petstore --oas https://petstore.swagger.io/v2/swagger.json
    ```

4. The project is now initialized. You should notice a directory with name "petstore" being created in the location 
where you executed the command. 

 
5. Now lets start the Choreo Connect on docker by executing the docker compose script inside the `CC_HOME/docker-compose/choreo-connect/`. Navigate to `CC_HOME/docker-compose/choreo-connect/` and execute the following command
    ```
    docker-compose up -d
    ```

    Once containers are up and running, we can monitor the status of the containers using the following command

    ```
    docker ps | grep choreo-connect-
    ```
    The user credentials can be configured in the configurations file `CC_HOME/docker-compose/choreo-connect/conf/config.toml`inside Choreo Connect distribution. `admin:admin` is the default accepted credentials by the Choreo Connect adapter.

    !!! NOTE:

    Following apictl commands are being executed with -k flag to avoid SSL verification with the Choreo Connect.

    To communicate via https without skipping SSL verification (without -k flag), add the cert of Choreo Connect into `/home/<your-pc-username>/.wso2apictl/certs`.

6. To use apictl with Choreo Connect, let's first add a environment specifically for our Choreo Connect deployment. The environment will hold the adapter URL for further commands.

    ```
    apictl mg add env dev --adapter https://localhost:9843
    ```

7. Next you can use the following command to login to the above Choreo Connect cluster (in other words login to the Choreo Connect adapter).

    ```
    apictl mg login dev -k
    ```
    or
    ```
    apictl mg login dev -u admin -p admin -k
    ```


8. Now let's deploy our first API to Choreo Connect using the project created in the step 3. Navigate to the location where the petstore project was initialized. Execute the following command to deploy the API in the Choreo Connect deployment.

    ```
    apictl mg deploy api -f petstore -e dev -k
    ```

9. The next step would be to invoke the API using a REST tool. Since APIs on the Choreo Connect are by default secured. We need a valid token in order to invoke the API. 
Let's get a test token from the Choreo Connect using its `/testkey` endpoint.
   
    ```
    TOKEN=$(curl -X POST "https://localhost:9095/testkey" -d "scope=read:pets" -H "Authorization: Basic YWRtaW46YWRtaW4=" -k -v)
    ``` 
!!! NOTE:
    Recommendation is to disable this `/testkey` endpoint in production environments as it is configurable.

10. We can now invoke the API running on the Choreo Connect using cURL as below.
    ```
    curl -X GET "https://localhost:9095/v2/pet/findByStatus?status=available" -H "accept: application/json" -H "Authorization:Bearer $TOKEN" -k
    ```

11. Try out the following commands with apictl. 

    list APIs     - `apictl mg get apis -e dev`   
    undeploy API  - `apictl mg undeploy api -n SwaggerPetstore -v 1.0.5 -e dev`    
    logout        - `apictl mg logout dev`    
    remove environment - `apictl mg remove env dev`

## Choreo Connect Components
- **APICTL** : The APICTL is used to initiate Choreo Connect projects as well as to deploy APIs in to Choreo Connect environment by developers. 

- **Router** : The client facing component of the Choreo Connect. The downstream request will reach the proxy component and it will route the request
to the desired destination.

- **Enforcer** : This component will intercept the request going through the Router and applies security, rate limiting, publish analytics data and etc.
Router will forward the request to this component in order to validate and to add additional QoS.

- **Adapter** : The component configures the Router, and the enforcer components dynamically during the runtime upon receiving an event for API
creation or update.
## Architecture

The following diagram illustrates how the Choreo Connect expose microservices using Open API definition as well 
as exposing APIs from [WSO2 API Manager](https://wso2.com/api-management/).

![Alt text](Architecture.png?raw=true "Title")


## Choreo Connect APICTL commands

Following are the basic commands in APICTL which is used to handle APIs in Choreo Connect

Note: Before you execute any of the commands below you need to add the path to the `<CLI_HOME` directory to the PATH environment variable. Ex: /home/dev/wso2am-micro-gw/bin

### Init

`$ apictl init <project_name> --oas <filePathToOpenAPI_or_openAPIUrl>`

The "apictl init" command is used to initialize a project structure with artifacts required to deploy API in Choreo Connect. This will create a **api_definitions**  directory.

Execute `apictl init -h` to get more detailed information regarding the setup command.

Example

    $ apictl init petstore --oas https://petstore.swagger.io/v2/swagger.json

Let's see how we can expose the [petstore swagger](samples/petstore_swagger3.yaml) using the `apictl `through Choreo Connect.

### Add Environment and Login
```
apictl mg add env <mgw_environement> --adapter <adapter_url>
```
```
apictl mg login <mgw_environement> -u <username> -p <password>
```

NOTE: use `apictl mg login -h` to view more ways to include credentials

### Deploy

```
apictl mg deploy api -f <file_path_of_API_project_created> -e <mgw_environement>
```

### Update 
```
apictl mg deploy api -f <file_path_of_API_project_created> -e <mgw_environement> -o
```  
### List
```
apictl mg get apis -e <mgw_environement>
```    
### Undeploy
```
apictl mg undeploy api -n <API_name_without spaces> -v <API_version> -e <mgw_environement>
```   

### Logout and Remove environement
```
apictl mg logout <mgw_environement>
apictl mg remove env <mgw_environement>
```

## Invoke API exposed via Choreo Connect
Once APIs are exposed we can invoke API with a valid jwt token or an opaque access token.
In order to use jwt tokens, Choreo Connect should be presented with  a jwt signed by a trusted OAuth2 service. There are few ways we can get a jwt token

1. Any third party secure token service
The public certificate of the token service which used to sign the token should be added to the trust store of the Choreo Connect.
The jwt should have the claims **sub, aud, exp** in order to validate with Choreo Connect.

1. Get jwt from WSO2 API Manager
Please refer the [documentation](https://apim.docs.wso2.com/en/4.0.0-dev/consume/manage-application/generate-keys/generate-api-keys/) on how to get a valid jwt

The following sample command can be used to invoke the "/pet/findByStatus" resource of the petstore API

```
curl -X GET "https://localhost:9095/petstore/v1/pet/findByStatus?status=available" -H "accept: application/xml" -H "Authorization:Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsIng1dCI6Ik5UQXhabU14TkRNeVpEZzNNVFUxWkdNME16RXpPREpoWldJNE5ETmxaRFUxT0dGa05qRmlNUSJ9.eyJhdWQiOiJodHRwOlwvXC9vcmcud3NvMi5hcGltZ3RcL2dhdGV3YXkiLCJzdWIiOiJhZG1pbiIsImFwcGxpY2F0aW9uIjp7ImlkIjoyLCJuYW1lIjoiSldUX0FQUCIsInRpZXIiOiJVbmxpbWl0ZWQiLCJvd25lciI6ImFkbWluIn0sInNjb3BlIjoiYW1fYXBwbGljYXRpb25fc2NvcGUgZGVmYXVsdCIsImlzcyI6Imh0dHBzOlwvXC9sb2NhbGhvc3Q6OTQ0M1wvb2F1dGgyXC90b2tlbiIsImtleXR5cGUiOiJQUk9EVUNUSU9OIiwic3Vic2NyaWJlZEFQSXMiOltdLCJjb25zdW1lcktleSI6Ilg5TGJ1bm9oODNLcDhLUFAxbFNfcXF5QnRjY2EiLCJleHAiOjM3MDMzOTIzNTMsImlhdCI6MTU1NTkwODcwNjk2MSwianRpIjoiMjI0MTMxYzQtM2Q2MS00MjZkLTgyNzktOWYyYzg5MWI4MmEzIn0=.b_0E0ohoWpmX5C-M1fSYTkT9X4FN--_n7-bEdhC3YoEEk6v8So6gVsTe3gxC0VjdkwVyNPSFX6FFvJavsUvzTkq528mserS3ch-TFLYiquuzeaKAPrnsFMh0Hop6CFMOOiYGInWKSKPgI-VOBtKb1pJLEa3HvIxT-69X9CyAkwajJVssmo0rvn95IJLoiNiqzH8r7PRRgV_iu305WAT3cymtejVWH9dhaXqENwu879EVNFF9udMRlG4l57qa2AaeyrEguAyVtibAsO0Hd-DFy5MW14S6XSkZsis8aHHYBlcBhpy2RqcP51xRog12zOb-WcROy6uvhuCsv-hje_41WQ==" -k
```
Please note that the jwt provided in the command is a jwt token retrieved from WSO2 API Manager with higher expiry time which can be used with any API not protected with scopes.
This token works with any API since by default the microgateway config uses the public certificate of WSO2 API Manager to validate the signature.
