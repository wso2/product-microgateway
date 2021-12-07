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
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.choreo.connect.tests.apim.dto.Api;
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
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ApimResourceProcessor {
    private static final Logger log = LoggerFactory.getLogger(ApimResourceProcessor.class);
    private static final String BEFORE_CC_STARTUP_FOLDER = File.separator + "apimApisAppsSubs" + File.separator;
    private static final String APIS_FILE = "apis.json";
    private static final String APPLICATIONS_FILE = "applications.json";
    private static final String SUBSCRIPTION_FILE = "subscriptions.json";

    private static final Type TYPE_API_REQUEST = new TypeToken<List<Api>>() {}.getType();
    private static final Type TYPE_APPLICATION = new TypeToken<List<Application>>() {}.getType();
    private static final Type TYPE_SUBSCRIPTION = new TypeToken<List<Subscription>>() {}.getType();

    public static final Map<String, String> apiNameToId = new HashMap<>();
    public static final Map<String, String> applicationNameToId = new HashMap<>();

    public void createApisAppsSubs(String apiProvider, RestAPIPublisherImpl publisherRestClient,
                                   RestAPIStoreImpl storeRestClient) throws CCTestException, MalformedURLException {
        createApisAndUpdateMap(apiProvider, publisherRestClient);
        createAppsAndUpdateMap(storeRestClient);
        createSubscriptions(storeRestClient);
    }

    private void createApisAndUpdateMap(String apiProvider, RestAPIPublisherImpl publisherRestClient) throws CCTestException, MalformedURLException {
        List<Api> apis = readApisFromJsonFile(Utils.getTargetDirPath()
                + TestConstant.TEST_RESOURCES_PATH + BEFORE_CC_STARTUP_FOLDER + APIS_FILE);
        for (Api api : apis) {
            String apiId = PublisherUtils.createAPI(api, apiProvider, publisherRestClient);
            if (Objects.isNull(api.getVhosts())) {
                PublisherUtils.deployAndPublishAPI(apiId, api.getName(), "localhost", publisherRestClient);
            } else {
                for (String vhost: api.getVhosts()) {
                    PublisherUtils.deployAndPublishAPI(apiId, api.getName(), vhost, publisherRestClient);
                }
            }
            apiNameToId.put(api.getName(), apiId);
        }
    }

    private void createAppsAndUpdateMap(RestAPIStoreImpl storeRestClient) throws CCTestException {
        List<Application> applications = readApplicationsFromJsonFile(Utils.getTargetDirPath()
                + TestConstant.TEST_RESOURCES_PATH + BEFORE_CC_STARTUP_FOLDER + APPLICATIONS_FILE);
        for (Application application : applications) {
            String appId = StoreUtils.createApplication(application, storeRestClient);
            applicationNameToId.put(application.getName(), appId);
        }
    }

    private void createSubscriptions(RestAPIStoreImpl storeRestClient) throws CCTestException {
        List<Subscription> subscriptions = readSubscriptionsFromJsonFile(Utils.getTargetDirPath()
                + TestConstant.TEST_RESOURCES_PATH + BEFORE_CC_STARTUP_FOLDER + SUBSCRIPTION_FILE);
        for (Subscription subscription : subscriptions) {
            String apiId = apiNameToId.get(subscription.getApiName());
            String applicationId = applicationNameToId.get(subscription.getAppName());
            StoreUtils.subscribeToAPI(apiId, applicationId, subscription.getTier(), storeRestClient);
            log.info("Created Subscription for API:" + subscription.getApiName() + " App:" + subscription.getAppName());
        }
    }

    private List<Api> readApisFromJsonFile(String filename) throws CCTestException {
        try {
            String text = Files.readString(Paths.get(filename));
            Gson gson = new Gson();
            return gson.fromJson(text, TYPE_API_REQUEST);
        } catch (IOException e) {
            throw new CCTestException("Error occurred while reading json file " + filename, e);
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
}
