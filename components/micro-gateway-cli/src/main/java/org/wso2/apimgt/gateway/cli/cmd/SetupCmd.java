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
import org.wso2.apimgt.gateway.cli.codegen.CodeGenerationContext;
import org.wso2.apimgt.gateway.cli.codegen.CodeGenerator;
import org.wso2.apimgt.gateway.cli.codegen.ThrottlePolicyGenerator;
import org.wso2.apimgt.gateway.cli.config.TOMLConfigParser;
import org.wso2.apimgt.gateway.cli.constants.GatewayCliConstants;
import org.wso2.apimgt.gateway.cli.constants.RESTServiceConstants;
import org.wso2.apimgt.gateway.cli.exception.BallerinaServiceGenException;
import org.wso2.apimgt.gateway.cli.exception.CliLauncherException;
import org.wso2.apimgt.gateway.cli.exception.ConfigParserException;
import org.wso2.apimgt.gateway.cli.exception.HashingException;
import org.wso2.apimgt.gateway.cli.hashing.HashUtils;
import org.wso2.apimgt.gateway.cli.model.config.Client;
import org.wso2.apimgt.gateway.cli.model.config.Config;
import org.wso2.apimgt.gateway.cli.model.config.ContainerConfig;
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
    private static PrintStream outStream = System.err;
    @Parameter(names = "--java.debug", hidden = true)
    private String javaDebugPort;

    @Parameter(names = { "-u", "--username" }, hidden = true)
    private String username;

    @Parameter(names = { "-p", "--password" }, hidden = true)
    private String password;

    @Parameter(names = { "-l", "--label" }, hidden = true)
    private String label;

    @Parameter(names = { "--reset" }, hidden = true)
    private boolean reset;

    @Parameter(names = { "--path" }, hidden = true)
    private String workspace;

    @Parameter(names = { "--server-url" }, hidden = true)
    private String baseUrl;

    @Parameter(names = { "-t", "--truststore" }, hidden = true)
    private String trustStoreLocation;

    @Parameter(names = { "-s", "--truststore-pass" }, hidden = true)
    private String trustStorePassword;

    @Parameter(names = { "-n", "--project" }, hidden = true)
    private String projectName;

    @Parameter(names = { "-c", "--config" }, hidden = true)
    private String configPath;

    private String publisherEndpoint;
    private String adminEndpoint;
    private String registrationEndpoint;
    private String tokenEndpoint;
    private String clientID;
    private String clientSecret;

    public void execute() {
        //initialize CLI with the provided path. First time the cli runs it is a must to provide this. Once it is
        // provided, it is stored in <CLI_HOME>/temp/workspace.txt. In next runs, no need to provide the path and
        // path is taken from above file.
        if (StringUtils.isEmpty(label)) {
            outStream.println("Label can't be empty. You need to specify -l <label name>");
            return;
        }

        if (StringUtils.isEmpty(projectName)) {
            projectName = label;
        }

        if (StringUtils.isEmpty(configPath)) {
            configPath = GatewayCmdUtils.getMainConfigLocation();
        }

        init(workspace, projectName, configPath);

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
                    populateHosts(baseUrl);
                }
            } else {
                populateHosts(baseUrl);
            }
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
                configuredTrustStorePass = GatewayCmdUtils.decrypt(encryptedPass, new String(password));
            } catch (CliLauncherException e) {
                //different password used to encrypt
                configuredTrustStorePass = null;
            }
        }

        if (StringUtils.isEmpty(configuredTrustStorePass)) {
            if (StringUtils.isEmpty(trustStorePassword)) {
                isOverwriteRequired = true;
                if ((trustStorePassword = promptForTextInput("Enter Trust store password: [ use default? ]")).trim()
                        .isEmpty()) {
                    trustStorePassword = RESTServiceConstants.DEFAULT_TRUSTSTORE_PASS;
                }
            }
        } else {
            trustStorePassword = configuredTrustStorePass;
        }

        File trustStoreFile = new File(trustStoreLocation);
        if (!trustStoreFile.isAbsolute()) {
            trustStoreLocation = GatewayCmdUtils.getCLIHome() + File.separator + trustStoreLocation;
        }
        trustStoreFile = new File(trustStoreLocation);
        if (!trustStoreFile.exists()) {
            System.err.println("Error while loading trust store location: " + trustStoreLocation);
            Runtime.getRuntime().exit(1);
        }

        //set the trustStore
        System.setProperty("javax.net.ssl.keyStoreType", "pkcs12");
        System.setProperty("javax.net.ssl.trustStore", trustStoreLocation);
        System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);

        OAuthService manager = new OAuthServiceImpl();
        if (config != null) {
            clientID = config.getToken().getClientId();
            String encryptedSecret = config.getToken().getClientSecret();
            if (!StringUtils.isEmpty(clientID.trim()) && !StringUtils.isEmpty(encryptedSecret.trim())) {
                try {
                    clientSecret = GatewayCmdUtils.decrypt(encryptedSecret, new String(password));
                } catch (CliLauncherException e) {
                    //different password used to encrypt
                    clientSecret = null;
                }
            }
        }

        if (StringUtils.isEmpty(clientID) || StringUtils.isEmpty(clientSecret)) {
            String[] clientInfo = manager
                    .generateClientIdAndSecret(registrationEndpoint, username, password.toCharArray());
            clientID = clientInfo[0];
            clientSecret = clientInfo[1];
        }

        String accessToken = manager
                .generateAccessToken(tokenEndpoint, username, password.toCharArray(), clientID, clientSecret);

        RESTAPIService service = new RESTAPIServiceImpl(publisherEndpoint, adminEndpoint);
        List<ExtendedAPI> apis = service.getAPIs(label, accessToken);
        List<ApplicationThrottlePolicyDTO> applicationPolicies = service.getApplicationPolicies(accessToken);
        List<SubscriptionThrottlePolicyDTO> subscriptionPolicies = service.getSubscriptionPolicies(accessToken);

        ThrottlePolicyGenerator policyGenerator = new ThrottlePolicyGenerator();
        CodeGenerator codeGenerator = new CodeGenerator();
        boolean changesDetected = false;
        try {
            policyGenerator.generate(GatewayCmdUtils.getLabelSrcDirectoryPath(workspace, projectName) + File.separator
                    + GatewayCliConstants.POLICY_DIR, applicationPolicies, subscriptionPolicies);
            codeGenerator.generate(workspace, projectName, apis, true);
            //Initializing the ballerina label project and creating .bal folder.
            InitHandler.initialize(Paths.get(GatewayCmdUtils.getLabelDirectoryPath(workspace, projectName)), null,
                    new ArrayList<>(), null);
            try {
                changesDetected = HashUtils.detectChanges(apis, subscriptionPolicies, applicationPolicies);
            } catch (HashingException e) {
                outStream.println("Error while checking for changes of resources. Skipping no-change detection..");
                Runtime.getRuntime().exit(1);
            }
        } catch (IOException | BallerinaServiceGenException e) {
            outStream.println("Error while generating ballerina source");
            e.printStackTrace();
            Runtime.getRuntime().exit(1);
        }

        //if all the operations are success, write new config to file
        if (isOverwriteRequired) {
            Config newConfig = new Config();
            Client client = new Client();
            client.setHttpRequestTimeout(1000000);
            newConfig.setClient(client);

            String encryptedSecret = GatewayCmdUtils.encrypt(clientSecret, new String(password));
            String encryptedTrustStorePass = GatewayCmdUtils.encrypt(trustStorePassword, new String(password));
            Token token = new TokenBuilder().setPublisherEndpoint(publisherEndpoint).setAdminEndpoint(adminEndpoint)
                    .setRegistrationEndpoint(registrationEndpoint).setTokenEndpoint(tokenEndpoint).setUsername(username)
                    .setClientId(clientID).setClientSecret(encryptedSecret).setTrustStoreLocation(trustStoreLocation)
                    .setTrustStorePassword(encryptedTrustStorePass).build();
            newConfig.setToken(token);
            newConfig.setCorsConfiguration(GatewayCmdUtils.getDefaultCorsConfig());
            GatewayCmdUtils.saveConfig(newConfig, configPath);
        }

        //There should not be any logic after this system exit
        if (!changesDetected) {
            outStream.println("No changes from server.");
            Runtime.getRuntime().exit(GatewayCliConstants.EXIT_CODE_NOT_MODIFIED);
        }
    }

    @Override
    public String getName() {
        return GatewayCliCommands.HELP;
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
            publisherEndpoint = new URL(new URL(host), RESTServiceConstants.PUB_RESOURCE_PATH).toString();
            adminEndpoint = new URL(new URL(host), RESTServiceConstants.ADMIN_RESOURCE_PATH).toString();
            registrationEndpoint = new URL(new URL(host), RESTServiceConstants.DCR_RESOURCE_PATH).toString();
            tokenEndpoint = new URL(new URL(host), RESTServiceConstants.TOKEN_PATH).toString();
        } catch (MalformedURLException e) {
            throw new RuntimeException("Malformed URL is provided");
        }
    }

    private static void init(String overrideProjectRootPath, String label, String configPath) {
        try {
            String projectRoot;
            if (StringUtils.isBlank(overrideProjectRootPath)) {
                String storedProjectRoot = GatewayCmdUtils.getStoredWorkspaceLocation();
                if (StringUtils.isBlank(storedProjectRoot)) {
                    outStream.println("Stored workspace path not available. "
                            + "You need to specify --path <path to generate resources>");
                    Runtime.getRuntime().exit(1);
                }
                projectRoot = storedProjectRoot;
            } else {
                projectRoot = overrideProjectRootPath;
                GatewayCmdUtils.storeWorkspaceLocation(projectRoot);
                GatewayCmdUtils.createMainProjectStructure(projectRoot);
                GatewayCmdUtils.createLabelProjectStructure(projectRoot, label);
                GatewayCmdUtils.createLabelConfig(projectRoot, label);
            }

            //user can define different label time to time. So need to create irrespective path provided or not.
            GatewayCmdUtils.createLabelProjectStructure(projectRoot, label);

            Path configurationFile = Paths.get(configPath);
            if (Files.exists(configurationFile)) {
                Config config = TOMLConfigParser.parse(configPath, Config.class);
                GatewayCmdUtils.setConfig(config);
            } else {
                outStream.println("Config: " + configPath + " Not found.");
                Runtime.getRuntime().exit(1);
            }

            String labelConfigPath = GatewayCmdUtils.getLabelConfigLocation(projectRoot, label);
            ContainerConfig containerConfig = TOMLConfigParser.parse(labelConfigPath, ContainerConfig.class);
            GatewayCmdUtils.setContainerConfig(containerConfig);

            CodeGenerationContext codeGenerationContext = new CodeGenerationContext();
            codeGenerationContext.setLabel(label);
            GatewayCmdUtils.setCodeGenerationContext(codeGenerationContext);
        } catch (ConfigParserException e) {
            outStream.println(
                    "Error while parsing the config" + (e.getCause() != null ? ": " + e.getCause().getMessage() : ""));
            Runtime.getRuntime().exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            outStream.println("Error while processing files:" + e.getMessage());
            Runtime.getRuntime().exit(1);
        }
    }
}