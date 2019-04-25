# Micro-GW Docker Base Image

## Building the image

1. Build the distribution(product-microgateway/distribution) package using 'mvn clean install' which will copy the relevant jars to the runtime folder.
Or manually copy the jar with relevant version from nexus: https://maven.wso2.org/nexus/content/groups/wso2-public/org/wso2/am/microgw/org.wso2.micro.gateway.core/ to the runtime folder
1. Run the following command to build the base docker image. Command should be executed inside the docker folder

```docker build --no-cache=true -t wso2/micro-gw:<version> .```