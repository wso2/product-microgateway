# Service to Service - East-West Traffic

In this scenario, we deploy Choreo Connect within the Istio service mesh where it applies API Management for microservices to microservice communication.

![east-west-traffic](east-west-traffic.jpg)

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

### 2. Deploy Choreo Connect with WSO2 API Manager

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
    wso2apim-6fcf44cbfd-q4g6n                    1/1     Running   0          3m39s
    choreo-connect-adapter-5cd89bf658-6mt2p      1/1     Running   0          46s
    choreo-connect-deployment-5886856568-z2cpk   3/3     Running   0          46s
    ```

- Deploy the Gateway and Virtualservice artifacts ([gw_vs.yaml](gw_vs.yaml))

    ```
    kubectl apply -f ../sidecar-mode/gw_vs.yaml
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

### 3. Deploy and Apply API Management for the the Trains Microservice

- Deploy Trains service.
  ```sh
  kubectl apply -f microservices/trains.yaml
  ```

- Access the Publisher portal - https://apim.wso2.com/publisher/. Use admin:admin as the credentials.

- Use [trains-openapi.yaml](trains-openapi.yaml) to create the API. Publisher portal will extract following values from the open API yaml.
  ```
  API Name: Trains
  Context: trains
  Version: 1.0.0
  Endpoint: http://trains/trains-service/v1
  ```

- [Use API keys to secure](https://apim.docs.wso2.com/en/4.1.0/design/api-security/api-authentication/secure-apis-using-api-keys/#using-api-keys-to-secure-an-api) the created API.

- Deploy and Publish the API.

- Access the Dev portal - https://apim.wso2.com/devportal/. Use admin:admin as the credentials. Subscribe to an application and get an API Key with infinite validity period.
  ```sh
  API_KEY=<API_KEY>
  ```

- Try out in DevPortal with API Key and from the following curl with API Key.
  ```sh
  curl -X 'GET' \
    "https://gw.wso2.com/trains/1.0.0/trains" \
    -H "accept: application/json" \
    -H "apikey: $API_KEY" -k
  ```

### 5. Deploy the Schedules Microservice

- Create a k8s secret with API Key of Trains API.
  ```sh
  kubectl create secret generic schedules-creds --from-literal=trains_service_api_key=$API_KEY
  ```

- Create a Virtual Service to set `Host` header (to set the Vhost of the gateway environment).
  ```sh
  kubectl apply -f cc-internal-vs.yaml
  ```

- Deploy Schedules service.
    ```
    kubectl apply -f microservices/schedules.yaml
    ```

### 6. Apply API Management for the Schedules Microservice

- Access the Publisher portal - https://apim.wso2.com/publisher/.

- Create an API for the microservice

  - Import the [schedules-openapi.yaml](schedules-openapi.yaml) and create the API.

  - Provide the following details.
    ```
    API Name: Schedules
    Context: schedules
    Version: 1.0.0
    Endpoint: http://schedules:80/schedules-service/v1
    ```
  
- Deploy and test the API using the Tryout or using curl command provided in the API testing section.

- Productise and publish the API to the WSO2 Devportal for application developers.
