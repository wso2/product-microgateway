package org.wso2.micro.gateway.core.handlers;

import org.ballerinalang.jvm.values.MapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Class to dynamically invoke the jwt generators defined.
 */
public class MGWJWTGeneratorInvoker {
    private static final Logger log = LoggerFactory.getLogger("ballerina");
    private static AbstractMGWJWTGenerator abstractMGWJWTGenerator;

    public static boolean loadJWTGeneratorClass(String className,
                                               String dialectURI,
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
        try {
            Class jwtGeneratorClass = MGWJWTGeneratorInvoker.class.getClassLoader().loadClass(className);
            Constructor classConstructor = jwtGeneratorClass
                    .getDeclaredConstructor(String.class, String.class, String.class, String.class, int.class,
                            String.class, boolean.class, int.class, String.class, String.class, MapValue.class);
            abstractMGWJWTGenerator = (AbstractMGWJWTGenerator) classConstructor
                    .newInstance(dialectURI, signatureAlgorithm, keyStorePath, keyStorePassword, jwtExpiryTime,
                            restrictedClaims, cacheEnabled, cacheExpiry, tokenIssuer, tokenAudience, apiDetails);
            return true;
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException
                    | InvocationTargetException | NoSuchMethodException e) {
            log.error("Error while loading the jwt generator class: " + className, e);
        }
        return false;
    }

    public static String invokeGenerateToken(MapValue jwtInfo) {
        return abstractMGWJWTGenerator.generateToken(jwtInfo);
    }

    public static String invokeGetKeystorePath(String fullPath) {
        return AbstractMGWJWTGenerator.getKeyStorePath(fullPath);
    }
}
