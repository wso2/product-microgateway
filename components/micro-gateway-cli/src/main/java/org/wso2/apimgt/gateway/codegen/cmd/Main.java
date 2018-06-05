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

package org.wso2.apimgt.gateway.codegen.cmd;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.MissingCommandException;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import org.apache.commons.lang3.StringUtils;
import org.ballerinalang.packerina.init.InitHandler;
import org.ballerinalang.packerina.init.models.SrcFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.codegen.CodeGenerator;
import org.wso2.apimgt.gateway.codegen.config.ConfigYAMLParser;
import org.wso2.apimgt.gateway.codegen.config.bean.Config;
import org.wso2.apimgt.gateway.codegen.exception.BallerinaServiceGenException;
import org.wso2.apimgt.gateway.codegen.exception.CliLauncherException;
import org.wso2.apimgt.gateway.codegen.exception.ConfigParserException;
import org.wso2.apimgt.gateway.codegen.service.APIService;
import org.wso2.apimgt.gateway.codegen.service.APIServiceImpl;
import org.wso2.apimgt.gateway.codegen.service.bean.ext.ExtendedAPI;
import org.wso2.apimgt.gateway.codegen.token.TokenManagement;
import org.wso2.apimgt.gateway.codegen.token.TokenManagementImpl;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
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

    private static PrintStream outStream = System.err;

    private static final Logger cliLog = LoggerFactory.getLogger(Main.class);

    public static void main(String... args) {
        try {
            String tempRoot = "/home/harsha/Downloads/myroot";
            GatewayCmdUtils.createTempDir(tempRoot);
            String root = GatewayCmdUtils.getProjectRoot(tempRoot);
            String configPath = GatewayCmdUtils.getMainConfigPath(root) + File.separator +
                                                                            GatewayCliConstants.MAIN_CONFIG_FILE_NAME;
            Config config = ConfigYAMLParser.parse(configPath, Config.class);
            System.setProperty("javax.net.ssl.keyStoreType", "pkcs12");
            System.setProperty("javax.net.ssl.trustStore", config.getTokenConfig().getTrustoreLocation());
            System.setProperty("javax.net.ssl.trustStorePassword", config.getTokenConfig().getTrustorePassword());
            GatewayCmdUtils.setConfig(config);
            Optional<GatewayLauncherCmd> optionalInvokedCmd = getInvokedCmd(args);
            optionalInvokedCmd.ifPresent(GatewayLauncherCmd::execute);
        } catch (CliLauncherException e) {
            outStream.println(e.getMessages());
            Runtime.getRuntime().exit(1);
        } catch (ConfigParserException e) {
            outStream.println("Error while parsing the config");
            Runtime.getRuntime().exit(1);
        } catch (IOException e) {
            outStream.println("Error while processing files");
            Runtime.getRuntime().exit(1);
        }
    }

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

            cmdParser.setProgramName("micro-gw");
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
            String errorMsg = "unknown command '" + e.getUnknownCommand() + "'";
            throw GatewayCmdUtils.createUsageException(errorMsg);

        } catch (ParameterException e) {
            String msg = e.getMessage();
            if (msg == null) {
                throw GatewayCmdUtils.createUsageException("internal error occurred");

            } else if (msg.startsWith(JC_UNKNOWN_OPTION_PREFIX)) {
                String flag = msg.substring(JC_UNKNOWN_OPTION_PREFIX.length());
                throw GatewayCmdUtils.createUsageException("unknown flag '" + flag.trim() + "'");

            } else if (msg.startsWith(JC_EXPECTED_A_VALUE_AFTER_PARAMETER_PREFIX)) {
                String flag = msg.substring(JC_EXPECTED_A_VALUE_AFTER_PARAMETER_PREFIX.length());
                throw GatewayCmdUtils.createUsageException("flag '" + flag.trim() + "' needs an argument");

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

        @Parameter(names = {"-u", "--user"}, hidden = true)
        private String username;

        @Parameter(names = {"-p", "--password"}, hidden = true, password = true)
        private String password;

        @Parameter(names = {"-l", "--label"}, hidden = true)
        private String label;

        @Parameter(names = {"-o", "--overwrite"}, hidden = true)
        private boolean overwrite;

        @Parameter(names = {"--path"}, hidden = true)
        private String path;

        public void execute() {
            /* temporary hardcoded */
            username = "admin";
            password = "admin";
            /* ******************* */
            
            Config config = GatewayCmdUtils.getConfig();
            String configuredUser = config.getTokenConfig().getUsername();

            if (StringUtils.isEmpty(configuredUser) && StringUtils.isEmpty(username)) {
                if ((username = promptForTextInput("Enter Username: ")).trim().isEmpty()) {
                    if (username.trim().isEmpty()) {
                        username = promptForTextInput("Username can't be empty; enter username: ");
                        if (username.trim().isEmpty()) {
                            throw GatewayCmdUtils.createUsageException("Micro gateway setup failed: empty username.");
                        }
                    }
                }
            }

            if (StringUtils.isEmpty(password)) {
                if ((password = promptForPasswordInput("Enter Password: ")).trim().isEmpty()) {
                    if (StringUtils.isEmpty(password)) {
                        password = promptForPasswordInput("Password can't be empty; enter password: ");
                        if (password.trim().isEmpty()) {
                            throw GatewayCmdUtils.createUsageException("Micro gateway setup failed: empty password.");
                        }
                    }
                }
            }
            String root = path;
            try {
                root = GatewayCmdUtils.getProjectRoot(path);
                GatewayCmdUtils.createTempDir(path);
                GatewayCmdUtils.createTempPathTxt(path, path);
                GatewayCmdUtils.createMainProjectStructure(root);
                GatewayCmdUtils.createLabelProjectStructure(root, label); 
            } catch (IOException e) {
                outStream.println("Error while creating project structure");
                e.printStackTrace();
                Runtime.getRuntime().exit(1);
            }

            TokenManagement manager = new TokenManagementImpl();
            String clientId = config.getTokenConfig().getClientId();
            if (StringUtils.isEmpty(clientId)) {
                manager.generateClientIdAndSecret(config, root);
                clientId = config.getTokenConfig().getClientId();
            }

            String clientSecret = config.getTokenConfig().getClientSecret();
            manager.generateClientIdAndSecret(config, root);
            String accessToken = manager.generateAccessToken(username, password.toCharArray(),
                                                                                    clientId, clientSecret.toCharArray());

            APIService service = new APIServiceImpl();
            List<ExtendedAPI> apis = service.getAPIs(label, accessToken);

            CodeGenerator codeGenerator = new CodeGenerator();
            try {
                codeGenerator.generate(GatewayCmdUtils
                                .getLabelSrcDirectoryPath(root, label),
                        apis, true);
                InitHandler.initialize(Paths.get(GatewayCmdUtils
                        .getLabelDirectoryPath(root, label)), null, new ArrayList<SrcFile>(), null);
            } catch (IOException | BallerinaServiceGenException e) {
                outStream.println("Error while generating ballerina source");
                e.printStackTrace();
                Runtime.getRuntime().exit(1);
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
    }

    /**
     * This class represents the "build" command and it holds arguments and flags specified by the user.
     */
    @Parameters(commandNames = "build", commandDescription = "micro gateway build information")
    private static class BuildCmd implements GatewayLauncherCmd {

        @Parameter(names = "--java.debug", hidden = true)
        private String javaDebugPort;

        @Parameter(names = {"-l", "--label"}, hidden = true)
        private String label;

        @Parameter(names = {"--help", "-h", "?"}, hidden = true, description = "for more information")
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

        @Parameter(names = {"-l", "--label"}, hidden = true)
        private String label;

        @Parameter(names = {"--help", "-h", "?"}, hidden = true, description = "for more information")
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

        @Parameter(names = {"--help", "-h", "?"}, hidden = true, description = "for more information")
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
