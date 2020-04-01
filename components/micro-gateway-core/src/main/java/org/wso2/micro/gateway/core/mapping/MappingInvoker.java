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

import org.ballerinalang.jvm.values.MapValue;
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

    public static MapValue transformJWTValue(MapValue claims, String className) {
        jwtValueTransformer = (JWTValueTransformer) map.get(className);
        MapValue claimSet = jwtValueTransformer.transformJWT(claims);
        return claimSet;
    }
}
