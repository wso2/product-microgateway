import ballerinax/java;

# Extract  a directory.
#
# + projectName - Path of the directory to be compressed
# + serviceName - Path of the directory to place the compressed file
# + return - An error if an error occurs during the compression process
public function extractJAR(string projectName, string serviceName) returns map<string>|error {
   handle pjtName = java:fromString(projectName);
   handle servName = java:fromString(serviceName);
   return extract(pjtName, servName);
}

# Compresses a directory.
#
# + Name - Path of the directory to be compressed
# + serv - Path of the directory to place the compressed file
# + return - An error if an error occurs during the compression process
function extract(handle Name, handle serv) returns map<string> | error  = @java:Method {
     name: "extract",
     class: "org.wso2.micro.gateway.core.extrcator.Extract"

} external;