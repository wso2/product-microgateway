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
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.choreo.connect.tests.apim.dto.Application;
import org.wso2.choreo.connect.tests.apim.dto.Subscription;
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
import java.util.*;
import java.util.stream.Stream;

public class ApimResourceProcessor {
    private static final Logger log = LoggerFactory.getLogger(ApimResourceProcessor.class);
    String apimArtifactsIndex;
    String apiProvider;
    RestAPIPublisherImpl publisherRestClient;
    RestAPIStoreImpl storeRestClient;
    Map<String, ArrayList<String>> apiToVhosts;

    private static final String APIM_ARTIFACTS_FOLDER = File.separator + "apim" + File.separator;
    private static final String APIS_FOLDER = File.separator + "apis";
    private static final String APPLICATIONS_FILE = File.separator + "apps" + File.separator + "applications.json";
    private static final String SUBSCRIPTION_FILE = File.separator + "subscriptions" + File.separator + "subscriptions.json";

    private static final Type TYPE_API_REQUEST = new TypeToken<APIRequest>() {}.getType();
    private static final Type TYPE_APPLICATION = new TypeToken<List<Application>>() {}.getType();
    private static final Type TYPE_SUBSCRIPTION = new TypeToken<List<Subscription>>() {}.getType();

    public static final Map<String, String> apiNameToId = new HashMap<>();
    public static final Map<String, String> applicationNameToId = new HashMap<>();

    public ApimResourceProcessor(String apimArtifactsIndex, String apiProvider,
                                 RestAPIPublisherImpl publisherRestClient, RestAPIStoreImpl storeRestClient) {
        this.apimArtifactsIndex = apimArtifactsIndex;
        this.apiProvider = apiProvider;
        this.publisherRestClient = publisherRestClient;
        this.storeRestClient = storeRestClient;
    }

    public void createApisAppsSubs() throws CCTestException {
        createApisAndUpdateMap();
        createAppsAndUpdateMap();
        createSubscriptions();
    }

    private void createApisAndUpdateMap() throws CCTestException {
        Path apisLocation = Paths.get(Utils.getTargetDirPath() + TestConstant.TEST_RESOURCES_PATH +
                APIM_ARTIFACTS_FOLDER + apimArtifactsIndex + APIS_FOLDER);
        try (Stream<Path> paths = Files.walk(apisLocation)) {
            readApiToVhostMap();
            for (Iterator<Path> apiFiles = paths.filter(Files::isRegularFile).iterator(); apiFiles.hasNext();) {
                Path apiFilePath = apiFiles.next();
                String apiFileContent = Files.readString(apiFilePath);

                APIRequest apiRequest = new Gson().fromJson(apiFileContent, TYPE_API_REQUEST);
                apiRequest.setProvider(apiProvider);
                apiRequest.setTags("tags"); // otherwise, throws a NPE

                String apiId = PublisherUtils.createAPI(apiRequest, publisherRestClient);
                ArrayList<String> vHosts = apiToVhosts.get(apiRequest.getName());

                String revisionUUID = PublisherUtils.createAPIRevision(apiId, publisherRestClient);
                if (vHosts == null || vHosts.size() == 0) {
                    PublisherUtils.deployRevision(apiId, revisionUUID,"localhost", publisherRestClient);
                } else {
                    for (String vHost : vHosts) {
                        PublisherUtils.deployRevision(apiId, revisionUUID, vHost, publisherRestClient);
                    }
                }
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
        List<Application> applications = readApplicationsFromJsonFile(Utils.getTargetDirPath()
                + TestConstant.TEST_RESOURCES_PATH + APIM_ARTIFACTS_FOLDER + apimArtifactsIndex + APPLICATIONS_FILE);
        for (Application application : applications) {
            String appId = StoreUtils.createApplication(application, storeRestClient);
            applicationNameToId.put(application.getName(), appId);
        }
    }

    private void createSubscriptions() throws CCTestException {
        List<Subscription> subscriptions = readSubscriptionsFromJsonFile(Utils.getTargetDirPath()
                + TestConstant.TEST_RESOURCES_PATH + APIM_ARTIFACTS_FOLDER + apimArtifactsIndex + SUBSCRIPTION_FILE);
        for (Subscription subscription : subscriptions) {
            String apiId = apiNameToId.get(subscription.getApiName());
            String applicationId = applicationNameToId.get(subscription.getAppName());
            StoreUtils.subscribeToAPI(apiId, applicationId, subscription.getTier(), storeRestClient);
            log.info("Created Subscription for API:" + subscription.getApiName() + " App:" + subscription.getAppName());
        }
    }

    private List<Application> readApplicationsFromJsonFile(String filename) throws CCTestException {
        try {
            String text = Files.readString(Paths.get(filename));
            Gson gson = new Gson();
            return gson.fromJson(text, TYPE_APPLICATION);
        } catch (IOException e) {
            throw new CCTestException("Error occurred while reading json file " + filename, e);
        }
    }

    private List<Subscription> readSubscriptionsFromJsonFile(String filename) throws CCTestException {
        try {
            String text = Files.readString(Paths.get(filename));
            Gson gson = new Gson();
            return gson.fromJson(text, TYPE_SUBSCRIPTION);
        } catch (IOException e) {
            throw new CCTestException("Error occurred while reading json file " + filename, e);
        }
    }

    private void readApiToVhostMap() throws IOException {
        Path mapLocation = Paths.get(Utils.getTargetDirPath() + TestConstant.TEST_RESOURCES_PATH + File.separator
                + "apim" + File.separator + apimArtifactsIndex + File.separator + "apiToVhosts.json");
        String apiToVhostString = Files.readString(mapLocation);
        apiToVhosts = new ObjectMapper().readValue(apiToVhostString, new TypeReference<>() {});
    }
}
