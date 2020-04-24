package org.wso2.micro.gateway.core.jwtgenerator;

import org.ballerinalang.jvm.values.ArrayValue;
import org.ballerinalang.jvm.values.ArrayValueImpl;
import org.ballerinalang.jvm.values.MapValue;
import org.ballerinalang.jvm.values.MapValueImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.micro.gateway.core.Constants;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

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
                                                String certificateAlias,
                                                String privateKeyAlias,
                                                int jwtExpiryTime,
                                                String[] restrictedClaims,
                                                boolean cacheEnabled,
                                                int cacheExpiry,
                                                String tokenIssuer,
                                                String tokenAudience) {
        try {
            Class jwtGeneratorClass = MGWJWTGeneratorInvoker.class.getClassLoader().loadClass(className);
            Constructor classConstructor = jwtGeneratorClass
                    .getDeclaredConstructor(String.class, String.class, String.class, String.class, String.class,
                            String.class, int.class, String[].class, boolean.class, int.class, String.class,
                            String.class);
            abstractMGWJWTGenerator = (AbstractMGWJWTGenerator) classConstructor
                    .newInstance(dialectURI, signatureAlgorithm, keyStorePath, keyStorePassword, certificateAlias,
                            privateKeyAlias, jwtExpiryTime, restrictedClaims, cacheEnabled, cacheExpiry, tokenIssuer,
                            tokenAudience);
            return true;
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException
                    | InvocationTargetException | NoSuchMethodException e) {
            log.error("Error while loading the jwt generator class: " + className, e);
        }
        return false;
    }

    /**
     * Invoke token generation method
     */
    public static String invokeGenerateToken(MapValue jwtInfo, MapValue apiDetails) {
        Map<String, Object> jwtInfoMap = convertMapValueToMap(jwtInfo);
        Map<String, Object> apiDetailsMap = convertMapValueToMap(apiDetails);
        abstractMGWJWTGenerator.setApiDetails(apiDetailsMap);
        return abstractMGWJWTGenerator.generateToken(jwtInfoMap);
    }

    public static String invokeGetKeystorePath(String fullPath) {
        return getKeyStorePath(fullPath);
    }

    /**
     * Convert MapValue to Map
     */
    public static Map<String, Object> convertMapValueToMap(MapValue mapValue) {
        Map<String, Object> map = new HashMap<>();
        for (Object key: mapValue.getKeys()) {
            Object valueObject = mapValue.get(key.toString());
            if (valueObject != null && valueObject.getClass() == MapValueImpl.class) {
                MapValue subMapValue = mapValue.getMapValue(key.toString());
                Map<String, Object> subMap = convertMapValueToMap(subMapValue);
                map.put(key.toString(), subMap);
            } else if (valueObject != null && valueObject.getClass() == ArrayValueImpl.class) {
                ArrayValue arrayValue = mapValue.getArrayValue(key.toString());
                Object[] array = new Object[arrayValue.size()];
                for (int i = 0; i < arrayValue.size(); i++) {
                    if (arrayValue.get(i).getClass() == MapValueImpl.class) {
                        array[i] = convertMapValueToMap((MapValue) arrayValue.get(i));
                    } else {
                        array[i] = arrayValue.get(i);
                    }
                }
                map.put(key.toString(), array);
            } else {
                map.put(key.toString(), valueObject);
            }
        }
        return map;
    }

    /**
     * Used to get the keystore path
     */
    public static String getKeyStorePath(String fullPath) {
        String homePathConst = "\\$\\{mgw-runtime.home}";
        String homePath = System.getProperty(Constants.RUNTIME_HOME_PATH);
        return fullPath.replaceAll(homePathConst, homePath);
    }
}
