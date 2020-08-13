# Dockerfile for Micro-GW Base Image #

This section defines the step-by-step instructions to build an [Ubuntu](https://hub.docker.com/_/ubuntu/) Linux based Docker image and an [Alpine](https://hub.docker.com/_/alpine/) Linux based Docker image for Micro-GW base image.

## Prerequisites

* [Docker](https://www.docker.com/get-docker) v17.09.0 or above

## How to build an image

##### 1. Navigate to ubuntu or alpine folder based on the preference.
##### 2. Build the Docker image using the following command.

```docker build --no-cache=true -t wso2/wso2micro-gw:3.1.0 .```
   
> By default, the Docker image will prepackage the General Availability (GA) release version of the relevant WSO2 product.

NOTE : Please replace the '${MGW_VERSION}' with the relevant microgateway version. For ex: 3.2.0

## Docker command usage references

* [Docker build command reference](https://docs.docker.com/engine/reference/commandline/build/)
* [Docker run command reference](https://docs.docker.com/engine/reference/run/)
* [Dockerfile reference](https://docs.docker.com/engine/reference/builder/)
