# WSO2 API Microgateway

The WSO2 API Microgateway (MGW) is a Cloud Native API Gateway which can be used to expose one or many microservices as APIs.

The WSO2 API Microgateway is designed to expose heterogeneous microservices as APIs to end consumers using a common API 
interface based on the Open API Specification. This helps expose microservices using a unified interface to external 
consumers, internal consumers and partners. It applies the common quality of service attributes on API requests such as 
security, rate limiting and analytics and also offers a wide range of features which helps organizations to deploy APIs 
microservice architectures efficiently.

[![Build Status](https://wso2.org/jenkins/job/products/job/product-microgateway/badge/icon)](https://wso2.org/jenkins/view/All%20Builds/job/products/job/product-microgateway)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![License](https://img.shields.io/badge/slack-microgateway-blueviolet)](https://join.slack.com/t/wso2-apim/shared_invite/enQtNzEzMzk5Njc5MzM0LTgwODI3NmQ1MjI0ZDQyMGNmZGI4ZjdkZmI1ZWZmMjNkY2E0NmY3ZmExYjkxYThjNzNkOTU2NWJmYzM4YzZiOWU)
[![CII Best Practices](https://bestpractices.coreinfrastructure.org/projects/3312/badge)](https://bestpractices.coreinfrastructure.org/projects/3312)

## Table of Contents

   * [Quick Start](#quick-start)
   * [Documentation](#documentation)
   * [Features](#features)
   * [Workflow](#workflow)
   * [Contributing](#contributing)
   * [Contact](#contact)

## Quick Start
Let's expose the publicly available [petstore service](https://petstore.swagger.io/) via  microgateway.

1. Download the latest WSO2 API Microgateway release from the product [official page](https://wso2.com/api-management/api-microgateway/) or 
[github release page](https://github.com/wso2/product-microgateway/releases) and extract it. For Docker container based usages, downloading only the toolkit will be sufficient. 

1. Add the 'bin' directory of the extracted distributions to your PATH variable.
    <details open>
    <summary>Docker</summary>

    ```
    export PATH=$PATH:<TOOLKIT_EXTRACTED_LOCATION>/bin
    ```
    </details>

    <details>
    <summary>Virtual Machine</summary>

    ```
    export PATH=$PATH:<TOOLKIT_EXTRACTED_LOCATION>/bin
    export PATH=$PATH:<RUNTIME_EXTRACTED_LOCATION>/bin
    ```
    </details>

3. First we need to initialize a project by adding the [open API definition](https://petstore.swagger.io/v2/swagger.json) of the petstore service. We'll name the project as "petstore". To initialize the project, execute following command.
    ```
    micro-gw init petstore -a https://petstore.swagger.io/v2/swagger.json
    ```

4. The project is now initialized with the open api definition of the [petstore service](https://petstore.swagger.io/).

 
5. Next, Lets build the project and create a microgateway instance.
    <details open>
    <summary>Docker</summary>

    ```bash
    micro-gw build petstore --docker-image petstore:v1 --docker-base-image wso2/wso2micro-gw:3.2.0
    ```
    </details>

    <details>
    <summary>Virtual Machine</summary>

    ```bash
    micro-gw build petstore
    ```
    </details>

6. Start the gateway.
    <details open>
    <summary>Docker</summary>

    ```
    docker run -d -p 9090:9090 -p 9095:9095 petstore:v1
    ```
    </details>

    <details>
    <summary>Virtual Machine</summary>

    ```
    gateway <PROJECT_LOCATION>/target/petstore.jar
    ```
    </details>

7. Now we can test our API. Since APIs on the MGW are by default secured. We need a valid token or key in order to invoke the API. MGW can issue API keys on its own. Execute the command below to get a API key from microgateway. Following command will set the api key into `TOKEN` variable.

    ```
    TOKEN=$(curl -X get "https://localhost:9095/apikey" -H "Authorization:Basic YWRtaW46YWRtaW4=" -k)
    ``` 

8. We can now invoke the API running on the microgateway using cURL as below.
    ```
    curl -X GET "https://localhost:9095/v2/pet/1" -H "accept: application/json" -H "api_key:$TOKEN" -k
    ```
## Documentation

- [Official Documentation](https://mg.docs.wso2.com)
- [FAQ](https://mg.docs.wso2.com/en/latest/faqs/)
- [Articles](https://wso2.com/library/api-management/)
- [Blogs](https://medium.com/api-integration-essentials)

## Features

Below is a list of **most noticeable features** out of many other features MGW hosts.
1. Exposing one or more microservices as APIs using the Open API Specification.
1. Authentication and Authorization based on OAuth2.0 (opaque tokens and JWTs), API keys, Basic Auth and Mutual TLS.
1. Rate Limiting of API requests based on numerous policies.
1. Business Insights through API Analytics.
1. Service discovery.
1. Request and Response transformations.
1. Load balancing, failover and circuit breaking capabilities of API requests.
1. Seamless Docker and Kubernetes integration.
1. Integration with WSO2 API Manager to support design first APIs, API Analytics and shared rate limiting.

It also has the following characteristics that makes it a perfect fit for microservice architectures
- Less than 1s startup time, allowing faster scaling.
- Built on a stateless architecture, allowing for infinite scaling.
- Has an immutable runtime, making it heavily robust.
- CI/CD friendly worlkflow.
- Runs in isolation with no dependencies to other components

## Workflow

![Architecture](architecture-new.png?raw=true "Architecture")

Exposing APIs with MGW involves in two main phases.

### Dev Phase

MGW uses OpenApi specification and a set of MGW specific OpenApi vendor extensions to define an API. Users can use these extensions to enable/disable different gateway features on an API. A sample can be found in [here](samples/petstore_basic.yaml).
Dev phase involves defining the required API(s) using these extensions. This phase can involve below basic steps.
1. Initializing a project.
1. Adding/updating project's OpenApi definition(s).
1. Sharing the project with other developers/teams (in Github or any VCS)

### Ops Phase

Once the project is ready for the deployment, MGW can produce three types of main deployment artifacts. Out of these three types, you can select which artifact(s) you need for the required deployment.
1. **Executable**    
This is the default build output of a project. Output executable can be provided to a [MGW runtime installation](https://mg.docs.wso2.com/en/latest/install-and-setup/install-on-vm/) to start a microgateway server instance. Otherwise this executable can me mounted to a MGW docker container.
1. **Docker**   
MGW Toolkit `build` command can be configured to produce a docker image required to create a [docker installation](https://mg.docs.wso2.com/en/latest/install-and-setup/install-on-docker/).
1. **Kubernetes**   
Similar to docker images, `build` command can be configured to produce kubernetes artifacts required to create a [kubernetes installation](https://mg.docs.wso2.com/en/latest/install-and-setup/install-on-kubernetes/).

## Contributing

MGW runtime is mostly implemented with [ballerina-lang](https://ballerina.io). However the toolkit is implemented with Java. You can contribute your code to both runtime and toolkit. 

Contribution is not limited to the code. You can help the project with identifying issues, suggesting documentation changes etc.

For more information on how to contribute, read our [contribution guidelines](CONTRIBUTING.md).

## Contact

- Developer List: dev@wso2.org
- Slack: [#microgateway](https://apim-slack.wso2.com/)
- Twitter: [#wso2apimanager](https://twitter.com/wso2apimanager/)
