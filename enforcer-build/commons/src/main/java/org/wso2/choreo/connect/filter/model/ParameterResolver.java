/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org).
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.choreo.connect.filter.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ParameterResolver {

    private static final Pattern PARAMETER_PATTERN = Pattern.compile("(\\{[a-zA-Z]+\\})");
    private final List<String> parameterNames = new ArrayList<>();
    private final Pattern pattern;

    public ParameterResolver(final String parameterTemplate) {

        final Matcher matcher = PARAMETER_PATTERN.matcher(parameterTemplate);

        while (matcher.find()) {
            if (matcher.groupCount() == 1) {
                final String group = matcher.group(1);
                if (group.length() > 2) {
                    parameterNames.add(group.substring(1, group.length() - 1));
                } else {
                    parameterNames.add(group);
                }
            }
        }
        pattern = Pattern.compile(Pattern.quote(matcher.replaceAll("_____PARAM_____"))
                .replace("_____PARAM_____", "\\E([^/]*)\\Q"));
    }

    public Map<String, String> parametersByName(final String uriString) {
        final Matcher matcher = pattern.matcher(uriString);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Uri not matches!");
        }
        final Map<String, String> map = new HashMap<>();
        for (int i = 1; i <= matcher.groupCount(); i++) {
            map.put(parameterNames.get(i - 1), matcher.group(i));
        }
        return map;
    }
}
