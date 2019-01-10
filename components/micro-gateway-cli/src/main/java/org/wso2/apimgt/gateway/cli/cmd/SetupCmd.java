/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import org.wso2.apimgt.gateway.cli.codegen.CodeGenerationContext;
import org.wso2.apimgt.gateway.cli.codegen.CodeGenerator;
import org.wso2.apimgt.gateway.cli.codegen.ThrottlePolicyGenerator;
import org.wso2.apimgt.gateway.cli.config.TOMLConfigParser;
import org.wso2.apimgt.gateway.cli.constants.GatewayCliConstants;
import org.wso2.apimgt.gateway.cli.constants.RESTServiceConstants;
import org.wso2.apimgt.gateway.cli.exception.*;
import org.wso2.apimgt.gateway.cli.hashing.HashUtils;
import org.wso2.apimgt.gateway.cli.model.config.*;
import org.wso2.apimgt.gateway.cli.model.rest.ClientCertMetadataDTO;
import org.wso2.apimgt.gateway.cli.oauth.OAuthService;
import org.wso2.apimgt.gateway.cli.oauth.OAuthServiceImpl;
import org.wso2.apimgt.gateway.cli.rest.RESTAPIService;
import org.wso2.apimgt.gateway.cli.rest.RESTAPIServiceImpl;
import org.wso2.apimgt.gateway.cli.utils.GatewayCmdUtils;
import org.wso2.carbon.apimgt.rest.api.admin.dto.ApplicationThrottlePolicyDTO;
import org.wso2.carbon.apimgt.rest.api.admin.dto.SubscriptionThrottlePolicyDTO;
import org.wso2.carbon.apimgt.rest.api.publisher.dto.APIDTO;
import org.wso2.carbon.apimgt.rest.api.publisher.dto.APIInfoDTO;

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
 * This class represents the "setup" command and it holds arguments and flags specified by the user.
 */
@Parameters(commandNames = "setup", commandDescription = "setup information")
public class SetupCmd implements GatewayLauncherCmd {

    private static final Logger logger = LoggerFactory.getLogger(SetupCmd.class);
    private static PrintStream outStream = System.err;

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
    private String baseUrl;

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
    @Parameter(names = {"-f", "--force"}, hidden = true, arity = 0)
    private boolean isForcefully;

    @SuppressWarnings("unused")
    @Parameter(names = {"-k", "--insecure"}, hidden = true, arity = 0)
    private boolean isInsecure;

    @Parameter(names = {"-b", "--security"}, hidden = true)
    private String security;

    private String publisherEndpoint;
    private String adminEndpoint;
    private String registrationEndpoint;
    private String tokenEndpoint;
    private String clientSecret;

    private static void init(String projectName, String configPath, String deploymentConfigPath) {
        try {
            GatewayCmdUtils.createProjectStructure(projectName);
            GatewayCmdUtils.createDeploymentConfig(projectName, deploymentConfigPath);

            Path configurationFile = Paths.get(configPath);
            if (Files.exists(configurationFile)) {
                Config config = TOMLConfigParser.parse(configPath, Config.class);
                GatewayCmdUtils.setConfig(config);
            } else {
                logger.error("Configuration: {} Not found.", configPath);
                throw new CLIInternalException("Error occurred while loading configurations.");
            }

            deploymentConfigPath = GatewayCmdUtils.getDeploymentConfigLocation(projectName);
            ContainerConfig containerConfig = TOMLConfigParser.parse(deploymentConfigPath, ContainerConfig.class);
            GatewayCmdUtils.setContainerConfig(containerConfig);

            CodeGenerationContext codeGenerationContext = new CodeGenerationContext();
            codeGenerationContext.setProjectName(projectName);
            GatewayCmdUtils.setCodeGenerationContext(codeGenerationContext);
        } catch (ConfigParserException e) {
            logger.error("Error occurred while parsing the configurations {}", configPath, e);
            throw new CLIInternalException("Error occurred while loading configurations.");
        } catch (IOException e) {
            logger.error("Error occurred while generating project configurationss", e);
            throw new CLIInternalException("Error occurred while loading configurations.");
        }
    }

    public void execute() {
        String clientID;
        String workspace = GatewayCmdUtils.getUserDir();

        String projectName = GatewayCmdUtils.getProjectName(mainArgs);
        validateAPIGetRequestParams(label, apiName, version);

        if (StringUtils.isEmpty(toolkitConfigPath)) {
            toolkitConfigPath = GatewayCmdUtils.getMainConfigLocation();
        }

        if (new File(workspace + File.separator + projectName).exists() && !isForcefully) {
            throw GatewayCmdUtils.createUsageException("Project name `" + projectName
                    + "` already exist. use -f or --force to forcefully update the project directory.");
        }
        init(projectName, toolkitConfigPath, deploymentConfigPath);

        Config config = GatewayCmdUtils.getConfig();
        boolean isOverwriteRequired = false;


        //Setup username
        String configuredUser = config.getToken().getUsername();
        if (StringUtils.isEmpty(configuredUser)) {
            if (StringUtils.isEmpty(username)) {
                isOverwriteRequired = true;
                if ((username = promptForTextInput("Enter Username: ")).trim().isEmpty()) {
                    throw GatewayCmdUtils.createUsageException("Micro gateway setup failed: empty username.");
                }
            }
        } else {
            username = configuredUser;
        }

        //Setup password
        if (StringUtils.isEmpty(password)) {
            if ((password = promptForPasswordInput("Enter Password for " + username + ": ")).trim().isEmpty()) {
                if (StringUtils.isEmpty(password)) {
                    password = promptForPasswordInput("Password can't be empty; enter password for " + username + ": ");
                    if (password.trim().isEmpty()) {
                        throw GatewayCmdUtils.createUsageException("Micro gateway setup failed: empty password.");
                    }
                }
            }
        }

        //Setup urls
        publisherEndpoint = config.getToken().getPublisherEndpoint();
        adminEndpoint = config.getToken().getAdminEndpoint();
        registrationEndpoint = config.getToken().getRegistrationEndpoint();
        tokenEndpoint = config.getToken().getTokenEndpoint();
        if (StringUtils.isEmpty(publisherEndpoint) || StringUtils.isEmpty(adminEndpoint) || StringUtils
                .isEmpty(registrationEndpoint) || StringUtils.isEmpty(tokenEndpoint)) {
            if (StringUtils.isEmpty(baseUrl)) {
                isOverwriteRequired = true;
                if ((baseUrl = promptForTextInput("Enter APIM base URL [" + RESTServiceConstants.DEFAULT_HOST + "]: "))
                        .trim().isEmpty()) {
                    baseUrl = RESTServiceConstants.DEFAULT_HOST;
                }
            }
            populateHosts(baseUrl);
        }

        //configure trust store
        String configuredTrustStore = config.getToken().getTrustStoreLocation();
        if (StringUtils.isEmpty(configuredTrustStore)) {
            if (StringUtils.isEmpty(trustStoreLocation)) {
                isOverwriteRequired = true;
                if ((trustStoreLocation = promptForTextInput(
                        "Enter Trust store location: [" + RESTServiceConstants.DEFAULT_TRUSTSTORE_PATH + "]")).trim()
                        .isEmpty()) {
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
                if ((trustStorePassword = promptForPasswordInput("Enter Trust store password: [ use default? ]")).trim()
                        .isEmpty()) {
                    trustStorePassword = RESTServiceConstants.DEFAULT_TRUSTSTORE_PASS;
                }
            }
        } else {
            trustStorePassword = configuredTrustStorePass;
        }

        File trustStoreFile = new File(trustStoreLocation);
        if (!trustStoreFile.isAbsolute()) {
            trustStoreLocation = GatewayCmdUtils
                    .getUnixPath(GatewayCmdUtils.getCLIHome() + File.separator + trustStoreLocation);
        }
        trustStoreFile = new File(trustStoreLocation);
        if (!trustStoreFile.exists()) {
            logger.error("Provided trust store location {} does not exist.", trustStoreLocation);
            throw new CLIRuntimeException("Provided trust store location does not exist.");
        }

        //set the trustStore
        System.setProperty("javax.net.ssl.keyStoreType", "pkcs12");
        System.setProperty("javax.net.ssl.trustStore", trustStoreLocation);
        System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);

        //Security Schemas settings
        if (security == null) {
            security = "oauth2";
        } else if (security == "") {
            security = "oauth2";
        }
        setSecuritySchemas(security);

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

        List<APIInfoDTO> apis = new ArrayList<>();
        List<String> swaggerDefinitionList;
        RESTAPIService service = new RESTAPIServiceImpl(publisherEndpoint, adminEndpoint, isInsecure);
        APIDTO api;
        if (label != null) {
            apis = service.getAPIs(label, accessToken);
        } else {
            api = service.getAPI(apiName, version, accessToken);
            if (api != null) {
                apis.add(api);
            }
        }
        if (apis == null || (apis != null && apis.isEmpty())) {
            // Delete folder
            GatewayCmdUtils.deleteProject(workspace + File.separator + projectName);
            String errorMsg;
            if (label != null) {
                errorMsg = "No APIs found for the given label: " + label;
            } else {
                errorMsg = "No Published APIs matched for name:" + apiName + ", version:" + version;
            }
            throw new CLIRuntimeException(errorMsg);
        }
        //get the swagger definitions of each api
        swaggerDefinitionList = new ArrayList<>();
        for (APIInfoDTO apidto : apis) {
            swaggerDefinitionList.add(service.getAPISwaggerDefinition(apidto.getId(), accessToken));
        }
        List<ApplicationThrottlePolicyDTO> applicationPolicies = service.getApplicationPolicies(accessToken);
        List<SubscriptionThrottlePolicyDTO> subscriptionPolicies = service.getSubscriptionPolicies(accessToken);
        List<ClientCertMetadataDTO> clientCertificates = service.getClientCertificates(accessToken);
        logger.info(String.valueOf(clientCertificates));

        ThrottlePolicyGenerator policyGenerator = new ThrottlePolicyGenerator();
        CodeGenerator codeGenerator = new CodeGenerator();
        boolean changesDetected;
        try {
            policyGenerator.generate(GatewayCmdUtils.getProjectSrcDirectoryPath(projectName) + File.separator
                    + GatewayCliConstants.POLICY_DIR, applicationPolicies, subscriptionPolicies);
            codeGenerator.generate(projectName, apis, swaggerDefinitionList, true);
            //Initializing the ballerina project and creating .bal folder.
            InitHandler.initialize(Paths.get(GatewayCmdUtils.getProjectDirectoryPath(projectName)), null,
                    new ArrayList<>(), null);
            try {
                changesDetected = HashUtils.detectChanges(apis, subscriptionPolicies, applicationPolicies, projectName);
            } catch (HashingException e) {
                logger.error("Error while checking for changes of resources. Skipping no-change detection..");
                throw new CLIInternalException(
                        "Error while checking for changes of resources. Skipping no-change detection..");
            }
        } catch (IOException | BallerinaServiceGenException e) {
            logger.error("Error while generating ballerina source.");
            throw new CLIInternalException("Error while generating ballerina source.");
        }

        //if all the operations are success, write new config to file
        if (isOverwriteRequired) {
            Config newConfig = new Config();
            Client client = new Client();
            client.setHttpRequestTimeout(1000000);
            newConfig.setClient(client);

            String encryptedCS = GatewayCmdUtils.encrypt(clientSecret, password);
            String encryptedTrustStorePass = GatewayCmdUtils.encrypt(trustStorePassword, password);
            Token token = new TokenBuilder().setPublisherEndpoint(publisherEndpoint).setAdminEndpoint(adminEndpoint)
                    .setRegistrationEndpoint(registrationEndpoint).setTokenEndpoint(tokenEndpoint).setUsername(username)
                    .setClientId(clientID).setClientSecret(encryptedCS).setTrustStoreLocation(trustStoreLocation)
                    .setTrustStorePassword(encryptedTrustStorePass).build();
            newConfig.setToken(token);
            newConfig.setCorsConfiguration(GatewayCmdUtils.getDefaultCorsConfig());
            GatewayCmdUtils.saveConfig(newConfig, toolkitConfigPath);
        }

        if (!changesDetected) {
            outStream.println("No changes received from the server since the previous setup."
                    + " If you have already a built distribution, it can be reused.");
        }
        outStream.println("Setting up project " + projectName + " is successful.");

        //There should not be any logic after this system exit
        if (!changesDetected) {
            Runtime.getRuntime().exit(GatewayCliConstants.EXIT_CODE_NOT_MODIFIED);
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
        if ((StringUtils.isEmpty(label) && (StringUtils.isEmpty(apiName) || StringUtils.isEmpty(version)))
                || StringUtils.isNotEmpty(label) && (StringUtils.isNotEmpty(apiName) || StringUtils.isNotEmpty(version))
                || (StringUtils.isEmpty(apiName) && StringUtils.isNotEmpty(version)) || (StringUtils.isNotEmpty(apiName)
                && StringUtils.isEmpty(version))) {
            throw GatewayCmdUtils.createUsageException(
                    "Either label (-l <label>) or API name (-a <api-name>) with version (-v <version>) "
                            + "should be provided." + "\n\nEx:\tmicro-gw setup accounts-project -l accounts"
                            + "\n\tmicro-gw setup pizzashack-project -a Pizzashack -v 1.0.0");
        }
    }

    @Override
    public String getName() {
        return GatewayCliCommands.SETUP;
    }

    @Override
    public void setParentCmdParser(JCommander parentCmdParser) {
    }

    private String promptForTextInput(String msg) {
        outStream.println(msg);
        return System.console().readLine();
    }

    private String promptForPasswordInput(String msg) {
        outStream.println(msg);
        return new String(System.console().readPassword());
    }

    private void populateHosts(String host) {
        try {
            publisherEndpoint = (publisherEndpoint != null && !publisherEndpoint.isEmpty()) ?
                    publisherEndpoint :
                    new URL(new URL(host), RESTServiceConstants.PUB_RESOURCE_PATH).toString();
            adminEndpoint = (adminEndpoint != null && !adminEndpoint.isEmpty()) ?
                    adminEndpoint :
                    new URL(new URL(host), RESTServiceConstants.ADMIN_RESOURCE_PATH).toString();
            registrationEndpoint = (registrationEndpoint != null && !registrationEndpoint.isEmpty()) ?
                    registrationEndpoint :
                    new URL(new URL(host), RESTServiceConstants.DCR_RESOURCE_PATH).toString();
            tokenEndpoint = (tokenEndpoint != null && !tokenEndpoint.isEmpty()) ?
                    tokenEndpoint :
                    new URL(new URL(host), RESTServiceConstants.TOKEN_PATH).toString();
        } catch (MalformedURLException e) {
            logger.error("Malformed URL provided {}", host);
            throw new CLIInternalException("Error occurred while setting up URL configurations.");
        }
    }

    /**
     * Get the security Schemas string and based on that set boolean values for schemas to be used in service.bal.
     */
    public void setSecuritySchemas(String schemas) {
        Config config = GatewayCmdUtils.getConfig();
        BasicAuth basicAuth = new BasicAuth();
        boolean basic = false;
        boolean oauth2 = false;
        String[] schemasArray = schemas.trim().split("\\s*,\\s*");
        for (int i = 0; i < schemasArray.length; i++) {
            if (schemasArray[i].equalsIgnoreCase("basic")) {
                basic = true;
            } else if (schemasArray[i].equalsIgnoreCase("oauth2")) {
                oauth2 = true;
            }
        }
        if (basic && oauth2) {
            basicAuth.setOptional(true);
            basicAuth.setRequired(false);
        } else if (basic && !oauth2) {
            basicAuth.setRequired(true);
            basicAuth.setOptional(false);
        } else if (!basic && oauth2) {
            basicAuth.setOptional(false);
            basicAuth.setRequired(false);
        }
        config.setBasicAuth(basicAuth);
    }
}
