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
Empty `api_definitions` folder and `definitions.yaml` will be created inside the main folder. API developer can add multiple open API definitions inside the
api_definitions file and define endpoints and interceptors for the resources in definitions.yaml


##### Building the WSO2 API Microgateway project

Once the project has been created, the next step is to build the project sources.

#### WSO2 API Microgateway commands

Following are the set of commands included within the WSO2 API Microgateway.

Note: Before you execute any of the commands below you need to add the path to the <micro-gw-home>/bin directory to the PATH environment variable. Ex: /home/dev/wso2am-micro-gw/bin

##### Init

`$ micro-gw init`

The "micro-gw init" command is used to initialize a project structure with artifacts required in generating a WSO2 API Microgateway distribution. This will create a **api_definitions**  folder and an empty **definitions.yaml**.

* **api_defintions** - API developer should copy all the open API definitions of microservices inside this folder
* **definitions.yaml** - API developer can define API level and resource level endpoints and interceptors and  resource level throttle policies

If the project already exists, a warning will be prompted requesting permission to override existing project.

Execute `micro-gw help setup` to get more detailed information regarding the setup command.

Example


    $ micro-gw init petstore-project


Sample definition.yaml can be defined as follows. This is defined for the petstore swagger : https://petstore.swagger.io/v2/swagger.json. API deveoper should copy this swagger to api_defintions file


```
apis:
    /petstore/v1:
        title: Swagger Petstore
        version: 1.0.0
        production_endpoint:
            type: 'http'
            urls:
                - 'https://petstore.swagger.io/v2'
        sandbox_endpoint:
            type: 'http'
            urls:
                - 'https://sand.petstore.swagger.io/'
        resources:
            /pet/findByStatus:
                get:
                    production_endpoint:
                        type: 'http'
                        urls:
                            - 'http://www.mocky.io/v2/5cbd4d1d2f0000e70a16cc0e'
        security: 'oauth'

```


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
├── definition.yaml
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
