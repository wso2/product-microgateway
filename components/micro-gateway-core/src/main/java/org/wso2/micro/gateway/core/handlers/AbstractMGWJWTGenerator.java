package org.wso2.micro.gateway.core.handlers;

import com.nimbusds.jwt.JWTClaimsSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ballerinalang.jvm.values.MapValue;
import org.wso2.micro.gateway.core.Constants;
import org.wso2.micro.gateway.core.utils.ErrorUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 *  Abstract class for generate JWT for backend claims
 */
public abstract class AbstractMGWJWTGenerator {
    private static final Log logger = LogFactory.getLog(AbstractMGWJWTGenerator.class);
    private static final String NONE = "NONE";
    private static final String SHA256_WITH_RSA = "SHA256withRSA";
    private static volatile long ttl = -1L;
    private String dialectURI;
    private String signatureAlgorithm;
    private String keyStorePath;
    private String keyStorePassword;
    private int jwtExpiryTime;
    private ArrayList<String> restrictedClaims;
    private boolean cacheEnabled;
    private int cacheExpiry;
    private String tokenIssuer;
    private String tokenAudience;
    private MapValue apiDetails;

    public AbstractMGWJWTGenerator(String dialectURI,
                                   String signatureAlgorithm,
                                   String keyStorePath,
                                   String keyStorePassword,
                                   int jwtExpiryTime,
                                   String restrictedClaims,
                                   boolean cacheEnabled,
                                   int cacheExpiry,
                                   String tokenIssuer,
                                   String tokenAudience,
                                   MapValue apiDetails) {
        this.keyStorePath = keyStorePath;
        this.keyStorePassword = keyStorePassword;
        this.jwtExpiryTime = jwtExpiryTime;
        this.dialectURI = dialectURI;
        this.signatureAlgorithm = signatureAlgorithm;
        this.cacheEnabled = cacheEnabled;
        this.cacheExpiry = cacheExpiry;
        this.tokenIssuer = tokenIssuer;
        this.tokenAudience = tokenAudience;
        this.apiDetails = apiDetails;
        if (restrictedClaims.equals("")) {
            this.restrictedClaims = new ArrayList<>();
        } else {
            this.restrictedClaims = new ArrayList<>(Arrays.asList(restrictedClaims.split(",")));
        }
    }

    public MapValue getApiDetails() {
        return apiDetails;
    }

    public void setApiDetails(MapValue apiDetails) {
        this.apiDetails = apiDetails;
    }

    public String getTokenAudience() {
        return tokenAudience;
    }

    public void setTokenAudience(String tokenAudience) {
        this.tokenAudience = tokenAudience;
    }

    public String getTokenIssuer() {
        return tokenIssuer;
    }

    public void setTokenIssuer(String tokenIssuer) {
        this.tokenIssuer = tokenIssuer;
    }

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    public int getCacheExpiry() {
        return cacheExpiry;
    }

    public void setCacheExpiry(int cacheExpiry) {
        this.cacheExpiry = cacheExpiry;
    }

    public ArrayList<String> getRestrictedClaims() {
        return restrictedClaims;
    }

    public void setRestrictedClaims(ArrayList<String> restrictedClaims) {
        this.restrictedClaims = restrictedClaims;
    }

    public String getKeyStorePath() {
        return keyStorePath;
    }

    public void setKeyStorePath(String keyStorePath) {
        this.keyStorePath = keyStorePath;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public String getDialectURI() {
        return dialectURI;
    }

    public void setDialectURI(String dialectURI) {
        this.dialectURI = dialectURI;
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(String signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public int getJwtExpiryTime() {
        return jwtExpiryTime;
    }

    public void setJwtExpiryTime(int jwtExpiryTime) {
        this.jwtExpiryTime = jwtExpiryTime;
    }

    /**
     * Used to generate the JWT token
     */
    public String generateToken(MapValue jwtInfo) {
        String jwtHeader = buildHeader();
        String jwtBody = buildBody(jwtInfo);
        String base64UrlEncodedHeader = "";
        if (jwtHeader != null) {
            base64UrlEncodedHeader = encode(jwtHeader.getBytes(Charset.defaultCharset()));
        }
        String base64UrlEncodedBody = "";
        if (jwtBody != null) {
            base64UrlEncodedBody = encode(jwtBody.getBytes());
        }
        if (SHA256_WITH_RSA.equals(signatureAlgorithm)) {
            String assertion = base64UrlEncodedHeader + '.' + base64UrlEncodedBody;
            //get the assertion signed
            byte[] signedAssertion = signJWT(assertion);
            if (logger.isDebugEnabled()) {
                logger.debug("signed assertion value : " + new String(signedAssertion, Charset.defaultCharset()));
            }
            String base64UrlEncodedAssertion = encode(signedAssertion);
            return base64UrlEncodedHeader + '.' + base64UrlEncodedBody + '.' + base64UrlEncodedAssertion;
        } else {
            return base64UrlEncodedHeader + '.' + base64UrlEncodedBody + '.';
        }
    }

    /**
     * Used to build the JWT header
     */
    public String buildHeader() {
        String jwtHeader = null;
        if (NONE.equals(signatureAlgorithm)) {
            StringBuilder jwtHeaderBuilder = new StringBuilder();
            jwtHeaderBuilder.append("{\"typ\":\"JWT\",");
            jwtHeaderBuilder.append("\"alg\":\"");
            jwtHeaderBuilder.append("none");
            jwtHeaderBuilder.append('\"');
            jwtHeaderBuilder.append('}');

            jwtHeader = jwtHeaderBuilder.toString();

        } else if (SHA256_WITH_RSA.equals(signatureAlgorithm)) {
            jwtHeader = addCertToHeader();
        }
        return jwtHeader;
    }

    /**
     * Used to sign the JWT using the keystore
     */
    public byte[] signJWT(String assertion) {
        FileInputStream is = null;
        try {
            is = new FileInputStream(keyStorePath);
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            String alias = "ballerina";
            keystore.load(is, alias.toCharArray());
            Key key = keystore.getKey(alias, keyStorePassword.toCharArray());
            Key privateKey = null;
            if (key instanceof PrivateKey) {
                privateKey = key;
            }
            //initialize signature with private key and algorithm
            Signature signature = Signature.getInstance(signatureAlgorithm);
            signature.initSign((PrivateKey) privateKey);
            //update signature with data to be signed
            byte[] dataInBytes = assertion.getBytes(Charset.defaultCharset());
            signature.update(dataInBytes);
            //sign the assertion and return the signature
            return signature.sign();
        } catch (NoSuchAlgorithmException | KeyStoreException | SignatureException | InvalidKeyException |
                   UnrecoverableKeyException | IOException | CertificateException e) {
            logger.error("Error occurred while signing the JWT");
            throw ErrorUtils.getBallerinaError("Error occurred while signing the JWT", e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    logger.error("IO Exception");
                    throw ErrorUtils.getBallerinaError("IO Exception", e);
                }
            }
        }
    }

    /**
     * Used to get the expiration time of the token
     */
    public long getTTL() {
        if (ttl != -1) {
            return ttl;
        }
        synchronized (AbstractMGWJWTGenerator.class) {
            if (ttl != -1) {
                return ttl;
            }
            if (cacheEnabled) {
                ttl = cacheExpiry;
            } else {
                ttl = jwtExpiryTime;
            }
            return ttl;
        }
    }

    /**
     * Used to add the certificate from the keystore to the header
     */
    public String addCertToHeader() {
        FileInputStream is = null;
        try {
            is = new FileInputStream(keyStorePath);
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(is, "ballerina".toCharArray());
            Certificate publicCert = keystore.getCertificate("ballerina");

            //generate the SHA-1 thumbprint of the certificate
            MessageDigest digestValue = MessageDigest.getInstance("SHA-1");
            byte[] der = publicCert.getEncoded();
            digestValue.update(der);
            byte[] digestInBytes = digestValue.digest();
            String publicCertThumbprint = hexify(digestInBytes);
            String base64UrlEncodedThumbPrint;
            base64UrlEncodedThumbPrint = java.util.Base64.getUrlEncoder()
                    .encodeToString(publicCertThumbprint.getBytes("UTF-8"));
            StringBuilder jwtHeader = new StringBuilder();
            //Sample header
            //{"typ":"JWT", "alg":"SHA256withRSA", "x5t":"a_jhNus21KVuoFx65LmkW2O_l10"}
            //{"typ":"JWT", "alg":"[2]", "x5t":"[1]"}
            jwtHeader.append("{\"typ\":\"JWT\",");
            jwtHeader.append("\"alg\":\"");
            jwtHeader.append("RS256");
            jwtHeader.append("\",");

            jwtHeader.append("\"x5t\":\"");
            jwtHeader.append(base64UrlEncodedThumbPrint);
            jwtHeader.append('\"');

            jwtHeader.append('}');
            return jwtHeader.toString();
        } catch (IOException | CertificateException | NoSuchAlgorithmException |
                   KeyStoreException e) {
            logger.error("Error occurred while adding certificate to the header of JWT");
            throw ErrorUtils.getBallerinaError("Error occurred while adding certificate to the header of JWT", e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    logger.error("IO Exception");
                    throw ErrorUtils.getBallerinaError("IO Exception", e);
                }
            }
        }
    }

    /**
     * Used to build the body with claims
     */
    public String buildBody(MapValue jwtInfo) {
        JWTClaimsSet.Builder jwtClaimSetBuilder = new JWTClaimsSet.Builder();
        Map<String, Object> claims = populateStandardClaims(jwtInfo);
        Map<String, Object> customClaims = populateCustomClaims(jwtInfo, restrictedClaims);
        for (Map.Entry<String, Object> claimEntry : customClaims.entrySet()) {
            if (!claims.containsKey(claimEntry.getKey())) {
                claims.put(claimEntry.getKey(), claimEntry.getValue());
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Claim key " + claimEntry.getKey() + " already exist");
                }
            }
        }
        for (Map.Entry<String, Object> claimEntry : claims.entrySet()) {
            jwtClaimSetBuilder.claim(claimEntry.getKey(), claimEntry.getValue());
        }
        JWTClaimsSet jwtClaimsSet = jwtClaimSetBuilder.build();
        return jwtClaimsSet.toJSONObject().toString();
    }

    /**
     * Used for base64 encoding
     */
    public String encode(byte[] stringToBeEncoded) {
        return java.util.Base64.getUrlEncoder().encodeToString(stringToBeEncoded);
    }

    //Helper method to hexify a byte array
    public String hexify(byte bytes[]) {
        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7',
                '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

        StringBuilder buf = new StringBuilder(bytes.length * 2);
        for (byte aByte : bytes) {
            buf.append(hexDigits[(aByte & 0xf0) >> 4]);
            buf.append(hexDigits[aByte & 0x0f]);
        }
        return buf.toString();
    }

    /**
     * Used to get the keystore path
     */
    public static String getKeyStorePath(String fullPath) {
        String homePathConst = "\\$\\{mgw-runtime.home}";
        String homePath = System.getProperty(Constants.RUNTIME_HOME_PATH);
        String correctPath = fullPath.replaceAll(homePathConst, homePath);
        return correctPath;
    }

    /**
     * Used to add claims to the claim set
     */
    public void addClaim(MapValue claimInToken,
                         ArrayList<String> restrictedClaims,
                         Map<String, Object> claims) {
        for (Object key: claimInToken.getKeys()) {
            if (!restrictedClaims.contains(key.toString())) {
                try {
                    claims.put(key.toString(), claimInToken.getStringValue(key.toString()));
                } catch (ClassCastException e1) {
                    try {
                        claims.put(key.toString(), claimInToken.getIntValue(key.toString()));
                    } catch (ClassCastException e2) {
                        try {
                            claims.put(key.toString(), claimInToken.getFloatValue(key.toString()));
                        } catch (ClassCastException e3) {
                            try {
                                claims.put(key.toString(), claimInToken.getArrayValue(key.toString()).getJSONString());
                            } catch (ClassCastException e4) {
                                try {
                                    MapValue mapValue = claimInToken.getMapValue(key.toString());
                                    Map<String, Object> subClaims = new HashMap<>();
                                    addClaim(mapValue, restrictedClaims, subClaims);
                                    claims.put(key.toString(), subClaims);
                                } catch (ClassCastException e5) {
                                    try {
                                        claims.put(key.toString(), claimInToken.getBooleanValue(key.toString()));
                                    } catch (ClassCastException e6) {
                                        try {
                                            claims.put(key.toString(), claimInToken.getObjectValue(key.toString()));
                                        } catch (ClassCastException e7) {
                                            logger.error("Failed to convert claim value");
                                            throw ErrorUtils.getBallerinaError("Failed to convert claim value", e7);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public abstract Map<String, Object> populateStandardClaims(MapValue jwtInfo);
    public abstract Map<String, Object> populateCustomClaims(MapValue jwtInfo, ArrayList<String> restrictedClaims);
}
