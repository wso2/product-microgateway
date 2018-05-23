import ballerina/http;
import ballerina/log;
import ballerina/auth;
import ballerina/config;
import ballerina/runtime;
import ballerina/time;
import ballerina/io;
import ballerina/reflect;

public function isResourceSecured(http:ListenerAuthConfig? resourceLevelAuthAnn, http:ListenerAuthConfig?
    serviceLevelAuthAnn) returns boolean {
    boolean isSecured;
    match resourceLevelAuthAnn.authentication {
        http:Authentication authn => {
            isSecured = authn.enabled;
        }
        () => {
            // if not found at resource level, check in the service level
            match serviceLevelAuthAnn.authentication {
                http:Authentication authn => {
                    isSecured = authn.enabled;
                }
                () => {
                    isSecured = false;
                }
            }
        }
    }
    return isSecured;
}

@Description { value: "Tries to retrieve the annotation value for authentication hierarchically - first from the resource
level
and then from the service level, if its not there in the resource level" }
@Param { value: "annotationPackage: annotation package name" }
@Param { value: "annotationName: annotation name" }
@Param { value: "annData: array of annotationData instances" }
@Return { value: "ListenerAuthConfig: ListenerAuthConfig instance if its defined, else nil" }
public function getAuthAnnotation(string annotationPackage, string annotationName, reflect:annotationData[] annData)
                    returns (http:ListenerAuthConfig?) {
    if (lengthof annData == 0) {
        return ();
    }
    reflect:annotationData|() authAnn;
    foreach ann in annData {
        if (ann.name == annotationName && ann.pkgName == annotationPackage) {
            authAnn = ann;
            break;
        }
    }
    match authAnn {
        reflect:annotationData annData1 => {
            if (annotationName == RESOURCE_ANN_NAME) {
                http:HttpResourceConfig resourceConfig = check <http:HttpResourceConfig>annData1.value;
                return resourceConfig.authConfig;
            } else if (annotationName == SERVICE_ANN_NAME) {
                http:HttpServiceConfig serviceConfig = check <http:HttpServiceConfig>annData1.value;
                return serviceConfig.authConfig;
            } else {
                return ();
            }
        }
        () => {
            return ();
        }
    }
}


@Description { value: "Retrieve the annotation related to resources" }
@Return { value: "HttpResourceConfig: HttpResourceConfig instance if its defined, else nil" }
public function getResourceConfigAnnotation(reflect:annotationData[] annData)
                    returns (http:HttpResourceConfig) {
    if (lengthof annData == 0) {
        return {};
    }
    reflect:annotationData|() authAnn;
    foreach ann in annData {
        if (ann.name == RESOURCE_ANN_NAME && ann.pkgName == ANN_PACKAGE) {
            authAnn = ann;
            break;
        }
    }
    match authAnn {
        reflect:annotationData annData1 => {
            http:HttpResourceConfig resourceConfig = check <http:HttpResourceConfig>annData1.value;
            return resourceConfig;
        }
        () => {
            return {};
        }
    }
}

@Description { value: "Retrieve the annotation related to resource level Tier" }
@Return { value: "TierConfiguration: TierConfiguration instance if its defined, else nil" }
public function getResourceLevelTier(reflect:annotationData[] annData)
                    returns (TierConfiguration) {
    if (lengthof annData == 0) {
        return {};
    }
    reflect:annotationData|() tierAnn;
    foreach ann in annData {
        if (ann.name == RESOURCE_TIER_ANN_NAME && ann.pkgName == RESOURCE_TIER_ANN_PACKAGE) {
            tierAnn = ann;
            break;
        }
    }
    match tierAnn {
        reflect:annotationData annData1 => {
            TierConfiguration resourceLevelTier = check <TierConfiguration>annData1.value;
            return resourceLevelTier;
        }
        () => {
            return {};
        }
    }
}

@Description { value: "Retrieve the annotation related to service" }
@Return { value: "HttpServiceConfig: HttpResourceConfig instance if its defined, else nil" }
public function getServiceConfigAnnotation(reflect:annotationData[] annData)
                    returns (http:HttpServiceConfig) {
    if (lengthof annData == 0) {
        return {};
    }
    reflect:annotationData|() authAnn;
    foreach ann in annData {
        if (ann.name == SERVICE_ANN_NAME && ann.pkgName == ANN_PACKAGE) {
            authAnn = ann;
            break;
        }
    }
    match authAnn {
        reflect:annotationData annData1 => {
            http:HttpServiceConfig serviceConfig = check <http:HttpServiceConfig>annData1.value;
            return serviceConfig;
        }
        () => {
            return {};
        }
    }
}

@Description { value: "Retrieve the key validation request dto from filter context" }
@Return { value: "api key validation request dto" }
public function getKeyValidationRequestObject(http:FilterContext context) returns APIKeyValidationRequestDto {
    APIKeyValidationRequestDto apiKeyValidationRequest = {};
    http:HttpServiceConfig httpServiceConfig = getServiceConfigAnnotation(reflect:getServiceAnnotations
        (context.serviceType));
    http:HttpResourceConfig httpResourceConfig = getResourceConfigAnnotation
    (reflect:getResourceAnnotations(context.serviceType, context.resourceName));
    apiKeyValidationRequest.context = httpServiceConfig.basePath;
    apiKeyValidationRequest.apiVersion = getVersionFromServiceAnnotation(reflect:getServiceAnnotations
        (context.serviceType)).apiVersion;
    apiKeyValidationRequest.requiredAuthenticationLevel = "Any";
    apiKeyValidationRequest.clientDomain = "*";
    apiKeyValidationRequest.matchingResource = httpResourceConfig.path;
    apiKeyValidationRequest.httpVerb = httpResourceConfig.methods[0];
    // TODO get correct verb
    return apiKeyValidationRequest;

}

@Description {value:"Creates an instance of FilterResult"}
@Param {value:"canProceed: authorization status for the request"}
@Param {value:"statusCode: status code for the filter request"}
@Param {value:"message: response message from the filter"}
@Return {value:"FilterResult: Authorization result to indicate if the request can proceed or not"}
function createFilterResult (boolean canProceed, int statusCode, string message) returns (http:FilterResult) {
    http:FilterResult requestFilterResult = {};
    requestFilterResult = {canProceed:canProceed, statusCode:statusCode, message:message};
    return requestFilterResult;
}

@Description {value:"Creates an instance of FilterResult"}
@Param {value:"authenticated: filter status for the request"}
@Return {value:"FilterResult: Authorization result to indicate if the request can proceed or not"}
function createAuthnResult(boolean authenticated) returns (http:FilterResult) {
    http:FilterResult requestFilterResult = {};
    if (authenticated) {
        requestFilterResult = {canProceed: true, statusCode: 200, message: "Successfully authenticated"};
    } else {
        requestFilterResult = {canProceed: false, statusCode: 401, message: "Authentication failure"};
    }
    return requestFilterResult;
}

public function getVersionFromServiceAnnotation(reflect:annotationData[] annData) returns VersionConfiguration {
    if (lengthof annData == 0) {
        return {};
    }
    reflect:annotationData|() versionAnn;
    foreach ann in annData {
        if (ann.name == VERSION_ANN_NAME && ann.pkgName == GATEWAY_ANN_PACKAGE) {
            versionAnn = ann;
            break;
        }
    }
    match versionAnn {
        reflect:annotationData annData1 => {
            VersionConfiguration versionConfig = check <VersionConfiguration>annData1.value;
            return versionConfig;
        }
        () => {
            return {};
        }
    }
}

public function getTenantFromBasePath(string basePath) returns string {
    string[] splittedArray = basePath.split("/");
    return splittedArray[lengthof splittedArray - 1];
}


public function isAccessTokenExpired(APIKeyValidationDto apiKeyValidationDto) returns boolean {
    int validityPeriod = check <int>apiKeyValidationDto.validityPeriod;
    int issuedTime = check <int>apiKeyValidationDto.issuedTime;
    int timestampSkew = 5000;
    // TODO : make this configurable;
    int currentTime = time:currentTime().time;
    int intMaxValue = 9223372036854775807;
    if (validityPeriod != intMaxValue &&
            // For cases where validityPeriod is closer to int.MAX_VALUE (then issuedTime + validityPeriod would spill
            // over and would produce a negative value)
            (currentTime - timestampSkew) > validityPeriod) {
        if ((currentTime - timestampSkew) > (issuedTime + validityPeriod)) {
            apiKeyValidationDto.validationStatus = API_AUTH_INVALID_CREDENTIALS;
            return true;
        }
    }
    return false;
}
public function getContext(http:FilterContext context) returns (string) {
    http:HttpServiceConfig httpServiceConfig = getServiceConfigAnnotation(reflect:getServiceAnnotations
        (context.serviceType));
    return httpServiceConfig.basePath;

}

public function getClientIp(http:Request request) returns (string) {
    string clientIp;
    string header = "";
    string[] headerNames = request.getHeaderNames();
    foreach headerName in headerNames {
        string headerValue = untaint request.getHeader(headerName);
    }
    if(request.hasHeader(X_FORWARD_FOR_HEADER)) {
        header = request.getHeader(X_FORWARD_FOR_HEADER);
    }
    //TODO need to get the IP from REMOTE_ADDR
    clientIp = header;
    int idx = header.indexOf(",");
    if (idx > -1) {
        clientIp = clientIp.substring(0, idx);
    }
    return clientIp;
}

public function extractAccessToken (http:Request req) returns (string|error) {
    string authHeader = req.getHeader(AUTH_HEADER);
    string[] authHeaderComponents = authHeader.split(" ");
    if(lengthof authHeaderComponents != 2){
        return handleError("Incorrect bearer authentication header format");
    }
    return authHeaderComponents[1];
}

public function handleError(string message) returns (error) {
    error e = {message: message};
    return e;
}
public function getTenantDomain(http:FilterContext context) returns (string) {
    // todo: need to implement to get tenantDomain
    string apiContext = getContext(context);
    string[] splittedContext = apiContext.split("/");
    if (lengthof splittedContext > 3){
        // this check if basepath have /t/domain in
        return splittedContext[2];
    } else {
        return SUPER_TENANT_DOMAIN_NAME;
    }
}
public function getApiName(http:FilterContext context) returns (string) {
    string serviceName = context.serviceName;
    return serviceName.split("_")[0];
}
