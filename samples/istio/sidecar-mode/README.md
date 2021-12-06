# Istio - Sidecar Mode

In this scenario, we deploy Choreo Connect within the Istio service mesh where it applies API Management for the microservices.

![Alt text](sidecar.jpg?raw=true "Sidecar Mode")

### Installation Prerequisites

- [Kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/)

- [Kubernetes v1.20 or above](https://Kubernetes.io/docs/setup/) <br>

    - Minimum CPU : 6vCPU
    - Minimum Memory : 6GB

- [Istio v1.11.x or above](https://istio.io/docs/setup/platform-setup/)

### 1. Enable Istio Injection for the namespace

```
kubectl label namespace default istio-injection=enabled
```

### 2. Deploy the microservice

- Deploy Httpbin service
    ```
    kubectl apply -f ../microservice/httpbin.yaml
    ```

### 3. Deploy Choreo Connect with WSO2 API Manager

- Download and extract Choreo Connect distribution .zip file

  The extracted folder will be called as CHOREO-CONNECT_HOME hereafter.

- Deploy WSO2 API Manager

    ```
    cd CHOREO-CONNECT_HOME
    kubectl apply -f k8s-artifacts/choreo-connect-with-apim/apim
    ```

- Deploy Choreo Connect 

    ```
    kubectl apply -f k8s-artifacts/choreo-connect-with-apim/choreo-connect
    ```

- Verify the installation

    ```
    kubectl get pods

    Output:
    NAME                                         READY   STATUS    RESTARTS   AGE
    choreo-connect-adapter-bcf875545-bm8vm       1/1     Running   0          4m53s
    choreo-connect-deployment-5d5545f4dd-24f8z   2/2     Running   0          4m52s
    wso2apim-bd6545485-jsnds                     1/1     Running   0          5m19s
    ```

- Deploy the Gateway and Virtualservice artifacts ([gw_vs.yaml](gw_vs.yaml))

    ```
    kubectl apply -f gw_vs.yaml
    ```

- Retrieve the ingress gateway URL

    ```
    kubectl get svc istio-ingressgateway -n istio-system
  
    Output:
    NAME                   TYPE           CLUSTER-IP      EXTERNAL-IP   PORT(S)                                      AGE
    istio-ingressgateway   LoadBalancer   10.99.243.134   localhost     15021:32643/TCP,80:30653/TCP,443:30717/TCP   145m
    ```

  Use the EXTERNAL-IP as the ingress gateway host which is `localhost` and `443` as the ingress gateway port
  

- Add an `/etc/host` entry for Choreo Connect and WSO2 API Manager

    ```
    127.0.0.1  apim.wso2.com gw.wso2.com
    ```

### 4. Apply API Management for the microservice

- Access the Publisher portal - https://apim.wso2.com/publisher/

  Use admin:admin as the credentials.


- Create an API for the microservice

  - Import the [openAPI.yaml](openAPI.yaml) and create the API.
  - Provide the following details.

    ```
    API Name: HttpbinAPI
    Context: httpbinapi
    Version: v1.0.0
    Endpoint: http://httpbin
    ```
  
- Deploy and test the API using the Tryout or using curl command provided in the API testing section.


- Productise and publish the API to the WSO2 Devportal for application developers.

