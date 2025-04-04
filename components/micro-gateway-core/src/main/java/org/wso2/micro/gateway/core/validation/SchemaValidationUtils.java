/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.micro.gateway.core.validation;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.ballerinalang.jvm.values.ArrayValue;
import org.ballerinalang.jvm.values.MapValue;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Utility class for schema validation.
 */
public class SchemaValidationUtils {

    /**
     * Method to get a Collection map or an empty method
     *
     * @param map  Map of String Collection
     * @param name name of the key
     * @return Collection of Strings
     */
    public static Collection<String> getFromMapOrEmptyList(Map<String, Collection<String>> map, String name) {
        if (name != null && map.containsKey(name)) {
            return map.get(name).stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Method to convert MapValue<String, String> to Multimap<String, String>
     *
     * @param headersMapValue Ballerina headers MapValue
     * @return Java Multimap of headers
     */
    public static Multimap<String, String> convertToMultimap(MapValue<String, String> headersMapValue) {
        Multimap<String, String> headersMultimap = ArrayListMultimap.create();
        for (Map.Entry<String, String> entry : headersMapValue.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            String headerKey = key.equalsIgnoreCase("content-type") ? "Content-Type" : key.toLowerCase(Locale.ROOT);
            headersMultimap.put(headerKey, value);
        }
        return headersMultimap;
    }

    /**
     * Method to convert MapValue<String, ArrayValue> to Map<String, Collection<String>>
     *
     * @param mapValue Ballerina MapValue
     * @return Java Map of Strings with Collection of Strings
     */
    public static Map<String, Collection<String>> convertMapofArrayValuesToMap(MapValue<String, ArrayValue> mapValue) {
        return mapValue.entrySet().stream().collect(
                Collectors.toMap(Map.Entry::getKey, e -> {
                    ArrayValue arrayValue = e.getValue();
                    Collection<String> collection = new java.util.ArrayList<>();
                    for (int i = 0; i < arrayValue.size(); i++) {
                        collection.add(arrayValue.get(i).toString());
                    }
                    return collection;
                }));
    }
}
