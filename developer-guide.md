# Developer Guide

This provides the information to how to work with the current microgateway setup.

## Prerequisites

docker

jdk-11

maven 3.6

## Quick Start Guide

- sh remove-containers.sh (If it is the first time this is not required to run.
The purpose is to remove the already created docker containers. Otherwise the docker 
maven plugin would fail to create the new docker images)

- mvn clean install (make sure java-11 is set because the filter-chain pom contains
 java-11 as the source)
 
 - Once the mvn clean install is completed, navigate to distribution/target/.
 Then extract the zip file called WSO2-Microgateway-3.2.1-SNAPSHOT.zip
 
 - Then execute `docker-compose up` to run the setup. This will start an envoy container,
 filter-core container and piot container. The mounted configurations can be found from
 docker-compose directory.
 
 - The apictl is required to add APIs to the microgateway. https://github.com/wso2/product-apim-tooling/tree/envoy-gw
 Then you need initialize a project using apictl `init` command. (help command will guide)
 Then the certificate (resources/certs/localhost.pem) needs to be copied to `~/.wso2apictl/certs` 
 directory. And finally execute `apictl mg deploy ...` command. (help command will guide through it)
 
 - Configurations
    - configurations under `[server]` refer to the rest API server which is used by
    the apictl.
    - configurations under `[envoy]` refer to the listener configurations related to envoy listener.
    Other than that the user can edit the `envoy.yaml` file located insider `<distribution>/resources/proxy`
    directory.
 
 ### If the developer needs to build a separate component
 
 - In this case, first execute the `sh remove-containers.sh`. If you need to keep the
 other container up and running please modify the script. If not (usually) you can simply
 execute the script as it is because this does not remove the docker image.
 
 - Make sure three containers up and running using `docker ps`.
 
 - See if the envoy configuration is correct by navigating to `localhost:9000`
 
 - Then navigate to the component and execute `mvn clean install`
 
 - Finally, navigate to your extracted distribution and run docker-compose up as it is.
 (make sure you change the configuration files mounted, it the change is relevant)
 to a configuration file change)
 
 ### If your changes does not reflect,
 
 - Check if the docker-containers are removed correctly. Using `docker ps -a`. 
 the `sh remove-containers.sh` may fail if you have changed the image name, image version.
 And if the containers are removed, docker-maven-plugin fails to recreate the image with the changes.
 
 - Make sure you make/change the configurations correctly when you are using the distribution
 zip file.
 
 - If you need to run the control plane as a go executable, make sure you set MGW_HOME environment
 variable to point the directory where your configurations are located.
    - Ex. export MGW_HOME
