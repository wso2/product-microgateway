/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.apimgt.gateway.cli.cmd;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.MissingCommandException;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
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
import org.wso2.apimgt.gateway.cli.exception.BallerinaServiceGenException;
import org.wso2.apimgt.gateway.cli.exception.CliLauncherException;
import org.wso2.apimgt.gateway.cli.exception.ConfigParserException;
import org.wso2.apimgt.gateway.cli.exception.HashingException;
import org.wso2.apimgt.gateway.cli.hashing.HashUtils;
import org.wso2.apimgt.gateway.cli.model.config.Client;
import org.wso2.apimgt.gateway.cli.model.config.Config;
import org.wso2.apimgt.gateway.cli.model.config.ContainerConfig;
import org.wso2.apimgt.gateway.cli.model.config.Token;
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
import java.util.Map;
import java.util.Optional;

/**
 * This class executes the gateway cli program.
 */
public class Main {
    private static final String JC_UNKNOWN_OPTION_PREFIX = "Unknown option:";
    private static final String JC_EXPECTED_A_VALUE_AFTER_PARAMETER_PREFIX = "Expected a value after parameter";
    public static final String MICRO_GW = "micro-gw";

    private static PrintStream outStream = System.err;

    private static final Logger cliLog = LoggerFactory.getLogger(Main.class);

    public static void main(String... args) {
        try {
            Optional<GatewayLauncherCmd> optionalInvokedCmd = getInvokedCmd(args);
            optionalInvokedCmd.ifPresent(GatewayLauncherCmd::execute);
        } catch (CliLauncherException e) {
            outStream.println(e.getMessages());
            Runtime.getRuntime().exit(1);
        }
    }

    private static void init(String overrideProjectRootPath, String label) {
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

            String configPath = GatewayCmdUtils.getMainConfigLocation(projectRoot);
            Path configurationFile = Paths.get(configPath);
            if (Files.exists(configurationFile)) {
                Config config = TOMLConfigParser.parse(configPath, Config.class);
                System.setProperty("javax.net.ssl.keyStoreType", "pkcs12");
                System.setProperty("javax.net.ssl.trustStore", config.getToken().getTrustStoreAbsoluteLocation());
                System.setProperty("javax.net.ssl.trustStorePassword", config.getToken().getTrustStorePassword());
                GatewayCmdUtils.setConfig(config);
            } else {
                GatewayCmdUtils.createMainConfig(projectRoot);
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

    /**
     * Get the invoke CMD from the specified arguments
     *
     * @param args list of arguments
     * @return invoked CMD
     */
    private static Optional<GatewayLauncherCmd> getInvokedCmd(String... args) {
        try {
            DefaultCmd defaultCmd = new DefaultCmd();
            JCommander cmdParser = new JCommander(defaultCmd);
            defaultCmd.setParentCmdParser(cmdParser);

            HelpCmd helpCmd = new HelpCmd();
            cmdParser.addCommand(GatewayCliCommands.HELP, helpCmd);
            helpCmd.setParentCmdParser(cmdParser);

            SetupCmd setupCmd = new SetupCmd();
            cmdParser.addCommand(GatewayCliCommands.SETUP, setupCmd);
            setupCmd.setParentCmdParser(cmdParser);

            BuildCmd buildCmd = new BuildCmd();
            cmdParser.addCommand(GatewayCliCommands.BUILD, buildCmd);
            buildCmd.setParentCmdParser(cmdParser);

            RunCmd runCmd = new RunCmd();
            cmdParser.addCommand(GatewayCliCommands.RUN, runCmd);
            runCmd.setParentCmdParser(cmdParser);

            cmdParser.setProgramName(MICRO_GW);
            cmdParser.parse(args);
            String parsedCmdName = cmdParser.getParsedCommand();

            // User has not specified a command. Therefore returning the main command
            // which simply prints usage information.
            if (parsedCmdName == null) {
                return Optional.of(defaultCmd);
            }

            Map<String, JCommander> commanderMap = cmdParser.getCommands();
            return Optional.of((GatewayLauncherCmd) commanderMap.get(parsedCmdName).getObjects().get(0));
        } catch (MissingCommandException e) {
            String errorMsg = "Unknown command '" + e.getUnknownCommand() + "'";
            throw GatewayCmdUtils.createUsageException(errorMsg);

        } catch (ParameterException e) {
            String msg = e.getMessage();
            if (msg == null) {
                throw GatewayCmdUtils.createUsageException("Internal error occurred");

            } else if (msg.startsWith(JC_UNKNOWN_OPTION_PREFIX)) {
                String flag = msg.substring(JC_UNKNOWN_OPTION_PREFIX.length());
                throw GatewayCmdUtils.createUsageException("Unknown flag '" + flag.trim() + "'");

            } else if (msg.startsWith(JC_EXPECTED_A_VALUE_AFTER_PARAMETER_PREFIX)) {
                String flag = msg.substring(JC_EXPECTED_A_VALUE_AFTER_PARAMETER_PREFIX.length());
                throw GatewayCmdUtils.createUsageException("Flag '" + flag.trim() + "' needs an argument");

            } else {
                // Make the first character of the error message lower case
                throw GatewayCmdUtils.createUsageException(GatewayCmdUtils.makeFirstLetterLowerCase(msg));
            }
        }
    }

    private static void printUsageInfo(String commandName) {
        String usageInfo = GatewayLauncherCmd.getCommandUsageInfo(commandName);
        outStream.println(usageInfo);
    }

    /**
     * This class represents the "help" command and it holds arguments and flags specified by the user.
     */
    @Parameters(commandNames = "help", commandDescription = "print usage information")
    private static class HelpCmd implements GatewayLauncherCmd {

        @Parameter(description = "Command name")
        private List<String> helpCommands;

        @Parameter(names = "--java.debug", hidden = true)
        private String javaDebugPort;

        private JCommander parentCmdParser;

        public void execute() {
            if (helpCommands == null) {
                printUsageInfo(GatewayCliCommands.HELP);
                return;

            } else if (helpCommands.size() > 1) {
                throw GatewayCmdUtils.createUsageException("too many arguments given");
            }

            String userCommand = helpCommands.get(0);
            if (parentCmdParser.getCommands().get(userCommand) == null) {
                throw GatewayCmdUtils.createUsageException("unknown help topic `" + userCommand + "`");
            }

            String commandUsageInfo = GatewayLauncherCmd.getCommandUsageInfo(userCommand);
            outStream.println(commandUsageInfo);
        }

        @Override
        public String getName() {
            return GatewayCliCommands.HELP;
        }

        @Override
        public void setParentCmdParser(JCommander parentCmdParser) {
            this.parentCmdParser = parentCmdParser;
        }

    }

    /**
     * This class represents the "setup" command and it holds arguments and flags specified by the user.
     */
    @Parameters(commandNames = "setup", commandDescription = "setup information")
    private static class SetupCmd implements GatewayLauncherCmd {

        @Parameter(names = "--java.debug", hidden = true)
        private String javaDebugPort;

        @Parameter(names = { "-u", "--user" }, hidden = true)
        private String username;

        @Parameter(names = { "-p", "--password" }, hidden = true)
        private String password;

        @Parameter(names = { "-l", "--label" }, hidden = true)
        private String label;

        @Parameter(names = { "--reset" }, hidden = true)
        private boolean reset;

        @Parameter(names = { "--path" }, hidden = true)
        private String path;

        @Parameter(names = { "--base-url" }, hidden = true)
        private String baseUrl;

        @Parameter(names = { "-t", "--trustStore" }, hidden = true)
        private String trustStoreLocation;

        @Parameter(names = { "-s", "--trustStorePass" }, hidden = true)
        private String trustStorePassword;

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
                outStream.println("Label can't be empty. " + "You need to specify -l <label name>");
                return;
            }

            init(path, label);

            Config config = GatewayCmdUtils.getConfig();
            String projectRoot = StringUtils.EMPTY;
            try {
                projectRoot = GatewayCmdUtils.getStoredWorkspaceLocation();
            } catch (IOException e) {
                e.printStackTrace();
                outStream.println("Stored workspace path not available. "
                        + "You need to specify --path <path to generate resources>");
                Runtime.getRuntime().exit(1);
            }

            if (reset) {
                config.getToken().setClientId(StringUtils.EMPTY);
                config.getToken().setClientSecret(StringUtils.EMPTY);
                config.getToken().setUsername(StringUtils.EMPTY);
                try {
                    TOMLConfigParser.write(GatewayCmdUtils.getMainConfigDirPath(projectRoot), config);
                } catch (ConfigParserException e) {
                    e.printStackTrace();
                    outStream.println("Error occurred while writing to the config file");
                    Runtime.getRuntime().exit(1);
                }
            }

            boolean isOverwriteRequired = false;

            //Setup username
            //If config is there
            if (config != null) {
                //get previous user
                String configuredUser = config.getToken().getUsername();
                // check username is also provided
                if (!StringUtils.isEmpty(username)) {
                    // if provided user is different user it and mark as config is need to changed
                    if (!username.equalsIgnoreCase(configuredUser)) {
                        username = configuredUser;
                        isOverwriteRequired = true;
                    }
                    // if username is not provided, get the previous user
                } else {
                    username = configuredUser;
                }
            } else {
                isOverwriteRequired = true;
                // If there is no config file, prompt user to enter
                if (StringUtils.isEmpty(username)) {
                    if ((username = promptForTextInput("Enter Username: ")).trim().isEmpty()) {
                        if ((username = promptForTextInput("Enter Username: ")).trim().isEmpty()) {
                            throw GatewayCmdUtils.createUsageException("Micro gateway setup failed: empty username.");
                        }
                    }
                }
            }

            //Setup password
            if (StringUtils.isEmpty(password)) {
                if ((password = promptForPasswordInput("Enter Password for " + username + ": ")).trim().isEmpty()) {
                    if (StringUtils.isEmpty(password)) {
                        password = promptForPasswordInput(
                                "Password can't be empty; enter password for " + username + ": ");
                        if (password.trim().isEmpty()) {
                            throw GatewayCmdUtils.createUsageException("Micro gateway setup failed: empty password.");
                        }
                    }
                }
            }

            //Setup urls
            //If config is there
            if (config != null) {
                //get previous publisher endpoint
                String existingPublisherEndpoint = config.getToken().getPublisherEndpoint();
                // check baseUrl is also provided
                if (!StringUtils.isEmpty(baseUrl)) {
                    // if provided baseUrl is different, use it and mark as config is need to changed
                    String newPublisherEndpoint;
                    try {
                        newPublisherEndpoint = new URL(new URL(baseUrl), RESTServiceConstants.PUB_RESOURCE_PATH)
                                .toString();
                    } catch (MalformedURLException e) {
                        outStream.println("Existing publisher endpoint is malformed. Please reset and setup again.");
                        throw new RuntimeException(
                                "Existing publisher endpoint is malformed. Please reset and setup again.");
                    }
                    if (!newPublisherEndpoint.toString().equalsIgnoreCase(existingPublisherEndpoint)) {
                        // If baseUrl is different derive urls again
                        populateHosts(baseUrl);
                        isOverwriteRequired = true;
                    }
                } else {
                    //If same url is provided, use the existing config
                    publisherEndpoint = config.getToken().getPublisherEndpoint();
                    adminEndpoint = config.getToken().getAdminEndpoint();
                    registrationEndpoint = config.getToken().getRegistrationEndpoint();
                    tokenEndpoint = config.getToken().getTokenEndpoint();
                }
            } else {
                // If there is no config file, prompt user to enter baseUrl
                if (StringUtils.isEmpty(baseUrl)) {
                    if ((baseUrl = promptForTextInput(
                            "Enter APIM base URL [" + RESTServiceConstants.DEFAULT_HOST + "]: ")).trim().isEmpty()) {
                        baseUrl = RESTServiceConstants.DEFAULT_HOST;
                    }
                }
                // derive urls with given url
                populateHosts(baseUrl);
            }

            //configure trust store
            //If config is there
            if (config != null) {
                //get previous trust store
                String configuredTrustStore = config.getToken().getTrustStoreLocation();
                // check trust store is also provided
                if (!StringUtils.isEmpty(trustStoreLocation)) {
                    // if provided trust store is different use it and mark as config is need to changed
                    if (!trustStoreLocation.equalsIgnoreCase(configuredTrustStore)) {
                        isOverwriteRequired = true;
                    }
                    // if trust store is not provided, get the previous trust store
                } else {
                    trustStoreLocation = configuredTrustStore;
                }
            } else {
                // If there is no config file, prompt user to enter
                if (StringUtils.isEmpty(trustStoreLocation)) {
                    if ((trustStoreLocation = promptForTextInput(
                            "Enter Trust store location: [" + RESTServiceConstants.DEFAULT_TRUSTSTORE_PATH + "]"))
                            .trim().isEmpty()) {
                        trustStoreLocation = RESTServiceConstants.DEFAULT_TRUSTSTORE_PATH;
                    }
                }
            }

            //configure trust store password
            //If config is there
            if (config != null) {
                //get previous trust store password
                String configuredTrustStorePass = config.getToken().getTrustStorePassword();
                // check trust store password is also provided
                if (!StringUtils.isEmpty(trustStorePassword)) {
                    // if provided trust store password is different use it and mark as config is need to changed
                    if (!trustStorePassword.equalsIgnoreCase(configuredTrustStorePass)) {
                        isOverwriteRequired = true;
                    }
                    // if trust store password is not provided, get the previous trust store password
                } else {
                    trustStorePassword = configuredTrustStorePass;
                }
            } else {
                // If there is no config file, prompt user to enter
                if (StringUtils.isEmpty(trustStorePassword)) {
                    if ((trustStorePassword = promptForTextInput("Enter Trust store password: [ use default? ]")).trim()
                            .isEmpty()) {
                        trustStorePassword = RESTServiceConstants.DEFAULT_TRUSTSTORE_PASS;
                    }
                }
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

            //set the trustStore again
            if (isOverwriteRequired) {
                System.setProperty("javax.net.ssl.keyStoreType", "pkcs12");
                System.setProperty("javax.net.ssl.trustStore", trustStoreLocation);
                System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
            }

            OAuthService manager = new OAuthServiceImpl();
            if (config != null) {
                clientID = config.getToken().getClientId();
                String encryptedSecret = config.getToken().getClientSecret();
                clientSecret = GatewayCmdUtils.decrypt(encryptedSecret, new String(password));
            }

            if (clientID == null) {
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
            try {
                policyGenerator.generate(GatewayCmdUtils.getLabelSrcDirectoryPath(projectRoot, label) + File.separator
                        + GatewayCliConstants.POLICY_DIR, applicationPolicies, subscriptionPolicies);
                codeGenerator.generate(projectRoot, label, apis, true);
                //Initializing the ballerina label project and creating .bal folder.
                InitHandler.initialize(Paths.get(GatewayCmdUtils
                        .getLabelDirectoryPath(projectRoot, label)), null, new ArrayList<>(), null);
                try {
                    boolean changesDetected = HashUtils.detectChanges(apis, subscriptionPolicies, applicationPolicies);
                    if (!changesDetected) {
                        outStream.println("No changes from upstream.");
                        Runtime.getRuntime().exit(GatewayCliConstants.EXIT_CODE_NOT_MODIFIED);
                    }
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

                Token token = new Token();
                token.setPublisherEndpoint(publisherEndpoint);
                token.setAdminEndpoint(adminEndpoint);
                token.setRegistrationEndpoint(registrationEndpoint);
                token.setTokenEndpoint(tokenEndpoint);
                token.setUsername(username);
                token.setClientId(clientID);
                String encryptedSecret = GatewayCmdUtils.encrypt(clientSecret, new String(password));
                token.setClientSecret(encryptedSecret);
                token.setTrustStoreLocation(trustStoreLocation);
                token.setTrustStorePassword(trustStorePassword);
                newConfig.setToken(token);

                newConfig.setCorsConfiguration(GatewayCmdUtils.getDefaultCorsConfig());
                GatewayCmdUtils.saveConfig(newConfig);
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

    }

    /**
     * This class represents the "build" command and it holds arguments and flags specified by the user.
     */
    @Parameters(commandNames = "build", commandDescription = "micro gateway build information")
    private static class BuildCmd implements GatewayLauncherCmd {

        @Parameter(names = "--java.debug", hidden = true)
        private String javaDebugPort;

        @Parameter(names = { "-l", "--label" }, hidden = true)
        private String label;

        @Parameter(names = { "--help", "-h", "?" }, hidden = true, description = "for more information")
        private boolean helpFlag;

        @Parameter(arity = 1)
        private List<String> argList;

        private JCommander parentCmdParser;

        public void execute() {
            if (helpFlag) {
                String commandUsageInfo = GatewayLauncherCmd.getCommandUsageInfo("build");
                outStream.println(commandUsageInfo);
                return;
            }

            if (StringUtils.isEmpty(label)) {
                outStream.println("Label can't be empty. " + "You need to specify -l <label name>");
                return;
            }

            try {
                String projectRoot = GatewayCmdUtils.getStoredWorkspaceLocation();
                GatewayCmdUtils.createLabelGWDistribution(projectRoot, label);
            } catch (IOException e) {
                outStream.println(
                        "Error while creating micro gateway distribution for " + label + ". Reason: " + e.getMessage());
                Runtime.getRuntime().exit(1);
            }
            Runtime.getRuntime().exit(0);
        }

        @Override
        public String getName() {
            return GatewayCliCommands.BUILD;
        }

        @Override
        public void setParentCmdParser(JCommander parentCmdParser) {
            this.parentCmdParser = parentCmdParser;
        }
    }

    /**
     * This class represents the "run" command and it holds arguments and flags specified by the user.
     */
    @Parameters(commandNames = "run", commandDescription = "micro gateway run information")
    private static class RunCmd implements GatewayLauncherCmd {

        @Parameter(names = "--java.debug", hidden = true)
        private String javaDebugPort;

        @Parameter(names = { "-l", "--label" }, hidden = true)
        private String label;

        @Parameter(names = { "--help", "-h", "?" }, hidden = true, description = "for more information")
        private boolean helpFlag;

        @Parameter(arity = 1)
        private List<String> argList;

        private JCommander parentCmdParser;

        public void execute() {
            if (helpFlag) {
                String commandUsageInfo = GatewayLauncherCmd.getCommandUsageInfo("run");
                outStream.println(commandUsageInfo);
                return;
            }

            if (StringUtils.isEmpty(label)) {
                outStream.println("Label can't be empty. " + "You need to specify -l <label name>");
                return;
            }
        }

        @Override
        public String getName() {
            return GatewayCliCommands.RUN;
        }

        @Override
        public void setParentCmdParser(JCommander parentCmdParser) {
            this.parentCmdParser = parentCmdParser;
        }
    }

    /**
     * This class represents the "main" command required by the JCommander.
     */
    private static class DefaultCmd implements GatewayLauncherCmd {

        @Parameter(names = { "--help", "-h", "?" }, hidden = true, description = "for more information")
        private boolean helpFlag;

        @Parameter(names = "--java.debug", hidden = true)
        private String javaDebugPort;

        @Override
        public void execute() {
            if (helpFlag) {
                printUsageInfo(GatewayCliCommands.HELP);
                return;
            }
            printUsageInfo(GatewayCliCommands.DEFAULT);
        }

        @Override
        public String getName() {
            return GatewayCliCommands.DEFAULT;
        }

        @Override
        public void setParentCmdParser(JCommander parentCmdParser) {
        }
    }
}
