import ballerinax/java;

# Get the Alias name of the certificate used in handshake.
#
# + cert - Certicate used in Mutual SSL handshake
# + return - Name of the certificate alias or error occurs during the process

public function getAlias(string cert) returns handle|error {
    handle certificate = java:fromString(cert);
    handle|error certAlias = jgetAlias(certificate);
    return certAlias;
}

function jgetAlias(handle cert) returns handle|error = @java:Method {
    name: "getAlias",
    class: "org.wso2.micro.gateway.core.mutualssl.MutualsslWithoutLoadBalancerHeader"
} external;

# Get the Alias name of the cert used in header append by load balancer.
#
# + cert - Certicate append in the header
# + return - Name of the certificate alias or error occurs during the process
public function getAliasAFromHeaderCert(string cert) returns handle|error {
    handle certificate = java:fromString(cert);
    handle|error certAlias = jisExistCert(certificate);
    return certAlias;
}

function jisExistCert(handle cert) returns handle|error = @java:Method {
    name: "getAliasAFromHeaderCert",
    class: "org.wso2.micro.gateway.core.mutualssl.MutualsslWithLoadBalancerHeader"
} external;

# Load the trustore in keystore.
#
# + trustStorePath - truststore location
# + trustStorePassword - truststore password
function loadKeyStore(string trustStorePath,string trustStorePassword) {
    handle trustStorePath1 = java:fromString(trustStorePath);
    handle trustStorePassword1 = java:fromString(trustStorePassword);
    jloadKeyStore(trustStorePath1, trustStorePassword1);
}

function jloadKeyStore(handle trustStorePath,handle trustStorePassword) = @java:Method {
    name: "loadKeyStore",
    class: "org.wso2.micro.gateway.core.mutualssl.LoadKeyStore"
} external;
