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
import com.beust.jcommander.ParameterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.cli.constants.CliConstants;
import org.wso2.apimgt.gateway.cli.exception.CLIInternalException;
import org.wso2.apimgt.gateway.cli.exception.CLIRuntimeException;
import org.wso2.apimgt.gateway.cli.exception.CliLauncherException;
import org.wso2.apimgt.gateway.cli.utils.CmdUtils;

import java.io.PrintStream;
import java.util.Map;
import java.util.Optional;

/**
 * This class executes the gateway cli program.
 */
public class Main {
    private static final String INTERNAL_ERROR_MESSAGE = "Internal error occurred while executing command.";
    private static final String MICRO_GW = "micro-gw: ";
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private static PrintStream err = System.err;
    private static PrintStream out = System.out;

    public static void main(String... args) {
        Optional<LauncherCmd> invokedCmd = Optional.empty();
        try {
            invokedCmd = getInvokedCmd(args);
            invokedCmd.ifPresent(LauncherCmd::execute);
            CmdUtils.printMessagesToConsole();
            CmdUtils.printCallHomeMessage();
            Runtime.getRuntime().exit(0);
        } catch (CliLauncherException e) {
            err.println(e.getMessages());
            Throwable cause = e.getCause();
            if (cause instanceof ParameterException) {
                ParameterException paramEx = (ParameterException) cause;
                paramEx.usage();
            }
            logger.error(MICRO_GW + "Error occurred while executing command.", e);
            Runtime.getRuntime().exit(1);
        } catch (CLIInternalException e) {
            showBuildError(invokedCmd);
            err.println(MICRO_GW + INTERNAL_ERROR_MESSAGE + " - " + e.getMessage());
            logger.error(e.getMessage(), e);
            Runtime.getRuntime().exit(1);
        } catch (CLIRuntimeException e) {
            showBuildError(invokedCmd);
            err.println(MICRO_GW + e.getTerminalMsg());
            logger.error(e.getMessage(), e);
            Runtime.getRuntime().exit(e.getExitCode());
        } catch (Exception e) {
            //Use generic exception to catch all the runtime exception
            showBuildError(invokedCmd);
            err.println(MICRO_GW + INTERNAL_ERROR_MESSAGE);
            logger.error(INTERNAL_ERROR_MESSAGE, e);
            Runtime.getRuntime().exit(1);
        }
    }

    /**
     * Get the invoke CMD from the specified arguments
     *
     * @param args list of arguments
     * @return invoked CMD
     */
    private static Optional<LauncherCmd> getInvokedCmd(String... args) {
        try {
            HelpCmd helpCmd = new HelpCmd();
            InitCmd initCmd = new InitCmd();
            BuildCmd buildCmd = new BuildCmd();
            ResetCmd resetCmd = new ResetCmd();
            ImportCmd importCmd = new ImportCmd();
            VersionCmd versionCmd = new VersionCmd();

            JCommander cmdParser = JCommander.newBuilder()
                    .addCommand(CliCommands.HELP, helpCmd)
                    .addCommand(CliCommands.INIT, initCmd)
                    .addCommand(CliCommands.BUILD, buildCmd)
                    .addCommand(CliCommands.RESET, resetCmd)
                    .addCommand(CliCommands.IMPORT, importCmd)
                    .addCommand(CliCommands.VERSION, versionCmd)
                    .build();
            cmdParser.setProgramName(CliConstants.MICRO_GW);

            helpCmd.setParentCmdParser(cmdParser);
            initCmd.setParentCmdParser(cmdParser);
            buildCmd.setParentCmdParser(cmdParser);
            resetCmd.setParentCmdParser(cmdParser);
            importCmd.setParentCmdParser(cmdParser);
            versionCmd.setParentCmdParser(cmdParser);

            cmdParser.parse(args);
            Map<String, JCommander> commanderMap;
            String parsedCmdName = cmdParser.getParsedCommand();

            // User has not specified a command. print usage
            if (parsedCmdName == null) {
                ParameterException paramEx = new ParameterException("Command not specified");
                paramEx.setJCommander(cmdParser);
                throw paramEx;
            }
            commanderMap = cmdParser.getCommands();
            return Optional.of((LauncherCmd) commanderMap.get(parsedCmdName).getObjects().get(0));
        } catch (MissingCommandException e) {
            String errorMsg = "Unknown command '" + e.getUnknownCommand() + "'";
            throw CmdUtils.createUsageException(errorMsg);
        } catch (ParameterException e) {
            String msg = e.getMessage();
            CliLauncherException cliEx = new CliLauncherException(e);
            if (msg == null) {
                cliEx.addMessage("Internal error occurred");
            } else {
                cliEx.addMessage(msg);
            }

            throw cliEx;
        }
    }

    private static void showBuildError(Optional<LauncherCmd> invokedCmd) {
        invokedCmd.ifPresent(value -> {
            if (CliCommands.BUILD.equalsIgnoreCase(value.getName())) {
                out.print(CmdUtils.format("[ERROR]\n"));
            }
        });
    }
}
