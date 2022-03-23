/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.choreo.connect.tests.apim;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.am.integration.clients.admin.api.dto.ThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.AdvancedThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.ApplicationThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.SubscriptionThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.AdvancedThrottlePolicyInfoDTO;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.test.impl.RestAPIAdminImpl;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.choreo.connect.tests.apim.dto.Application;
import org.wso2.choreo.connect.tests.apim.dto.Subscription;
import org.wso2.choreo.connect.tests.apim.utils.AdminUtils;
import org.wso2.choreo.connect.tests.apim.utils.JsonReader;
import org.wso2.choreo.connect.tests.apim.utils.PublisherUtils;
import org.wso2.choreo.connect.tests.apim.utils.StoreUtils;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class ApimResourceProcessor {
    private static final Logger log = LoggerFactory.getLogger(ApimResourceProcessor.class);

    private final String apimArtifactsIndex;
    private final String apiProvider;
    private final RestAPIAdminImpl adminRestClient;
    private final RestAPIPublisherImpl publisherRestClient;
    private final RestAPIStoreImpl storeRestClient;

    Map<String, ArrayList<String>> apiToVhosts;   // API name -> list of vhosts
    Map<String, String> apiToOpenAPI;             // API name -> OpenAPI file name
    Map<String, String> apiToAsyncAPI;            // API name -> AsyncAPI file name

    Map<String, ThrottlePolicyDTO> advancedThrottlePoliciesList; // API policy name -> API policy DTO
    Map<String, ThrottlePolicyDTO> applicationThrottlePoliciesList; // Application policy name -> Application policy DTO
    Map<String, ThrottlePolicyDTO> subscriptionThrottlePoliciesList; // Subscription policy name -> Subscription policy DTO

    private static final String APIM_ARTIFACTS_FOLDER = File.separator + "apim" + File.separator;
    private static final String APIS_FOLDER = File.separator + "apis";

    private static final Type TYPE_API_REQUEST = new TypeToken<APIRequest>() {}.getType();

    public static final Map<String, String> apiNameToId = new HashMap<>();
    public static final Map<String, String> applicationNameToId = new HashMap<>();

    public ApimResourceProcessor(String apimArtifactsIndex, String apiProvider,
                                 RestAPIAdminImpl adminRestClient, RestAPIPublisherImpl publisherRestClient,
                                 RestAPIStoreImpl storeRestClient) {
        this.apimArtifactsIndex = apimArtifactsIndex;
        this.apiProvider = apiProvider;
        this.adminRestClient = adminRestClient;
        this.publisherRestClient = publisherRestClient;
        this.storeRestClient = storeRestClient;
    }

    public void populateApiManager() throws CCTestException {
        createAdminThrottlePolicies();
        createApisAndUpdateMap();
        createAppsAndUpdateMap();
        createSubscriptions();
    }

    private void createAdminThrottlePolicies() throws CCTestException {
        // Recreate advanced throttle policies
        if (JsonReader.isThrottlePolicyFolderExists(TestConstant.THROTTLING.ADVANCED, apimArtifactsIndex)) {
            advancedThrottlePoliciesList = JsonReader.readThrottlePoliciesFromJsonFiles(
                    TestConstant.THROTTLING.ADVANCED, apimArtifactsIndex);
            removeNewlyAddedAdvancedThrottlingPolicies();
            createAdvancedThrottlingPolicies();
        } else {
            log.info("No advanced throttle policies for the apimArtifactsIndex {}", apimArtifactsIndex);
        }

        // Recreate application throttle policies
        if (JsonReader.isThrottlePolicyFolderExists(TestConstant.THROTTLING.APPLICATION, apimArtifactsIndex)) {
            applicationThrottlePoliciesList = JsonReader.readThrottlePoliciesFromJsonFiles(
                    TestConstant.THROTTLING.APPLICATION, apimArtifactsIndex);
            removeNewlyAddedApplicationThrottlingPolicies();
            createApplicationThrottlingPolicies();
        } else {
            log.info("No application throttle policies for the apimArtifactsIndex {}", apimArtifactsIndex);
        }

        // Recreate subscription throttle policies
        if (JsonReader.isThrottlePolicyFolderExists(TestConstant.THROTTLING.SUBSCRIPTION, apimArtifactsIndex)) {
            subscriptionThrottlePoliciesList = JsonReader.readThrottlePoliciesFromJsonFiles(
                    TestConstant.THROTTLING.SUBSCRIPTION, apimArtifactsIndex);
            removeNewlyAddedSubscriptionThrottlingPolicies();
            createSubscriptionThrottlingPolicies();
        } else {
            log.info("No subscription throttle policies for the apimArtifactsIndex {}", apimArtifactsIndex);
        }
    }

    private void createApisAndUpdateMap() throws CCTestException {
        apiToOpenAPI = JsonReader.readApiToOpenAPIMap(apimArtifactsIndex);
        apiToAsyncAPI = JsonReader.readApiToAsyncAPIMap(apimArtifactsIndex);
        apiToVhosts = JsonReader.readApiToVhostMap(apimArtifactsIndex);

        Path apisLocation = Paths.get(Utils.getTargetDirPath() + TestConstant.TEST_RESOURCES_PATH +
                APIM_ARTIFACTS_FOLDER + apimArtifactsIndex + APIS_FOLDER);
        try (Stream<Path> paths = Files.walk(apisLocation)) {
            for (Iterator<Path> apiFiles = paths.filter(Files::isRegularFile).iterator(); apiFiles.hasNext();) {
                Path apiFilePath = apiFiles.next();
                String apiFileContent = Files.readString(apiFilePath);

                APIRequest apiRequest = new Gson().fromJson(apiFileContent, TYPE_API_REQUEST);
                apiRequest.setProvider(apiProvider);
                apiRequest.setTags("tags"); // otherwise, throws a NPE

                // Create API
                String apiId = PublisherUtils.createAPI(apiRequest, publisherRestClient);
                if(apiToOpenAPI.containsKey(apiRequest.getName())) {
                    String openAPIFileName = apiToOpenAPI.get(apiRequest.getName());
                    PublisherUtils.updateOpenAPIDefinition(apiId, openAPIFileName, publisherRestClient);
                } else if(apiToAsyncAPI.containsKey(apiRequest.getName())) {
                    String asyncAPIFileName = apiToAsyncAPI.get(apiRequest.getName());
                    PublisherUtils.updateAsyncAPIDefinition(apiId, asyncAPIFileName, publisherRestClient);
                }

                // Deploy API
                ArrayList<String> vHosts = apiToVhosts.get(apiRequest.getName());
                String revisionUUID = PublisherUtils.createAPIRevision(apiId, publisherRestClient);
                if (vHosts == null || vHosts.size() == 0) {
                    PublisherUtils.deployRevision(apiId, revisionUUID,"localhost", publisherRestClient);
                } else {
                    for (String vHost : vHosts) {
                        PublisherUtils.deployRevision(apiId, revisionUUID, vHost, publisherRestClient);
                    }
                }

                // Publish API
                PublisherUtils.publishAPI(apiId, apiRequest.getName(), publisherRestClient);
                apiNameToId.put(apiRequest.getName(), apiId);
            }
        } catch (IOException | ApiException e) {
            log.error("Error while creating, deploying or publishing API", e);
            throw new CCTestException("Error while creating, deploying or publishing APIs", e);
        } catch (CCTestException e) {
            log.error("Error while creating, deploying or publishing APIs", e);
            throw e;
        }
    }

    private void createAppsAndUpdateMap() throws CCTestException {
        List<Application> applications = JsonReader.readApplicationsFromJsonFile(apimArtifactsIndex);
        for (Application application : applications) {
            String appId = StoreUtils.createApplication(application, storeRestClient);
            applicationNameToId.put(application.getName(), appId);
        }
    }

    private void createSubscriptions() throws CCTestException {
        List<Subscription> subscriptions = JsonReader.readSubscriptionsFromJsonFile(apimArtifactsIndex);
        for (Subscription subscription : subscriptions) {
            String apiId = apiNameToId.get(subscription.getApiName());
            String applicationId = applicationNameToId.get(subscription.getAppName());
            StoreUtils.subscribeToAPI(apiId, applicationId, subscription.getTier(), storeRestClient);
            log.info("Created Subscription for API:" + subscription.getApiName() + " App:" + subscription.getAppName());
        }
    }

    // --- Sub Methods for Throttle Policy Creation and Deletion

    private void createAdvancedThrottlingPolicies() {
        advancedThrottlePoliciesList.forEach((key, value) -> {
            try {
                adminRestClient.addAdvancedThrottlingPolicy((AdvancedThrottlePolicyDTO) value);
            } catch (org.wso2.am.integration.clients.admin.ApiException e) {
                log.error("Error while creating AdvancedThrottlingPolicy. Name: {}, Response: {}",
                        key, e.getResponseBody());
            }
            log.info("Created Advanced Throttling policy {}", key);
        });
    }

    private void createApplicationThrottlingPolicies() {
        applicationThrottlePoliciesList.forEach((key, value) -> {
            try {
                adminRestClient.addApplicationThrottlingPolicy((ApplicationThrottlePolicyDTO) value);
            } catch (org.wso2.am.integration.clients.admin.ApiException e) {
                log.error("Error while creating ApplicationThrottlingPolicy. Name: {}, Response: {}",
                        key, e.getResponseBody());
            }
            log.info("Created Application Throttling Policy {}", key);
        });
    }

    private void createSubscriptionThrottlingPolicies() {
        subscriptionThrottlePoliciesList.forEach((key, value) -> {
            try {
                adminRestClient.addSubscriptionThrottlingPolicy((SubscriptionThrottlePolicyDTO) value);
            } catch (org.wso2.am.integration.clients.admin.ApiException e) {
                log.error("Error while creating SubscriptionThrottlingPolicy. Name: {}, Response: {}",
                        key, e.getResponseBody());
            }
            log.info("Created Subscription Throttling Policy {}", key);
        });
    }

    private void removeNewlyAddedAdvancedThrottlingPolicies() throws CCTestException {
        List<AdvancedThrottlePolicyInfoDTO> advancedThrottlingPolicies =
                AdminUtils.getAllAdvancedThrottlingPolicies(adminRestClient);
        for (AdvancedThrottlePolicyInfoDTO existingPolicy: advancedThrottlingPolicies) {
            // Check if the policy is not a policy that comes with the pack by default (i.e. a new policy)
            if (advancedThrottlePoliciesList.containsKey(existingPolicy.getPolicyName())) {
                try {
                    adminRestClient.deleteAdvancedThrottlingPolicy(existingPolicy.getPolicyId());
                    log.info("Deleted Advanced Throttling Policy: {}", existingPolicy.getPolicyName());
                } catch (org.wso2.am.integration.clients.admin.ApiException e) {
                    throw new CCTestException(
                            "Error while deleting AdvancedThrottlingPolicy. Name: " + existingPolicy.getPolicyName(), e);
                }
            }
        }
    }

    private void removeNewlyAddedApplicationThrottlingPolicies() throws CCTestException {
        List<ApplicationThrottlePolicyDTO> applicationThrottlingPolicies =
                AdminUtils.getAllApplicationThrottlingPolicies(adminRestClient);
        for (ApplicationThrottlePolicyDTO existingPolicy: applicationThrottlingPolicies) {
            // Check if the policy is not a policy that comes with the pack by default (i.e. a new policy)
            if (applicationThrottlePoliciesList.containsKey(existingPolicy.getPolicyName())) {
                try {
                    adminRestClient.deleteApplicationThrottlingPolicy(existingPolicy.getPolicyId());
                    log.info("Deleted Application Throttling Policy: {}", existingPolicy.getPolicyName());
                } catch (org.wso2.am.integration.clients.admin.ApiException e) {
                    throw new CCTestException(
                            "Error while deleting ApplicationThrottlingPolicy. Name: " + existingPolicy.getPolicyName(), e);
                }
            }
        }
    }

    private void removeNewlyAddedSubscriptionThrottlingPolicies() throws CCTestException {
        List<SubscriptionThrottlePolicyDTO> subscriptionThrottlingPolicies =
                AdminUtils.getAllSubscriptionThrottlingPolicies(adminRestClient);
        for (SubscriptionThrottlePolicyDTO existingPolicy: subscriptionThrottlingPolicies) {
            // Check if the policy is not a policy that comes with the pack by default (i.e. a new policy)
            if (subscriptionThrottlePoliciesList.containsKey(existingPolicy.getPolicyName())) {
                try {
                    adminRestClient.deleteSubscriptionThrottlingPolicy(existingPolicy.getPolicyId());
                    log.info("Deleted Subscription Throttling Policy: {}", existingPolicy.getPolicyName());
                } catch (org.wso2.am.integration.clients.admin.ApiException e) {
                    throw new CCTestException(
                            "Error while deleting SubscriptionThrottlingPolicy. Name: " + existingPolicy.getPolicyName(), e);
                }
            }
        }
    }
}
