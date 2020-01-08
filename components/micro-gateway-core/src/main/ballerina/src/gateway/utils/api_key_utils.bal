import ballerina/crypto;
import ballerina/jwt;
import ballerina/system;
import ballerina/time;
import ballerina/runtime;
import ballerina/http;
import ballerina/stringutils;

# Provide self generated jwt as api key.
# + req - http request
# + return - Returns api key.
public function provideAPIKey(http:Request req) returns string | error {
    crypto:KeyStore keyStore = {
        path: getConfigValue(API_KEY_ISSUER_TOKEN_CONFIG, API_KEY_ISSUER_KEY_STORE_PATH,
        "${ballerina.home}/bre/security/ballerinaKeystore.p12"),
        password: getConfigValue(API_KEY_ISSUER_TOKEN_CONFIG, API_KEY_ISSUER_KEY_STORE_PASSWORD, "ballerina")
    };

    jwt:JwtKeyStoreConfig config = {
        keyStore: keyStore,
        keyAlias: getConfigValue(API_KEY_ISSUER_TOKEN_CONFIG, API_KEY_ISSUER_KEYSTORE_ALIAS, "ballerina"),
        keyPassword: getConfigValue(API_KEY_ISSUER_TOKEN_CONFIG, API_KEY_ISSUER_KEY_STORE_PASSWORD, "ballerina")
    };

    jwt:JwtHeader header = {};
    header.alg = jwt:RS256;
    header.typ = AUTH_SCHEME_JWT;
    header.kid = getConfigValue(API_KEY_ISSUER_TOKEN_CONFIG, API_KEY_ISSUER_KEYSTORE_ALIAS, "ballerina");

    jwt:JwtPayload jwtPayload = {};
    //get authenticated user
    printDebug(API_KEY_UTIL, "get authenticated user");
    runtime:InvocationContext invocationContext = runtime:getInvocationContext();
    AuthenticationContext authContext =  <AuthenticationContext>invocationContext.attributes[AUTHENTICATION_CONTEXT];
    string username = authContext.username;
    
    jwtPayload.sub = username;
    jwtPayload.iss = getConfigValue(API_KEY_ISSUER_TOKEN_CONFIG, API_KEY_ISSUER, "https://localhost:9443/oauth2/token");
    jwtPayload.jti = system:uuid();
    jwtPayload.aud = getConfigValue(API_KEY_ISSUER_TOKEN_CONFIG, API_KEY_ISSUER_AUDIENCE, "http://org.wso2.apimgt/gateway");
    int currentTime = time:currentTime().time / 1000;    //current time in seconds
    int expiryTime = getConfigIntValue(API_KEY_ISSUER_TOKEN_CONFIG, API_KEY_VALIDITY_PERIOD, 600);
    jwtPayload.exp = currentTime + expiryTime;
    jwtPayload.iat = currentTime;


    // Add additional claims
    // var payload = req.getJsonPayload();
    // if (payload is json) {
    //     json|error jsonName = payload.name;
    //     json|error jsonVerson = payload.'version;
    //     json[] apis = [];

    //     //if request has payload
    //     if (jsonName is json && isAPIExists(<@untainted> jsonName.toJsonString())) {
    //         string name = <@untainted>jsonName.toJsonString();
    //         string verson = "";
    //         string basepath = getConfigValue("apikey.issuer.apis" + "." + name , "basepath", " ");
    //         if (jsonVerson is json) {
    //             verson = <@untainted>jsonVerson.toJsonString();
    //             string[] allowedVersionList = split(getConfigValue("apikey.issuer.apis" + "." + name , "versions", " "), ",");

    //             if (!stringutils:equalsIgnoreCase(verson,"")) {
    //                 io:println("63");
    //                 foreach string v in allowedVersionList {
    //                     if (stringutils:equalsIgnoreCase(v,verson)) {
    //                         io:println("66");
    //                         json api = {subscriberTenantDomain: "undefined", name: name, context: basepath + "/" + v, publisher: "undefined", subscriptionTier:"Default", 'version: v };
    //                         apis.push(api);
    //                     }
    //                 }
    //             } else {
    //                 io:println("72");
    //                 foreach string v in allowedVersionList {
    //                     io:println("74");
    //                     json api = {subscriberTenantDomain: "undefined", name: name, context: basepath + "/" + v, publisher: "undefined", subscriptionTier:"Default", 'version: v };
    //                     apis.push(api);                           
    //                 }
    //             }              
    //         } else {
    //             printDebug(API_KEY_UTIL, "error while getting request version.");
    //         }

    //     } else {
    //             printDebug(API_KEY_UTIL, "error while processing request API name.");
    //     }
        
    // } else {
    //     printDebug(API_KEY_UTIL, "error while getting request payload.");
    // }
  
    json[] apis = [];
    int counter = 1;
    while (true) {  
        map<any> apiMap= getConfigMapValue("apikey.issuer.apis." + counter.toString());
        counter =  counter + 1;
        if (apiMap.keys().length() > 0) {           
            string name = <string>apiMap.get("name");
            string basepath = <string>apiMap.get("basepath");
            if (!apiMap.hasKey("versions") || stringutils:equalsIgnoreCase("*", <string>apiMap.get("versions"))) {
                json api = {name: name, context: basepath + "/*" , 'version: "*" };
                apis.push(api);  
            }
            else{
                string allowedVersionsfromConfig = <string>apiMap.get("versions");
                string[] allowedVersionList = split(allowedVersionsfromConfig, ",");
                foreach string v in allowedVersionList {
                    json api = { name: name, context: basepath + "/" + v.trim(), 'version: v.trim() };           
                    apis.push(api);                           
                }
            }                 
        }
        else {
            break;
        }
    }
    map<json[]> customClaims = {};
    json[] subscribedAPIs = apis;
    customClaims["allowedAPIs"] = subscribedAPIs;
    jwtPayload.customClaims = customClaims;

    printDebug(API_KEY_UTIL, "API Key is being issued.. .");
    string | error apiKey = jwt:issueJwt(header, jwtPayload, config);
    printDebug(API_KEY_UTIL, "API Key issuing process completed");
    return apiKey;
}

// # Read the config and check whether the api exists.
// #
// # + apiName - api name.
// # + return - Returns boolean value.
// public function isAPIExists(string apiName) returns boolean {
//     string basepath = getConfigValue("apikey.issuer.apis" + "." + apiName , "basepath", " ");
//     if (!stringutils:equalsIgnoreCase(" ", basepath)) {
//         printDebug(API_KEY_UTIL, "api" + apiName + "exists. basepath: " + basepath);
//         return true;
//     }
//     return false;
// }

# If api key is given. validate the user in sub claim.
#
# + apiKeyToken - api key token string.
# + return - Returns boolean value.
public function validateAPIKey(string apiKeyToken) returns boolean {
    //decode jwt
    [jwt:JwtHeader,jwt:JwtPayload]|jwt:Error decodedJWT = jwt:decodeJwt(apiKeyToken);
    if (decodedJWT is error) {
        printDebug(API_KEY_UTIL, "Error while decoding the JWT token");
        return false;
    }
    [jwt:JwtHeader,jwt:JwtPayload] [jwtHeader,payload] = <[jwt:JwtHeader,jwt:JwtPayload]> decodedJWT;
    map<json>? customClaims = payload?.customClaims;
    string? username = payload?.sub;

    //invocation context
    runtime:InvocationContext invocationContext = runtime:getInvocationContext();  
    AuthenticationContext authenticationContext = {};
    authenticationContext.apiKey = apiKeyToken;
    authenticationContext.callerToken = apiKeyToken;
    authenticationContext.authenticated = false;
    if (username is string) {
        printDebug(API_KEY_UTIL, "set username : " + username);
        authenticationContext.username = username;
    }


    if (customClaims is map<json> && customClaims.hasKey("subscribedAPIs")){
    //handle subscribed
    }

    //////////
    if (customClaims is map<json> && customClaims.hasKey("allowedAPIs")){
        printDebug(API_KEY_UTIL, "allowedAPIs claim found in the jwt");
        //set keytype
        printDebug(API_KEY_UTIL, "set keytype as apikey ");
        authenticationContext.keyType = "apikey";
        invocationContext.attributes[KEY_TYPE_ATTR] = "apikey";

        json subscribedAPIList = customClaims.get("allowedAPIs");
        if (subscribedAPIList is json[]) {  
            APIConfiguration? apiConfig = apiConfigAnnotationMap[<string>invocationContext.attributes["SERVICE_NAME"]];
            if (apiConfig is APIConfiguration) {
                string apiName = apiConfig.name;
                string apiVersion = apiConfig.apiVersion;
                int l = subscribedAPIList.length();
                int index = 0;
                while (index < l) {
                    var subscription = subscribedAPIList[index];
                    if (subscription.name.toString() == apiName && subscription.'version.toString() == apiVersion) {
                        authenticationContext.authenticated = true;
                        printDebug(API_KEY_UTIL, "Found a matching allowed api with name:" + subscription.name.toString() + " version:" + subscription.'version.toString());
                        invocationContext.attributes[AUTHENTICATION_CONTEXT] = authenticationContext;
                        return true;
                    }
                    index += 1;
                }
            }
        }
    }
    invocationContext.attributes[AUTHENTICATION_CONTEXT] = authenticationContext;
    return true;
}