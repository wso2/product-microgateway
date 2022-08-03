# WSO2 API Manager WebSocket API Example Project

This is a sample API project can be used to deploy an WebSocket API in Choreo Connect with standalone mode.

## Project structure

``` bash
├── api.yaml
├── api_meta.yaml
├── deployment_environments.yaml
├── Client-certificates
├── Definitions
│   └── asyncapi.yaml
├── Docs
├── Endpoint-certificates
├── Image
└── Policies
```

> **_NOTE:_**  
> In the above structure, main files needed to deploy an WebSocket API are `api.yaml`and `definitions/asyncapi.yaml`.
> Sample project added in here includes only those files.
> Other files are optional and those will be needed if you have additional WebSocket API configurations.

## Deploy a minimal API

Below section lists the minimum set of changes that you need to change when you are using this project to match with your own
WebSocket API. You can configure below attributes to match with you own WebSocket API implementation.

- `name` : Must not include spaces (i.e. MyAPI)
- `version` : A simple version string (i.e. v1.0.0)
- `context` : Context of the API (i.e. /MyAPI)
- `lifeCycleStatus` : Lifecycle status of the API (If you want to change the status of the API to `PUBLISHED`, modify this field to `PUBLISHED`)
- `production_endpoints` : This is listed under the `endpoint_config` attribute. You can use this to give your WebSocket API's backend URL.

`Definitions/asyncapi.yaml` contains attributes specific to the WebSocket API implementation.

Import the API project using the command,
`apictl import api -f [directory path] -e [env name]`

> **_NOTE:_**  
> This sample project uses `ws://ws.ifelse.io:80` endpoint to test the WebSocket API. Since this is a deployment that does not
> have a graphical user interface, you need to installed `wscat client.`.