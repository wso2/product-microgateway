# Choreo Connect

[![Build Status](https://wso2.org/jenkins/job/products/job/product-microgateway/badge/icon)](https://wso2.org/jenkins/job/products/job/product-microgateway/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![License](https://img.shields.io/badge/slack-microgateway-blueviolet)](https://join.slack.com/t/wso2-apim/shared_invite/enQtNzEzMzk5Njc5MzM0LTgwODI3NmQ1MjI0ZDQyMGNmZGI4ZjdkZmI1ZWZmMjNkY2E0NmY3ZmExYjkxYThjNzNkOTU2NWJmYzM4YzZiOWU)

Choreo Connect is a cloud-native, open-source, and developer-centric API gateway proxy. It provides first-class support for K8s while facilitating an array of API management quality of services (QoS), such as message security rate-limiting, observability, and message mediation.

It is powered by [Envoy Proxy](https://www.envoyproxy.io/)!

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

## Quick Start Guide

*Prerequisites*  
- On your machine, you should have installed,  
  - *docker*
  - *docker-compose* 

Let's host our first API on Choreo Connect. We will be exposing the publicly available [petstore services](https://petstore.swagger.io/) via  Choreo Connect.

1. First download the latest CLI tool(APICTL) and the Choreo Connect distributions
and extract them to a folder of your choice.
   
    * [CLI (APICTL - v4.1.x)](https://github.com/wso2/product-apim-tooling/releases/)
    * [Choreo Connect Distribution](https://github.com/wso2/product-microgateway/releases/)
    
      CLI tool extracted location will be referred as `CLI_HOME` and Choreo Connect distribution extracted location would be referred as `CC_HOME`.

2. Using your command line client tool add the `CLI_HOME` folder to your PATH variable.
    ```
    export PATH=$PATH:<CLI_HOME>
    ```

3. Let's create our first project with name "petstore" by adding the [OpenAPI definition](https://petstore.swagger.io/v2/swagger.json) of the petstore . You can do that by executing the following command using your command line tool.

    ```
    apictl init petstore --oas https://petstore.swagger.io/v2/swagger.json
    ```
   > **Note:**  If you have used a previous version of apictl before, remember to delete the directories  .wso2apictl and .wso2apictl.local that are located in `/home/<your-pc-username>`. Deleting them will make the newer apictl create them again, with content compatible with the current version. You can backup the files before deleting them, in case you had to refer them later.


4. The project is now initialized. You should notice a directory with name "petstore" being created in the location 
where you executed the command. 

 
5. Now let's start the Choreo Connect on docker by executing the docker compose script inside the `CC_HOME/docker-compose/choreo-connect/`. Navigate to `CC_HOME/docker-compose/choreo-connect/` and execute the following command
    ```
    docker-compose up -d
    ```

    Once containers are up and running, we can monitor the status of the containers using the following command

    ```
    docker ps | grep choreo-connect-
    ```
    The user credentials can be configured in the configurations file `CC_HOME/docker-compose/choreo-connect/conf/config.toml`inside Choreo Connect distribution. `admin:admin` is the default accepted credentials by the Choreo Connect adapter.

    > **Note:** Following apictl commands are being executed with -k flag to avoid SSL verification with the Choreo Connect.
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
   > **Note:**
    Recommendation is to disable this `/testkey` endpoint in production environments as it is only for testing purposes.

10. We can now invoke the API running on the Choreo Connect using cURL as below.
    ```
    curl -X GET "https://localhost:9095/v2/pet/findByStatus?status=available" -H "accept: application/json" -H "Authorization:Bearer $TOKEN" -k
    ```

11. Try out the following commands with apictl. 

    ```
    List APIs          - apictl mg get apis -e dev -k
    Undeploy API       - apictl mg undeploy api -n SwaggerPetstore -v 1.0.6 -e dev -k
    Logout             - apictl mg logout dev   
    Remove Environment - apictl mg remove env dev
    ```

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

## Choreo Connect Modes

- **Choreo Connect with WSO2 API Manager as a Control Plane**
    
  Choreo Connect can use WSO2 API Manager as its control plane, whether it's in the cloud or on-premise.


- **Choreo Connect as a Standalone Gateway**

    Without the API Manager as the control plane, Choreo Connect can be deployed as a standalone gateway. APICTL is a command-line program that can be used to deploy APIs. By incorporating the OpenAPI specifications in APICTL, it is possible to develop API projects. 

## Documentation

You can refer [Choreo Connect Documentation](https://apim.docs.wso2.com/en/4.0.0/deploy-and-publish/deploy-on-gateway/choreo-connect/getting-started/choreo-connect-overview/).
  