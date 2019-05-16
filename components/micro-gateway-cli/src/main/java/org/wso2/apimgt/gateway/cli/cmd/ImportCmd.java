/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.apimgt.gateway.cli.cmd;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.apache.commons.lang3.StringUtils;
import org.ballerinalang.packerina.init.InitHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.cli.codegen.CodeGenerator;
import org.wso2.apimgt.gateway.cli.codegen.ThrottlePolicyGenerator;
import org.wso2.apimgt.gateway.cli.config.TOMLConfigParser;
import org.wso2.apimgt.gateway.cli.constants.GatewayCliConstants;
import org.wso2.apimgt.gateway.cli.constants.RESTServiceConstants;
import org.wso2.apimgt.gateway.cli.exception.CLIInternalException;
import org.wso2.apimgt.gateway.cli.exception.CLIRuntimeException;
import org.wso2.apimgt.gateway.cli.exception.CliLauncherException;
import org.wso2.apimgt.gateway.cli.exception.ConfigParserException;
import org.wso2.apimgt.gateway.cli.model.config.Client;
import org.wso2.apimgt.gateway.cli.model.config.Config;
import org.wso2.apimgt.gateway.cli.model.config.ContainerConfig;
import org.wso2.apimgt.gateway.cli.model.config.Etcd;
import org.wso2.apimgt.gateway.cli.model.config.Token;
import org.wso2.apimgt.gateway.cli.model.config.TokenBuilder;
import org.wso2.apimgt.gateway.cli.model.rest.ext.ExtendedAPI;
import org.wso2.apimgt.gateway.cli.model.rest.policy.ApplicationThrottlePolicyDTO;
import org.wso2.apimgt.gateway.cli.model.rest.policy.SubscriptionThrottlePolicyDTO;
import org.wso2.apimgt.gateway.cli.oauth.OAuthService;
import org.wso2.apimgt.gateway.cli.oauth.OAuthServiceImpl;
import org.wso2.apimgt.gateway.cli.rest.RESTAPIService;
import org.wso2.apimgt.gateway.cli.rest.RESTAPIServiceImpl;
import org.wso2.apimgt.gateway.cli.utils.GatewayCmdUtils;
import org.wso2.apimgt.gateway.cli.utils.ToolkitLibExtractionUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * This class represents the "import" command and it pull the swagger and generate the source code
 */
@Parameters(commandNames = "import", commandDescription = "pull the api from publisher")
public class ImportCmd implements GatewayLauncherCmd {
    private static final Logger logger = LoggerFactory.getLogger(ImportCmd.class);
    private static PrintStream outStream = System.out;

    @SuppressWarnings("unused")
    @Parameter(hidden = true, required = true)
    private List<String> mainArgs;

    @SuppressWarnings("unused")
    @Parameter(names = "--java.debug", hidden = true)
    private String javaDebugPort;

    @Parameter(names = {"-u", "--username"}, hidden = true)
    private String username;

    @Parameter(names = {"-p", "--password"}, hidden = true)
    private String password;

    @SuppressWarnings("unused")
    @Parameter(names = {"-l", "--label"}, hidden = true)
    private String label;

    @Parameter(names = {"-s", "--server-url"}, hidden = true)
    private String baseURL;

    @Parameter(names = {"-t", "--truststore"}, hidden = true)
    private String trustStoreLocation;

    @Parameter(names = {"-w", "--truststore-pass"}, hidden = true)
    private String trustStorePassword;

    @Parameter(names = {"-c", "--config"}, hidden = true)
    private String toolkitConfigPath;

    @SuppressWarnings("unused")
    @Parameter(names = {"-d", "--deployment-config"}, hidden = true)
    private String deploymentConfigPath;

    @SuppressWarnings("unused")
    @Parameter(names = {"-a", "--api-name"}, hidden = true)
    private String apiName;

    @SuppressWarnings("unused")
    @Parameter(names = {"-v", "--version"}, hidden = true)
    private String version;

    @SuppressWarnings("unused")
    @Parameter(names = {"-k", "--insecure"}, hidden = true, arity = 0)
    private boolean isInsecure;

    @Parameter(names = {"-b", "--security"}, hidden = true)
    private String security;

    @Parameter(names = {"--help", "-h", "?"}, hidden = true, description = "for more information", help = true)
    private boolean helpFlag;

    private String publisherEndpoint;
    private String adminEndpoint;
    private String registrationEndpoint;
    private String tokenEndpoint;
    private String clientSecret;
    private boolean isOverwriteRequired;

    @Override
    public void execute() {
        if (helpFlag) {
            String commandUsageInfo = getCommandUsageInfo("import");
            outStream.println(commandUsageInfo);
            return;
        }
        String clientID;
        String projectName = GatewayCmdUtils.getSingleArgument(mainArgs);
        File projectLocation = new File(GatewayCmdUtils.getProjectDirectoryPath(projectName));

        if (!projectLocation.exists()) {
            throw GatewayCmdUtils.createUsageException("Project " + projectName + " does not exist. Please execute the command '" +
                    "micro-gw init " + projectName + "' to initialize the project.");
        }
        //extract the ballerina platform and runtime
        ToolkitLibExtractionUtils.extractPlatformAndRuntime();
        //Security Schemas settings
        if (StringUtils.isEmpty(security)) {
            security = "oauth2";
        }
        if (StringUtils.isEmpty(toolkitConfigPath)) {
            toolkitConfigPath = GatewayCmdUtils.getMainConfigLocation();
        }

        init(toolkitConfigPath, deploymentConfigPath, projectName);
        Config config = GatewayCmdUtils.getConfig();
        isOverwriteRequired = false;

        validateAPIGetRequestParams(label, apiName, version);
        //Setup username
        String configuredUser = config.getToken().getUsername();
        if (StringUtils.isEmpty(configuredUser)) {
            if (StringUtils.isEmpty(username)) {
                isOverwriteRequired = true;
                if ((username = GatewayCmdUtils.promptForTextInput(outStream, "Enter Username: "))
                        .trim().isEmpty()) {
                    throw GatewayCmdUtils.createUsageException("Micro gateway init failed: empty username.");
                }
            }
        } else {
            username = configuredUser;
        }

        //Setup password
        if (StringUtils.isEmpty(password)) {
            if ((password = promptForPasswordInput("Enter Password for " +
                    username + ": ")).trim().isEmpty()) {
                if (StringUtils.isEmpty(password)) {
                    password = promptForPasswordInput(
                            "Password can't be empty; enter password for " + username + ": ");
                    if (password.trim().isEmpty()) {
                        throw GatewayCmdUtils.createUsageException("Micro gateway init failed: empty password.");
                    }
                }
            }
        }

        //setup endpoints
        Token configToken = config.getToken();
        TokenBuilder configTokenValues = setEndpoints(configToken);

        //configure trust store
        String configuredTrustStore = config.getToken().getTrustStoreLocation();
        if (StringUtils.isEmpty(configuredTrustStore)) {
            if (StringUtils.isEmpty(trustStoreLocation)) {
                isOverwriteRequired = true;
                if ((trustStoreLocation = GatewayCmdUtils.promptForTextInput(outStream,
                        "Enter Trust store location: [" + RESTServiceConstants.DEFAULT_TRUSTSTORE_PATH + "]")).trim().isEmpty()) {
                    trustStoreLocation = RESTServiceConstants.DEFAULT_TRUSTSTORE_PATH;
                }
            }
        } else {
            trustStoreLocation = configuredTrustStore;
        }

        //configure trust store password
        String encryptedPass = config.getToken().getTrustStorePassword();
        String configuredTrustStorePass;
        if (StringUtils.isEmpty(encryptedPass)) {
            configuredTrustStorePass = null;
        } else {
            try {
                configuredTrustStorePass = GatewayCmdUtils.decrypt(encryptedPass, password);
            } catch (CliLauncherException e) {
                //different password used to encrypt
                configuredTrustStorePass = null;
            }
        }

        if (StringUtils.isEmpty(configuredTrustStorePass)) {
            if (StringUtils.isEmpty(trustStorePassword)) {
                isOverwriteRequired = true;
                if ((trustStorePassword = promptForPasswordInput(
                        "Enter Trust store password: " + "[ use default? ]")).trim()
                        .isEmpty()) {
                    trustStorePassword = RESTServiceConstants.DEFAULT_TRUSTSTORE_PASS;
                }
            }
        } else {
            trustStorePassword = configuredTrustStorePass;
        }

        File trustStoreFile = new File(trustStoreLocation);
        if (!trustStoreFile.isAbsolute()) {
            trustStoreLocation = GatewayCmdUtils.getUnixPath(GatewayCmdUtils.getCLIHome() + File.separator
                    + trustStoreLocation);
        }
        trustStoreFile = new File(trustStoreLocation);
        if (!trustStoreFile.exists()) {
            logger.error("Provided trust store location {} does not exist.", trustStoreLocation);
            throw new CLIRuntimeException("Provided trust store location does not exist.");
        }

        //set the trustStore
        System.setProperty("javax.net.ssl.keyStoreType", "pkcs12");
        System.setProperty("javax.net.ssl.trustStoreType", "pkcs12");
        System.setProperty("javax.net.ssl.trustStore", trustStoreLocation);
        System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);

        OAuthService manager = new OAuthServiceImpl();
        clientID = config.getToken().getClientId();
        String encryptedSecret = config.getToken().getClientSecret();
        if (!StringUtils.isEmpty(clientID.trim()) && !StringUtils.isEmpty(encryptedSecret.trim())) {
            try {
                clientSecret = GatewayCmdUtils.decrypt(encryptedSecret, password);
            } catch (CliLauncherException e) {
                //different password used to encrypt
                clientSecret = null;
            }
        }

        if (StringUtils.isEmpty(clientID) || StringUtils.isEmpty(clientSecret)) {
            String[] clientInfo = manager
                    .generateClientIdAndSecret(registrationEndpoint, username, password.toCharArray(), isInsecure);
            clientID = clientInfo[0];
            clientSecret = clientInfo[1];
        }

        String accessToken = manager
                .generateAccessToken(tokenEndpoint, username, password.toCharArray(), clientID, clientSecret,
                        isInsecure);

        List<ExtendedAPI> apis = new ArrayList<>();
        RESTAPIService service = new RESTAPIServiceImpl(publisherEndpoint, adminEndpoint, isInsecure);
        if (label != null) {
            apis = service.getAPIs(label, accessToken);
        } else {
            ExtendedAPI api = service.getAPI(apiName, version, accessToken);
            if (api != null) {
                apis.add(api);
            }
        }

        //ExtendedAPI's security is updated
        apis.forEach(api -> api.setMgwApiSecurity(security));

        if (apis.isEmpty()) {
            // Delete folder
            GatewayCmdUtils.deleteProject(File.separator + projectName);
            String errorMsg;
            if (label != null) {
                errorMsg = "No APIs found for the given label: " + label;
            } else {
                errorMsg = "No Published APIs matched for name:" + apiName + ", version:" + version;
            }
            throw new CLIRuntimeException(errorMsg);
        }

        List<ApplicationThrottlePolicyDTO> applicationPolicies = service.getApplicationPolicies(accessToken);
        List<SubscriptionThrottlePolicyDTO> subscriptionPolicies = service.getSubscriptionPolicies(accessToken);

        //delete the folder if an exception is thrown in following steps
        try {
            GatewayCmdUtils.saveSwaggerDefinitionForMultipleAPIs(projectName, apis);
            ThrottlePolicyGenerator policyGenerator = new ThrottlePolicyGenerator();
            policyGenerator.generate(GatewayCmdUtils.getProjectGenSrcDirectoryPath(projectName) + File.separator
                    + GatewayCliConstants.POLICY_DIR, applicationPolicies, subscriptionPolicies);
            CodeGenerator codeGenerator = new CodeGenerator();
            codeGenerator.generate(projectName, apis, true);
            //Initializing the ballerina project and creating .bal folder.
            InitHandler.initialize(Paths.get(GatewayCmdUtils.getProjectGenDirectoryPath(projectName)), null,
                    new ArrayList<>(), null);
        } catch (Exception e) {
            throw new CLIInternalException("Exception occurred during codeGeneration process");
        }
        //todo: check if the files has been changed using hash utils

        //if all the operations are success, write new config to file
        if (isOverwriteRequired) {
            Config newConfig = new Config();
            Client client = new Client();
            client.setHttpRequestTimeout(1000000);
            newConfig.setClient(client);

            String encryptedCS = GatewayCmdUtils.encrypt(clientSecret, password);
            String encryptedTrustStorePass = GatewayCmdUtils.encrypt(trustStorePassword, password);
            Token token = configTokenValues
                    .setUsername(username)
                    .setClientId(clientID)
                    .setClientSecret(encryptedCS)
                    .setTrustStoreLocation(trustStoreLocation)
                    .setTrustStorePassword(encryptedTrustStorePass)
                    .build();
            newConfig.setToken(token);
            newConfig.setCorsConfiguration(GatewayCmdUtils.getDefaultCorsConfig());
            GatewayCmdUtils.saveConfig(newConfig, toolkitConfigPath);
        }
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void setParentCmdParser(JCommander parentCmdParser) {

    }

    private String promptForTextInput(String msg) {
        outStream.println(msg);
        return System.console().readLine();
    }

    private static void init(String configPath, String deploymentConfig, String projectName) {
        try {
            Path configurationFile = Paths.get(configPath);
            if (Files.exists(configurationFile)) {
                Config config = TOMLConfigParser.parse(configPath, Config.class);
                GatewayCmdUtils.setConfig(config);
            } else {
                logger.error("Configuration: {} Not found.", configPath);
                throw new CLIInternalException("Error occurred while loading configurations.");
            }
            if(deploymentConfig != null){
                Path deploymentConfigFile = Paths.get(deploymentConfig);
                if(Files.exists(deploymentConfigFile)){
                    GatewayCmdUtils.createDeploymentConfig(projectName, deploymentConfig);
                }
            }
            String deploymentConfigPath = GatewayCmdUtils.getDeploymentConfigLocation(projectName);
            ContainerConfig containerConfig = TOMLConfigParser.parse(deploymentConfigPath, ContainerConfig.class);
            GatewayCmdUtils.setContainerConfig(containerConfig);
        } catch (ConfigParserException e) {
            logger.error("Error occurred while parsing the configurations {}", configPath, e);
            throw new CLIInternalException("Error occurred while loading configurations.");
        } catch (IOException e){
            throw new CLIInternalException("Error occured while reading the deployment configuration", e);
        }
    }

    /**
     * Validates label, API name and version parameters in for below conditions.
     * 1. Either label should be provided or both API name and version should be provided.
     * 2. Cannot provide all params; i.e. label, API name and version at the same time.
     *
     * @param label   Label name
     * @param apiName API name
     * @param version API version
     */
    private void validateAPIGetRequestParams(String label, String apiName, String version) {
        if ((StringUtils.isEmpty(label) && (StringUtils.isEmpty(apiName) || StringUtils.isEmpty(version))) ||
                StringUtils.isNotEmpty(label) && (StringUtils.isNotEmpty(apiName) || StringUtils.isNotEmpty(version)) ||
                (StringUtils.isEmpty(apiName) && StringUtils.isNotEmpty(version)) ||
                (StringUtils.isNotEmpty(apiName) && StringUtils.isEmpty(version))) {
            throw GatewayCmdUtils.createUsageException(
                    "Either label (-l <label>) or API name (-a <api-name>) with version (-v <version>) "
                            + "should be provided."
                            + "\n\nEx:\tmicro-gw setup accounts-project -l accounts"
                            + "\n\tmicro-gw setup pizzashack-project -a Pizzashack -v 1.0.0");
        }
    }

    /**
     * Set endpoints of publisher, admin, registration and token
     *
     * @param token token from config file
     * @return TokenBuilder modified token to be written back to configuration file
     */
    private TokenBuilder setEndpoints(Token token) {
        //new token values for config(to rewrite configuration file)
        TokenBuilder configTokenValues = new TokenBuilder();

        boolean isEndPointsNeeded; //if endpoint(s) is empty and not defined
        boolean isBaseURLNeeded; //if endpoint(s) contains {baseURL} or endPointsNeeded
        boolean isRestVersionNeeded; //if endpoint(s) contains {restVersion}

        String restVersion = token.getRestVersion();
        publisherEndpoint = token.getPublisherEndpoint();
        adminEndpoint = token.getAdminEndpoint();
        registrationEndpoint = token.getRegistrationEndpoint();
        tokenEndpoint = token.getTokenEndpoint();

        //copy current token config values
        configTokenValues.setPublisherEndpoint(publisherEndpoint);
        configTokenValues.setAdminEndpoint(adminEndpoint);
        configTokenValues.setRegistrationEndpoint(registrationEndpoint);
        configTokenValues.setTokenEndpoint(tokenEndpoint);
        configTokenValues.setRestVersion(restVersion);
        configTokenValues.setBaseURL(token.getBaseURL());

        isEndPointsNeeded = StringUtils.isEmpty(publisherEndpoint) || StringUtils.isEmpty(adminEndpoint) || StringUtils
                .isEmpty(registrationEndpoint) || StringUtils.isEmpty(tokenEndpoint);

        isBaseURLNeeded = publisherEndpoint.contains(RESTServiceConstants.BASE_URL_TAG) ||
                adminEndpoint.contains(RESTServiceConstants.BASE_URL_TAG) ||
                registrationEndpoint.contains(RESTServiceConstants.BASE_URL_TAG) ||
                tokenEndpoint.contains(RESTServiceConstants.BASE_URL_TAG) || isEndPointsNeeded;

        isRestVersionNeeded = publisherEndpoint.contains(RESTServiceConstants.REST_VERSION_TAG) ||
                adminEndpoint.contains(RESTServiceConstants.REST_VERSION_TAG)
                || registrationEndpoint.contains(RESTServiceConstants.REST_VERSION_TAG) || isEndPointsNeeded;

        //set endpoints format if endpoint(s) is empty
        if (isEndPointsNeeded) {
            if (StringUtils.isEmpty(publisherEndpoint)) {
                publisherEndpoint = RESTServiceConstants.CONFIG_PUBLISHER_ENDPOINT;
            }
            if (StringUtils.isEmpty(adminEndpoint)) {
                adminEndpoint = RESTServiceConstants.CONFIG_ADMIN_ENDPOINT;
            }
            if (StringUtils.isEmpty(registrationEndpoint)) {
                registrationEndpoint = RESTServiceConstants.CONFIG_REGISTRATION_ENDPOINT;
            }
            if (StringUtils.isEmpty(tokenEndpoint)) {
                tokenEndpoint = RESTServiceConstants.CONFIG_TOKEN_ENDPOINT;
            }
        }

        //set base URL
        if (isBaseURLNeeded) {

            //if base url not set from setup argument "-s", "--server-url"
            if (StringUtils.isEmpty(baseURL)) {
                baseURL = token.getBaseURL();

                //if baseURL not configured in token, use default host
                if (StringUtils.isEmpty(baseURL)) {
                    baseURL = RESTServiceConstants.DEFAULT_HOST;
                }

                //cli command to ask user to accept the baseURL or enter a new base url
                String userInputURL = getBaseURLfromCmd(baseURL);
                if (!userInputURL.isEmpty()) {
                    baseURL = userInputURL;
                    isOverwriteRequired = true;
                }
            }
            configTokenValues.setBaseURL(baseURL);
        }

        // set rest version
        if (isRestVersionNeeded) {

            if (StringUtils.isEmpty(restVersion)) {
                restVersion = RESTServiceConstants.CONFIG_REST_VERSION;
            }
            informRestVersiontoUser(restVersion);
            configTokenValues.setRestVersion(restVersion);
        }

        if (isBaseURLNeeded || isRestVersionNeeded) {
            publisherEndpoint = publisherEndpoint.replace(RESTServiceConstants.BASE_URL_TAG, baseURL)
                    .replace(RESTServiceConstants.REST_VERSION_TAG, restVersion);
            adminEndpoint = adminEndpoint.replace(RESTServiceConstants.BASE_URL_TAG, baseURL)
                    .replace(RESTServiceConstants.REST_VERSION_TAG, restVersion);
            registrationEndpoint = registrationEndpoint.replace(RESTServiceConstants.BASE_URL_TAG, baseURL)
                    .replace(RESTServiceConstants.REST_VERSION_TAG, restVersion);
            tokenEndpoint = tokenEndpoint.replace(RESTServiceConstants.BASE_URL_TAG, baseURL)
                    .replace(RESTServiceConstants.REST_VERSION_TAG, restVersion);
        }

        //validate URLs
        validateURL(publisherEndpoint);
        validateURL(adminEndpoint);
        validateURL(registrationEndpoint);
        validateURL(tokenEndpoint);

        return configTokenValues;
    }

    /**
     * validate URLs
     *
     * @param urlString url string to be validated
     */
    private void validateURL(String urlString) {

        try {
            new URL(urlString);
        } catch (MalformedURLException e) {
            logger.error("Malformed URL provided {}", urlString);
            throw new CLIInternalException("Error occurred while setting up URL configurations.");
        }

    }

    /**
     * inform user on REST version of endpoint URLs
     *
     * @param restVersion API Manager's REST version
     */
    private void informRestVersiontoUser(String restVersion) {
        outStream.println(
                "You are using REST version - " + restVersion + " of API Manager. (If you want to change this, go to "
                        + "<MICROGW_HOME>/conf/toolkit-config.toml)");
    }

    /**
     * prompt to get the base URL
     */
    private String getBaseURLfromCmd(String defaultBaseURL) {
        String userInputURL;
        userInputURL = promptForTextInput("Enter APIM base URL [" + defaultBaseURL + "]: ").trim();
        return userInputURL;
    }

    private String promptForPasswordInput(String msg) {
        outStream.println(msg);
        return new String(System.console().readPassword());
    }
}