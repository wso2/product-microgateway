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
import org.wso2.apimgt.gateway.cli.model.config.Client;
import org.wso2.apimgt.gateway.cli.model.config.Config;
import org.wso2.apimgt.gateway.cli.model.config.Token;
import org.wso2.apimgt.gateway.cli.model.config.TokenBuilder;
import org.wso2.apimgt.gateway.cli.utils.GatewayCmdUtils;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This class represents the "reset" command and it holds arguments and flags specified by the user.
 */
@Parameters(commandNames = "reset", commandDescription = "reset configurations")
public class ResetCmd implements GatewayLauncherCmd {
    private static PrintStream outStream = System.err;
    @SuppressWarnings("unused")
    @Parameter(names = "--java.debug", hidden = true)
    private String javaDebugPort;

    @Parameter(names = { "-c", "--config" }, hidden = true)
    private String configPath;

    public void execute() {
        // Reset the configuration of the provided config path. If path is not given use the default config file
        if (StringUtils.isEmpty(configPath)) {
            configPath = GatewayCmdUtils.getMainConfigLocation();
        }

        Path configurationFile = Paths.get(configPath);
        if (!Files.exists(configurationFile)) {
            outStream.println("Config: " + configPath + " Not found.");
            Runtime.getRuntime().exit(1);
        }

        //Write empty config to config
        Config newConfig = new Config();
        Client client = new Client();
        client.setHttpRequestTimeout(1000000);
        newConfig.setClient(client);
        Token token = new TokenBuilder().setBaseURL(StringUtils.EMPTY).setRestVersion(StringUtils.EMPTY).setPublisherEndpoint(StringUtils.EMPTY).setAdminEndpoint(StringUtils.EMPTY)
                .setRegistrationEndpoint(StringUtils.EMPTY).setTokenEndpoint(StringUtils.EMPTY)
                .setUsername(StringUtils.EMPTY).setClientId(StringUtils.EMPTY).setClientSecret(StringUtils.EMPTY)
                .setTrustStoreLocation(StringUtils.EMPTY).setTrustStorePassword(StringUtils.EMPTY).build();
        newConfig.setToken(token);
        newConfig.setCorsConfiguration(GatewayCmdUtils.getDefaultCorsConfig());
        GatewayCmdUtils.saveConfig(newConfig, configPath);
    }

    @Override
    public String getName() {
        return GatewayCliCommands.RESET;
    }

    @Override
    public void setParentCmdParser(JCommander parentCmdParser) {
        // Nothing to implement
    }
}
