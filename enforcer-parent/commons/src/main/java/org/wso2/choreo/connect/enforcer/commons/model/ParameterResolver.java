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
package org.wso2.choreo.connect.enforcer.commons.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ParameterResolver {

    private static final Pattern PARAMETER_PATTERN = Pattern.compile("(\\{[a-zA-Z0-9]+[a-z-_A-Z0-9]*\\})");
    private static final Logger logger = LogManager.getLogger(ParameterResolver.class);
    private final List<String> parameterNames = new ArrayList<>();
    private final Pattern pattern;
    private String pathTemplate;

    public ParameterResolver(final String parameterTemplate) {
        // This formatting is required since /foo and /foo/ are considered to be equal
        this.pathTemplate = parameterTemplate;
        String formattedPathParamTemplate = parameterTemplate.endsWith("/") ?
                parameterTemplate.substring(0, parameterTemplate.length() - 1) : parameterTemplate;
        final Matcher matcher = PARAMETER_PATTERN.matcher(formattedPathParamTemplate);

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
        String regex = Pattern.quote(matcher.replaceAll("_____PARAM_____"))
                .replace("_____PARAM_____", "\\E([^/]*)\\Q");
        regex = regex.endsWith("*\\E") ? regex.substring(0, regex.length() - 4) + "\\E($|([/]{1}(.*)))" : regex;
        pattern = Pattern.compile(regex);
    }

    public Map<String, String> parametersByName(final String uriString) throws IllegalArgumentException {
        // This formatting is required since /foo and /foo/ are considered to be equal
        String formattedURI = uriString.endsWith("/") ?
                uriString.substring(0, uriString.length() - 1) : uriString;
        final Matcher matcher = pattern.matcher(formattedURI);
        if (!matcher.matches()) {
            // Unlikely to occur as this pair is already matched within router.
            logger.debug("PathTemplate: {}  and RawPath: {} is mismatched.", pathTemplate, uriString);
            return new HashMap<>();
        }
        final Map<String, String> map = new HashMap<>();
        for (int i = 1; i <= matcher.groupCount(); i++) {
            // There can be multiple match for trailing wildcard (if available.)
            // Those matches will appear in the end. Hence those can be discarded.
            // ex: /pet/{id}/*
            if (i == parameterNames.size() + 1) {
                break;
            }
            map.put(parameterNames.get(i - 1), matcher.group(i));
        }
        return map;
    }
//
//    private String preProcessPath(String path)
}
