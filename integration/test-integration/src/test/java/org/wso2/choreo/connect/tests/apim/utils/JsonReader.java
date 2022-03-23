/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.tests.apim.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.wso2.am.integration.clients.admin.api.dto.AdvancedThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.ApplicationThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.SubscriptionThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.ThrottlePolicyDTO;
import org.wso2.choreo.connect.tests.apim.dto.Application;
import org.wso2.choreo.connect.tests.apim.dto.Subscription;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class JsonReader {
    private static final String APIM_ARTIFACTS_FOLDER = File.separator + "apim" + File.separator;
    private static final String ADMIN_FOLDER = File.separator + "admin";
    private static final String THROTTLE_FOLDER = File.separator + "throttle";
    private static final String APPLICATIONS_FILE = File.separator + "apps" + File.separator + "applications.json";
    private static final String SUBSCRIPTION_FILE = File.separator + "subscriptions" + File.separator + "subscriptions.json";


    private static final Type TYPE_APPLICATION = new TypeToken<List<Application>>() {}.getType();
    private static final Type TYPE_SUBSCRIPTION = new TypeToken<List<Subscription>>() {}.getType();
    private static final Type TYPE_ADVANCED_THROTTLE_POLICY_DTO = new TypeToken<AdvancedThrottlePolicyDTO>() {}.getType();
    private static final Type TYPE_APPLICATION_THROTTLE_POLICY_DTO = new TypeToken<ApplicationThrottlePolicyDTO>() {}.getType();
    private static final Type TYPE_SUBSCRIPTION_THROTTLE_POLICY_DTO = new TypeToken<SubscriptionThrottlePolicyDTO>() {}.getType();

    public static Map<String, String> readApiToOpenAPIMap(String apimArtifactsIndex) throws CCTestException {
        Path mapLocation = Paths.get(Utils.getTargetDirPath() + TestConstant.TEST_RESOURCES_PATH + File.separator
                + "apim" + File.separator + apimArtifactsIndex + File.separator + "apiToOpenAPI.json");
        if (Files.exists(mapLocation)) {
            try {
                String apiToOpenAPIString = Files.readString(mapLocation);
                return new ObjectMapper().readValue(apiToOpenAPIString, new TypeReference<>() {});
            } catch (IOException e) {
                throw new CCTestException("Error while reading apiToOpenAPI.json", e);
            }
        }
        return new HashMap<>();
    }

    public static Map<String, String> readApiToAsyncAPIMap(String apimArtifactsIndex) throws CCTestException {
        Path mapLocation = Paths.get(Utils.getTargetDirPath() + TestConstant.TEST_RESOURCES_PATH + File.separator
                + "apim" + File.separator + apimArtifactsIndex + File.separator + "apiToAsyncAPI.json");
        if (Files.exists(mapLocation)) {
            try {
                String apiToAsyncAPIString = Files.readString(mapLocation);
                return new ObjectMapper().readValue(apiToAsyncAPIString, new TypeReference<>() {});
            } catch (IOException e) {
                throw new CCTestException("Error while reading apiToAsyncAPI.json", e);
            }
        }
        return new HashMap<>();
    }

    public static Map<String, ArrayList<String>> readApiToVhostMap(String apimArtifactsIndex) throws CCTestException {
        Path mapLocation = Paths.get(Utils.getTargetDirPath() + TestConstant.TEST_RESOURCES_PATH + File.separator
                + "apim" + File.separator + apimArtifactsIndex + File.separator + "apiToVhosts.json");
        if (Files.exists(mapLocation)) {
            try {
                String apiToVhostString = Files.readString(mapLocation);
                return new ObjectMapper().readValue(apiToVhostString, new TypeReference<>() {});
            } catch (IOException e) {
                throw new CCTestException("Error while reading apiToVhosts.json", e);
            }
        }
        return new HashMap<>();
    }

    public static List<Application> readApplicationsFromJsonFile(String apimArtifactsIndex) throws CCTestException {
        String filename = Utils.getTargetDirPath()
                + TestConstant.TEST_RESOURCES_PATH + APIM_ARTIFACTS_FOLDER + apimArtifactsIndex + APPLICATIONS_FILE;
        try {
            String text = Files.readString(Paths.get(filename));
            Gson gson = new Gson();
            return gson.fromJson(text, TYPE_APPLICATION);
        } catch (IOException e) {
            throw new CCTestException("Error occurred while reading json file " + filename, e);
        }
    }

    public static List<Subscription> readSubscriptionsFromJsonFile(String apimArtifactsIndex) throws CCTestException {
        String filename = Utils.getTargetDirPath()
                + TestConstant.TEST_RESOURCES_PATH + APIM_ARTIFACTS_FOLDER + apimArtifactsIndex + SUBSCRIPTION_FILE;
        try {
            String text = Files.readString(Paths.get(filename));
            Gson gson = new Gson();
            return gson.fromJson(text, TYPE_SUBSCRIPTION);
        } catch (IOException e) {
            throw new CCTestException("Error occurred while reading json file " + filename, e);
        }
    }

    public static Map<String, ThrottlePolicyDTO> readThrottlePoliciesFromJsonFiles(
            String throttleType, String apimArtifactsIndex) throws CCTestException {
        Map<String, ThrottlePolicyDTO> throttlePoliciesList = new HashMap<>();
        Path throttlePolicyLocation = Paths.get(Utils.getTargetDirPath() + TestConstant.TEST_RESOURCES_PATH +
                APIM_ARTIFACTS_FOLDER + apimArtifactsIndex + ADMIN_FOLDER + THROTTLE_FOLDER +
                File.separator + throttleType);
        try (Stream<Path> paths = Files.walk(throttlePolicyLocation)) {
            for (Iterator<Path> apiFiles = paths.filter(Files::isRegularFile).iterator(); apiFiles.hasNext();) {
                Path apiFilePath = apiFiles.next();
                String apiFileContent = Files.readString(apiFilePath);

                if (TestConstant.THROTTLING.ADVANCED.equals(throttleType)) {
                    AdvancedThrottlePolicyDTO apiPolicyDto = new Gson().fromJson(apiFileContent,
                            TYPE_ADVANCED_THROTTLE_POLICY_DTO);
                    throttlePoliciesList.put(apiPolicyDto.getPolicyName(), apiPolicyDto);
                } else if (TestConstant.THROTTLING.APPLICATION.equals(throttleType)) {
                    ApplicationThrottlePolicyDTO apiPolicyDto = new Gson().fromJson(apiFileContent,
                            TYPE_APPLICATION_THROTTLE_POLICY_DTO);
                    throttlePoliciesList.put(apiPolicyDto.getPolicyName(), apiPolicyDto);
                } else if (TestConstant.THROTTLING.SUBSCRIPTION.equals(throttleType)) {
                    SubscriptionThrottlePolicyDTO apiPolicyDto = new Gson().fromJson(apiFileContent,
                            TYPE_SUBSCRIPTION_THROTTLE_POLICY_DTO);
                    throttlePoliciesList.put(apiPolicyDto.getPolicyName(), apiPolicyDto);
                } else {
                    throw new CCTestException("Throttling policy conversion from JSON to Java Object not handled for " +
                            throttleType);
                }
            }
        } catch (IOException e) {
            throw new CCTestException("Error while reading json for throttling policies of type " +
                    throttleType, e);
        }
        return throttlePoliciesList;
    }

    public static boolean isThrottlePolicyFolderExists(String throttleType, String apimArtifactsIndex) {
        Path apiThrottlePolicyLocation = Paths.get(Utils.getTargetDirPath() + TestConstant.TEST_RESOURCES_PATH +
                APIM_ARTIFACTS_FOLDER + apimArtifactsIndex + ADMIN_FOLDER + THROTTLE_FOLDER + File.separator + throttleType);
        return Files.exists(apiThrottlePolicyLocation);
    }

}
