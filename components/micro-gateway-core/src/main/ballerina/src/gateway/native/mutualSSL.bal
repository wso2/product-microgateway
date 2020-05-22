import ballerinax/java;

# Get the Alias name of the cert.
#
# + cert - certicate
# + trustStorePath - truststore location
# + trustStorePassword - truststore password
# + return - An error if an error occurs during the process or name of the certificate alias

public function getAlias(string cert,string trustStorePath,string trustStorePassword) returns handle|error {
    handle cert1 = java:fromString(cert);
    handle trustStorePath1 = java:fromString(trustStorePath);
    handle trustStorePassword1 = java:fromString(trustStorePassword);
    handle|error certAlias = jgetAlias(cert1,trustStorePath1,trustStorePassword1);
    return certAlias;
}

function jgetAlias(handle cert, handle trustStorePath,handle trustStorePassword) returns handle|error = @java:Method {
    name: "getAlias",
    class: "org.wso2.micro.gateway.core.mutualssl.MutualsslRequestInvoker"
} external;

# validate the certificate whether it is exist in the trustore.
#
# + cert - certicate
# + trustStorePath - truststore location
# + trustStorePassword - truststore password
# + return - An error if an error occurs during the process or boolean true/false
public function isExistCert(string cert,string trustStorePath,string trustStorePassword) returns boolean|error {
    handle cert1 = java:fromString(cert);
    handle trustStorePath1 = java:fromString(trustStorePath);
    handle trustStorePassword1 = java:fromString(trustStorePassword);
    boolean|error certt = jisExistCert(cert1,trustStorePath1,trustStorePassword1);
    return certt;
}

function jisExistCert(handle cert, handle trustStorePath,handle trustStorePassword) returns boolean|error = @java:Method {
    name: "isExistCert",
    class: "org.wso2.micro.gateway.core.mutualssl.MutualsslHeaderInvoker"
} external;