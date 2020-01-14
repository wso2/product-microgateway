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
    if (getConfigBooleanValue(API_KEY_ISSUER_TOKEN_CONFIG, API_KEY_ISSUER_ENABLED, false)) {
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

        json[] apis = [];
        int counter = 1;
        while (true) {  
            map<any> apiMap= getConfigMapValue(API_KEY_ISSUER_APIS + "." + counter.toString());
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
        map<json> customClaims = {};
        json[] subscribedAPIs = apis;
        customClaims[ALLOWED_APIS] = subscribedAPIs;
        json keyType = PRODUCTION_KEY_TYPE;
        customClaims[KEY_TYPE] = keyType;
        jwtPayload.customClaims = customClaims;

        printDebug(API_KEY_UTIL, "API Key is being issued.. .");
        string | error apiKey = jwt:issueJwt(header, jwtPayload, config);
        printDebug(API_KEY_UTIL, "API Key issuing process completed");
        return apiKey;
    }
    return "Error: API Key issuer is disabled";
}
