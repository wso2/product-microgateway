import ballerina/crypto;
import ballerina/http;
import ballerina/jwt;
import ballerina/lang.'int as ints;
import ballerina/runtime;
import ballerina/stringutils;
import ballerina/system;
import ballerina/time;

# Provide self generated jwt as api key.
# + req - http request
# + return - Returns api key.
public function generateAPIKey(http:Request req) returns string | error {
    if (getConfigBooleanValue(API_KEY_ISSUER_TOKEN_CONFIG, API_KEY_ISSUER_ENABLED, false)) {
        crypto:KeyStore keyStore = {
            path: getConfigValue(API_KEY_ISSUER_TOKEN_CONFIG, KEY_STORE_PATH,
            "${ballerina.home}/bre/security/ballerinaKeystore.p12"),
            password: getConfigValue(API_KEY_ISSUER_TOKEN_CONFIG, KEY_STORE_PASSWORD, "ballerina")
        };

        jwt:JwtKeyStoreConfig config = {
            keyStore: keyStore,
            keyAlias: getConfigValue(API_KEY_ISSUER_TOKEN_CONFIG, CERTIFICATE_ALIAS, "ballerina"),
            keyPassword: getConfigValue(API_KEY_ISSUER_TOKEN_CONFIG, KEY_STORE_PASSWORD, "ballerina")
        };

        jwt:JwtHeader header = {};
        header.alg = jwt:RS256;
        header.typ = AUTH_SCHEME_JWT;
        header.kid = getConfigValue(API_KEY_ISSUER_TOKEN_CONFIG, CERTIFICATE_ALIAS, "ballerina");

        jwt:JwtPayload jwtPayload = {};
        //get authenticated user
        printDebug(API_KEY_UTIL, "get authenticated user");
        runtime:InvocationContext invocationContext = runtime:getInvocationContext();
        AuthenticationContext authContext = <AuthenticationContext>invocationContext.attributes[AUTHENTICATION_CONTEXT];
        string username = authContext.username;

        jwtPayload.sub = username;
        jwtPayload.iss = getConfigValue(API_KEY_ISSUER_TOKEN_CONFIG, ISSUER, "https://localhost:9443/oauth2/token");
        jwtPayload.jti = system:uuid();
        jwtPayload.aud = getConfigValue(API_KEY_ISSUER_TOKEN_CONFIG, AUDIENCE, "http://org.wso2.apimgt/gateway");
        int currentTime = time:currentTime().time / 1000;        //current time in seconds
        int expiryTime = getExpiryTime(req);

        if (expiryTime > 0) {
            jwtPayload.exp = currentTime + expiryTime;
        }

        jwtPayload.iat = currentTime;
        json[] apis = [];
        int counter = 1;
        while (true) {
            map<any> apiMap = getConfigMapValue(API_KEY_ISSUER_APIS + "." + counter.toString());
            counter = counter + 1;
            if (apiMap.keys().length() > 0) {
                string name = <string>apiMap.get("name");
                if (!apiMap.hasKey("versions") || stringutils:equalsIgnoreCase("*", <string>apiMap.get("versions"))) {
                    json api = {name: name, 'version: "*"};
                    apis.push(api);
                }
                else {
                    string allowedVersionsfromConfig = <string>apiMap.get("versions");
                    string[] allowedVersionList = split(allowedVersionsfromConfig, ",");
                    foreach string v in allowedVersionList {
                        json api = {name: name, 'version: v.trim()};
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

public function getExpiryTime(http:Request req) returns @tainted (int) {
    var payload = req.getJsonPayload();
    int expiryTime = getConfigIntValue(API_KEY_ISSUER_TOKEN_CONFIG, API_KEY_VALIDITY_TIME, -1);
    printDebug(API_KEY_UTIL, "Validity Period in config: " + expiryTime.toString());

    //if payload > 0 and (payload < expirytime || expirytime < 0) from config  

    if (payload is json) {
        map<json> payloadMap = <map<json>>payload;
        if (payloadMap.hasKey(API_KEY_VALIDITY_TIME)) {
            var expiryTimefromPayload = ints:fromString(payloadMap[API_KEY_VALIDITY_TIME].toString());
            if (expiryTimefromPayload is int && expiryTimefromPayload > 0 && (expiryTime < 0 || expiryTime > expiryTimefromPayload)) {
                expiryTime = expiryTimefromPayload;
            }
        }
    }
    return expiryTime;
}

# api key authorization
#
# + apiKeyToken - api key token string.
# + return - Returns boolean value.
public function validateAPIKey(string apiKeyToken) returns boolean {
    boolean validated = false;
    boolean validateAllowedAPIs = getConfigBooleanValue(API_KEY_INSTANCE_ID, API_KEY_VALIDATE_ALLOWED_APIS, false);

    runtime:InvocationContext invocationContext = runtime:getInvocationContext();
    runtime:AuthenticationContext? authContext = invocationContext?.authenticationContext;
    if (authContext is runtime:AuthenticationContext) {
        printDebug(API_KEY_UTIL, "Set authContext scheme to " + AUTH_SCHEME_API_KEY);
        authContext.scheme = AUTH_SCHEME_API_KEY;
    }
    //decode jwt
    [jwt:JwtHeader, jwt:JwtPayload] | jwt:Error decodedJWT = jwt:decodeJwt(apiKeyToken);
    if (decodedJWT is error) {
        printError(API_KEY_UTIL, "Error while decoding the JWT token");
        return false;
    }
    //get payload
    (jwt:JwtPayload | error) payload = getDecodedJWTPayload(apiKeyToken);
    if (payload is jwt:JwtPayload) {
        json subscribedAPIList = [];
        //get allowed apis
        map<json>? customClaims = payload?.customClaims;
        if (customClaims is map<json>) {
            if (customClaims.hasKey(SUBSCRIBED_APIS)) {
                printDebug(API_KEY_UTIL, "subscribedAPIs claim found in the jwt");
                subscribedAPIList = customClaims.get(SUBSCRIBED_APIS);
            } else if (customClaims.hasKey(ALLOWED_APIS)) {
                printDebug(API_KEY_UTIL, "allowedAPIs claim found in the jwt");
                subscribedAPIList = customClaims.get(ALLOWED_APIS);
            }
        }
        if (subscribedAPIList is json[]) {
            if (validateAllowedAPIs && subscribedAPIList.length() < 1) {
                printError(API_KEY_UTIL, "subscribedAPI list is empty");
                return false;
            }
            validated = handleSubscribedAPIs(apiKeyToken, payload, subscribedAPIList, validateAllowedAPIs);
            if (validated || !validateAllowedAPIs) {
                printDebug(API_KEY_UTIL, "Subscriptions validated.");
                return true;
            }
        }
    }
    printError(KEY_SUBSCRIPTION_FILTER, "Failed to validate API.");
    return false;
}

public function setAPIKeyAuth(string inName, string name) {
    runtime:InvocationContext invocationContext = runtime:getInvocationContext();
    invocationContext.attributes["apiKeyIn"] = inName;
    invocationContext.attributes["apiKeyName"] = name;
}

public function getAPIKeyAuth() returns [string, string] | error {
    runtime:InvocationContext invocationContext = runtime:getInvocationContext();
    if (invocationContext.attributes.hasKey("apiKeyIn") && invocationContext.attributes.hasKey("apiKeyName")) {
        return [<string>invocationContext.attributes["apiKeyIn"], <string>invocationContext.attributes["apiKeyName"]];
    } else {
        printDebug(API_KEY_UTIL, "API key is missing in invocation context");
        return error("API key is missing in invocation context");
    }
}
