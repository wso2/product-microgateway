# Envoy filter example

This project demonstrates the linking of additional HTTP filters with the Envoy binary.
A new filter `envoy.mgw` can be used to modify the downstream payload body. 

The new filter works as follows.
- Enable ext_authz and mgw filters.
```
  - name: envoy.mgw
    typed_config:
        "@type": type.googleapis.com/envoy.extensions.filters.http.mgw.v3.MGW
```
- External authorization service add modified payload in ok response's metadata under the key `payload`.
- If payload key is found under the ext_auth metadata, then payload will be modified. 

## Building

To build the Envoy static binary:

1. `git submodule update --init`
2. `bazel build //:envoy`

## Sample config

```
admin:
  access_log_path: /tmp/admin_access.log
  address:
    socket_address:
      protocol: TCP
      address: 0.0.0.0
      port_value: 9901
static_resources:
  listeners:
  - name: listener_0
    address:
      socket_address:
        protocol: TCP
        address: 0.0.0.0
        port_value: 10000
    filter_chains:
    - filters:
      - name: envoy.http_connection_manager
        typed_config:
          "@type": type.googleapis.com/envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager
          stat_prefix: ingress_http
          route_config:
            name: local_route
            virtual_hosts:
            - name: local_service
              domains: ["*"]
              routes:
              - match:
                  prefix: "/"
                route:
                  cluster: netty-backend
          http_filters:
          - name: envoy.mgw
            typed_config:
                "@type": type.googleapis.com/envoy.extensions.filters.http.mgw.v3.MGW
          - name: envoy.router
            typed_config: 
              "@type": type.googleapis.com/envoy.extensions.filters.http.router.v3.Router
              
  clusters:
  - name: netty-backend
    connect_timeout: 300s
    type: LOGICAL_DNS
    # Comment out the following line to test on v6 networks
    dns_lookup_family: V4_ONLY
    lb_policy: ROUND_ROBIN
    load_assignment:
      cluster_name: netty-backend
      endpoints:
      - lb_endpoints:
        - endpoint:
            address:
              socket_address:
                address: 0.0.0.0
                port_value: 8688
    connect_timeout: 600s
```