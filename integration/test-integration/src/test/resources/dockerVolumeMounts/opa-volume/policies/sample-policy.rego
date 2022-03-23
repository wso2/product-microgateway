package sample

default allow = false

allow = true {
    input.transportHeaders.foo == "bar"
    input.method == "POST"
    input.authenticationContext.keyType == "PRODUCTION"
    input.authenticationContext.tokenType == "JWT"
    input.apiContext.vhost == "localhost"
    input.apiContext.orgId == "carbon.super"
}

envoy_headers[headerKey] {
    some headerKey
    input.transportHeaders[headerKey]
    startswith(headerKey, ":")
}
