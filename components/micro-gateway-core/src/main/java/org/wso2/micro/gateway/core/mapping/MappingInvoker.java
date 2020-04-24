/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.micro.gateway.core.mapping;

import org.ballerinalang.jvm.values.ArrayValue;
import org.ballerinalang.jvm.values.MapValue;
import org.ballerinalang.jvm.values.MapValueImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.micro.gateway.jwttransformer.JWTValueTransformer;

import java.util.HashMap;
import java.util.Map;

/**
 * This class Class to dynamically invoke the transformer.
 */
public class MappingInvoker {
    private static Map map;
    private static int index = 0;
    private static Map editedClaims;
    private static MapValue<String, Object> mapValue;
    private static final Logger log = LoggerFactory.getLogger("ballerina");
    private static JWTValueTransformer jwtValueTransformer;
    public static void initiateJwtMap() {
        map = new HashMap<String, JWTValueTransformer>();
    }

    public static boolean loadMappingClass(String className) {
        try {
            Class mappingClass = MappingInvoker.class.getClassLoader().loadClass(className);
            jwtValueTransformer = (JWTValueTransformer) mappingClass.newInstance();
            map.put(className, jwtValueTransformer);
            return true;
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            log.error("Error while loading the jwttransformer class: " + className, e);
        }
        return false;
    }

    /**
     * Used to add claims to the claim set
     */
    public static MapValue transformJWTValue(MapValue claims, String className) throws Exception {
        jwtValueTransformer = (JWTValueTransformer) map.get(className);
        editedClaims = convertMapValueToMap(claims);
        Map<String, Object> claimSet = jwtValueTransformer.transformJWT(editedClaims);
        mapValue = new MapValueImpl();
        claimSet.forEach((key, value) -> mapValue.put(key, value));
        return mapValue;
    }

    /**
     * Convert MapValue to Map
     */
    public static Map<String, Object> convertMapValueToMap(MapValue mapValue) throws Exception {
        Map<String, Object> map = new HashMap<>();
        for (Object key: mapValue.getKeys()) {
            Object valueObject = mapValue.get(key.toString());
            if (valueObject != null && valueObject instanceof MapValue) {
                MapValue subMapValue = mapValue.getMapValue(key.toString());
                Map<String, Object> subMap = convertMapValueToMap(subMapValue);
                map.put(key.toString(), subMap);
            } else if (valueObject != null && valueObject instanceof ArrayValue) {
                ArrayValue arrayValue = mapValue.getArrayValue(key.toString());
                Object[] array = new Object[arrayValue.size()];
                for (int i = 0; i < arrayValue.size(); i++) {
                    if (arrayValue.get(i) instanceof MapValue) {
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
}
