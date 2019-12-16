import ballerina/crypto;
import ballerina/jwt;
import ballerina/log;
import ballerina/system;
import ballerina/time;
import ballerina/runtime;

# Provide self generated jwt as api key.
#
# + return - Returns api key.
public function provideAPIKey() returns string | error {
    crypto:KeyStore keyStore = {
        path: getConfigValue(LISTENER_CONF_INSTANCE_ID, LISTENER_CONF_KEY_STORE_PATH,
        "${ballerina.home}/bre/security/ballerinaKeystore.p12"),
        password: getConfigValue(LISTENER_CONF_INSTANCE_ID, LISTENER_CONF_KEY_STORE_PASSWORD, "ballerina")
    };

    jwt:JwtKeyStoreConfig config = {
        keyStore: keyStore,
        keyAlias: getConfigValue(API_KEY_INSTANCE_ID, KEYSTORE_ALIAS, "ballerina"),
        keyPassword: getConfigValue(LISTENER_CONF_INSTANCE_ID, LISTENER_CONF_KEY_STORE_PASSWORD, "ballerina")
    };

    jwt:JwtHeader header = {};
    header.alg = jwt:RS256;
    header.typ = AUTH_SCHEME_JWT;
    header.kid = getConfigValue(API_KEY_INSTANCE_ID, KEYSTORE_ALIAS, "ballerina");

    jwt:JwtPayload payload = {};
    //get authenticated user
    runtime:InvocationContext invocationContext = runtime:getInvocationContext();
    AuthenticationContext authContext =  <AuthenticationContext>invocationContext.attributes[AUTHENTICATION_CONTEXT];
    string username = authContext.username;
    
    payload.sub = username;
    payload.iss = getConfigValue(JWT_INSTANCE_ID, ISSUER, "https://localhost:9443/oauth2/token");
    payload.jti = system:uuid();
    payload.aud = getConfigValue(JWT_INSTANCE_ID, AUDIENCE, "http://org.wso2.apimgt/gateway");
    int currentTime = time:currentTime().time / 1000;    //current time in seconds
    int expiryTime = getConfigIntValue(API_KEY_INSTANCE_ID, API_KEY_VALIDITY_PERIOD, 600);
    payload.exp = currentTime + expiryTime;
    payload.iat = currentTime;
    // Add custom claims
    map<boolean> customClaims = {};
    customClaims[API_KEY]=true;
    payload.customClaims = customClaims;

    log:printDebug("API Key is being issued.. .");
    string | error jwt = jwt:issueJwt(header, payload, config);
    log:printDebug("API Key issuing process completed");
    return jwt;
}

# If api key is given. validate the user in sub claim.
#
# + apiKeyToken - api key token string.
# + return - Returns boolean value.
public function validateIfAPIKey(string apiKeyToken) returns boolean {
     [jwt:JwtHeader,jwt:JwtPayload]|jwt:Error decodedJWT = jwt:decodeJwt(apiKeyToken);
     if (decodedJWT is error) {
         log:printDebug("Error while decoding the JWT token");
         return false;
     }
     [jwt:JwtHeader,jwt:JwtPayload] [jwtHeader,payload] = <[jwt:JwtHeader,jwt:JwtPayload]> decodedJWT;
     map<json>? customClaims = payload?.customClaims;
     if (customClaims is map<json> && customClaims.hasKey(API_KEY)){
        log:printDebug("API Key claim is in the token.");
        string? subject = payload?.sub;
        return (subject is string) ? isUserExists( <@untainted> subject) : false;
     }
    return true;
}


