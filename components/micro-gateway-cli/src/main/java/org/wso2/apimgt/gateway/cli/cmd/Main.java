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
import org.wso2.apimgt.gateway.cli.exception.CLIInternalException;
import org.wso2.apimgt.gateway.cli.exception.CLIRuntimeException;
import org.wso2.apimgt.gateway.cli.exception.CliLauncherException;
import org.wso2.apimgt.gateway.cli.utils.GatewayCmdUtils;

import java.io.PrintStream;
import java.util.Map;
import java.util.Optional;

/**
 * This class executes the gateway cli program.
 */
public class Main {
    private static final String JC_UNKNOWN_OPTION_PREFIX = "Unknown option:";
    private static final String JC_EXPECTED_A_VALUE_AFTER_PARAMETER_PREFIX = "Expected a value after parameter";
    private static final String INTERNAL_ERROR_MESSAGE = "Internal error occurred while executing command.";
    private static final String MICRO_GW = "micro-gw";

    private static PrintStream outStream = System.err;

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String... args) {
        try {
            Optional<GatewayLauncherCmd> optionalInvokedCmd = getInvokedCmd(args);
            optionalInvokedCmd.ifPresent(GatewayLauncherCmd::execute);
        } catch (CliLauncherException e) {
            outStream.println(e.getMessages());
            logger.error("micro-gw: Error occurred while executing command.", e);
            Runtime.getRuntime().exit(1);
        } catch (CLIInternalException e) {
            outStream.println("micro-gw: " + INTERNAL_ERROR_MESSAGE);
            logger.error(e.getMessage(), e);
            Runtime.getRuntime().exit(1);
        } catch (CLIRuntimeException e) {
            outStream.println("micro-gw: " + e.getTerminalMsg());
            logger.error(e.getMessage(), e);
            Runtime.getRuntime().exit(e.getExitCode());
        } catch (Exception e) {
            //Use generic exception to catch all the runtime exception
            outStream.println("micro-gw: " + INTERNAL_ERROR_MESSAGE);
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
    private static Optional<GatewayLauncherCmd> getInvokedCmd(String... args) {
        try {
            DefaultCmd defaultCmd = new DefaultCmd();
            JCommander cmdParser = new JCommander(defaultCmd);
            defaultCmd.setParentCmdParser(cmdParser);

            HelpCmd helpCmd = new HelpCmd();
            cmdParser.addCommand(GatewayCliCommands.HELP, helpCmd);
            helpCmd.setParentCmdParser(cmdParser);

            SetupCmd setupCmd = new SetupCmd();
            setupCmd.setArgsForAddCmd(args);
            cmdParser.addCommand(GatewayCliCommands.SETUP, setupCmd);
            setupCmd.setParentCmdParser(cmdParser);

            BuildCmd buildCmd = new BuildCmd();
            cmdParser.addCommand(GatewayCliCommands.BUILD, buildCmd);
            buildCmd.setParentCmdParser(cmdParser);

            RunCmd runCmd = new RunCmd();
            cmdParser.addCommand(GatewayCliCommands.RUN, runCmd);
            runCmd.setParentCmdParser(cmdParser);

            ResetCmd resetCmd = new ResetCmd();
            cmdParser.addCommand(GatewayCliCommands.RESET, resetCmd);
            resetCmd.setParentCmdParser(cmdParser);

            AddCmd addCmd = new AddCmd();
            cmdParser.addCommand(GatewayCliCommands.ADD, addCmd);
            addCmd.setParentCmdParser(cmdParser);

            FunctionCmd functionCmd = new FunctionCmd();
            cmdParser.addCommand(GatewayCliCommands.FUNCTION,functionCmd);
            functionCmd.setParentCmdParser(cmdParser);

            ListCmd listCmd = new ListCmd();
            cmdParser.addCommand(GatewayCliCommands.LIST, listCmd);
            listCmd.setParentCmdParser(cmdParser);

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


}
