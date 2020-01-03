import ballerina/crypto;
import ballerina/jwt;
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
        keyAlias: getConfigValue(API_KEY_INSTANCE_ID, API_KEY_KEYSTORE_ALIAS, "ballerina"),
        keyPassword: getConfigValue(LISTENER_CONF_INSTANCE_ID, LISTENER_CONF_KEY_STORE_PASSWORD, "ballerina")
    };

    jwt:JwtHeader header = {};
    header.alg = jwt:RS256;
    header.typ = AUTH_SCHEME_JWT;
    header.kid = getConfigValue(API_KEY_INSTANCE_ID, API_KEY_KEYSTORE_ALIAS, "ballerina");

    jwt:JwtPayload payload = {};
    //get authenticated user
    printDebug(API_KEY_UTIL, "get authenticated user");
    runtime:InvocationContext invocationContext = runtime:getInvocationContext();
    AuthenticationContext authContext =  <AuthenticationContext>invocationContext.attributes[AUTHENTICATION_CONTEXT];
    string username = authContext.username;
    
    payload.sub = username;
    payload.iss = getConfigValue(API_KEY_INSTANCE_ID, API_KEY_ISSUER, "https://localhost:9443/oauth2/token");
    payload.jti = system:uuid();
    payload.aud = getConfigValue(API_KEY_INSTANCE_ID, API_KEY_AUDIENCE, "http://org.wso2.apimgt/gateway");
    int currentTime = time:currentTime().time / 1000;    //current time in seconds
    int expiryTime = getConfigIntValue(API_KEY_INSTANCE_ID, API_KEY_VALIDITY_PERIOD, 600);
    payload.exp = currentTime + expiryTime;
    payload.iat = currentTime;

    printDebug(API_KEY_UTIL, "API Key is being issued.. .");
    string | error apiKey = jwt:issueJwt(header, payload, config);
    printDebug(API_KEY_UTIL, "API Key issuing process completed");
    return apiKey;
}



