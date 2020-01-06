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
import org.wso2.apimgt.gateway.cli.config.TOMLConfigParser;
import org.wso2.apimgt.gateway.cli.exception.CLIInternalException;
import org.wso2.apimgt.gateway.cli.exception.ConfigParserException;
import org.wso2.apimgt.gateway.cli.model.config.Config;
import org.wso2.apimgt.gateway.cli.model.config.Token;
import org.wso2.apimgt.gateway.cli.utils.CmdUtils;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This class represents the "reset" command and it holds arguments and flags specified by the user.
 */
@Parameters(commandNames = "reset", commandDescription = "reset configurations")
public class ResetCmd implements LauncherCmd {
    private static PrintStream outStream = System.err;

    @Parameter(names = {"-c", "--config"}, description = "external config file path")
    private String configPath;

    @SuppressWarnings("unused")
    @Parameter(names = "--java.debug", hidden = true)
    private String javaDebugPort;

    public void execute() {
        // Reset the configuration of the provided config path. If path is not given use the default config file
        if (StringUtils.isEmpty(configPath)) {
            configPath = CmdUtils.getMainConfigLocation();
        }

        Path configurationFile = Paths.get(configPath);
        if (!Files.exists(configurationFile)) {
            outStream.println("Config: " + configPath + " Not found.");
            Runtime.getRuntime().exit(1);
        }

        //Reset only the token related configurations and keep the rest of the configuration.
        try {
            Config newConfig = TOMLConfigParser.parse(configPath, Config.class);
            Token token = newConfig.getToken();
            token.setClientId(StringUtils.EMPTY);
            token.setClientSecret(StringUtils.EMPTY);
            token.setUsername(StringUtils.EMPTY);
            CmdUtils.saveConfig(newConfig, configPath);
        } catch (ConfigParserException e) {
            throw new CLIInternalException("Error occurred while parsing the configuration : " + configPath, e);
        }
    }

    @Override
    public String getName() {
        return CliCommands.RESET;
    }

    @Override
    public void setParentCmdParser(JCommander parentCmdParser) {
        // Nothing to implement
    }
}
