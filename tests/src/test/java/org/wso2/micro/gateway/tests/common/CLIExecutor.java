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
package org.wso2.micro.gateway.tests.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.cli.constants.GatewayCliConstants;
import org.wso2.micro.gateway.tests.context.Constants;
import org.wso2.micro.gateway.tests.context.ServerLogReader;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Execute APIM CLI functions
 */
public class CLIExecutor {
    private static final Logger log = LoggerFactory.getLogger(CLIExecutor.class);
    private String homeDirectory;
    private String cliHome;
    private static CLIExecutor instance;

    public void generate(String label, String project, String security) throws Exception {
        org.wso2.apimgt.gateway.cli.cmd.Main main = new org.wso2.apimgt.gateway.cli.cmd.Main();

        String baseDir = (System.getProperty(Constants.SYSTEM_PROP_BASE_DIR, ".")) + File.separator + "target";
        Path path = Files.createTempDirectory(new File(baseDir).toPath(), "userProject", new FileAttribute[0]);
        log.info("CLI Project Home: " + path.toString());

        System.setProperty(GatewayCliConstants.CLI_HOME, this.cliHome);
        log.info("CLI Home: " + this.cliHome);

        String config = new File(
                getClass().getClassLoader().getResource("confs" + File.separator + "default-cli-test-config.toml")
                        .getPath()).getAbsolutePath();
        System.setProperty("user.dir", path.toString());
        String[] args = {"setup", project, "--label", label, "--username", "admin", "--password",
                "admin", "--server-url", "http://localhost:9443", "--truststore",
                "lib/platform/bre/security/ballerinaTruststore.p12", "--truststore-pass", "ballerina", "--config",
                config, "--security", security};
        main.main(args);

        String balCommand = this.cliHome + File.separator + GatewayCliConstants.CLI_LIB + File.separator + "platform"
                + File.separator + GatewayCliConstants.GW_DIST_BIN + File.separator + "ballerina";
        homeDirectory = path + File.separator + project;

        String[] cmdArray = new String[]{"bash", balCommand, "build"};
        String[] args2 = new String[]{"src", "-o", project,"--experimental"};
        String[] cmdArgs = Stream.concat(Arrays.stream(cmdArray), Arrays.stream(args2)).toArray(String[]::new);
        Process process = Runtime.getRuntime().exec(cmdArgs, null, new File(homeDirectory));

        new ServerLogReader("errorStream", process.getErrorStream()).start();
        new ServerLogReader("inputStream", process.getInputStream()).start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Error occurred when building.");
        }
    }

    public void generateFromDefinition(String label, String project, String endpoint, String security) throws Exception {
        org.wso2.apimgt.gateway.cli.cmd.Main main = new org.wso2.apimgt.gateway.cli.cmd.Main();

        String baseDir = (System.getProperty(Constants.SYSTEM_PROP_BASE_DIR, ".")) + File.separator + "target";
        Path path = Files.createTempDirectory(new File(baseDir).toPath(), "userProject", new FileAttribute[0]);
        log.info("CLI Project Home: " + path.toString());

        System.setProperty(GatewayCliConstants.CLI_HOME, this.cliHome);
        log.info("CLI Home: " + this.cliHome);

        String config = new File(
                getClass().getClassLoader().getResource("confs" + File.separator + "default-cli-test-config.toml")
                        .getPath()).getAbsolutePath();
        String oasFilePath = new File(
                getClass().getClassLoader().getResource("testapi.json")
                        .getPath()).getAbsolutePath();
        System.setProperty("user.dir", path.toString());
        String[] args = {"setup", project, "--label", label,
                "-oa", oasFilePath, "-e", endpoint, "--config", config, "--security", security};
        main.main(args);

        String balCommand = this.cliHome + File.separator + GatewayCliConstants.CLI_LIB + File.separator + "platform"
                + File.separator + GatewayCliConstants.GW_DIST_BIN + File.separator + "ballerina";
        homeDirectory = path + File.separator + project;

        String[] cmdArray = new String[]{"bash", balCommand, "build"};
        String[] args2 = new String[]{"src", "-o", project, "--experimental"};
        String[] cmdArgs = Stream.concat(Arrays.stream(cmdArray), Arrays.stream(args2)).toArray(String[]::new);
        Process process = Runtime.getRuntime().exec(cmdArgs, null, new File(homeDirectory));

        new ServerLogReader("errorStream", process.getErrorStream()).start();
        new ServerLogReader("inputStream", process.getInputStream()).start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Error occurred when building.");
        }
    }

    public void generatePassingFlag(String label, String project, String additionalFlag) throws Exception {
        org.wso2.apimgt.gateway.cli.cmd.Main main = new org.wso2.apimgt.gateway.cli.cmd.Main();

        String baseDir = (System.getProperty(Constants.SYSTEM_PROP_BASE_DIR, ".")) + File.separator + "target";
        Path path = Files.createTempDirectory(new File(baseDir).toPath(), "userProject", new FileAttribute[0]);
        log.info("CLI Project Home: " + path.toString());

        System.setProperty(GatewayCliConstants.CLI_HOME, this.cliHome);
        log.info("CLI Home: " + this.cliHome);

        String config = new File(
                getClass().getClassLoader().getResource("confs" + File.separator + "default-cli-test-config.toml")
                        .getPath()).getAbsolutePath();
        System.setProperty("user.dir", path.toString());
        String[] args = { "setup", project, "--label", label, "--username", "admin", "--password",
                "admin", "--server-url", "http://localhost:9443", "--truststore",
                "lib/platform/bre/security/ballerinaTruststore.p12", "--truststore-pass", "ballerina", "--config",
                config, "--security", "oauth2", additionalFlag };
        main.main(args);

        String balCommand = this.cliHome + File.separator + GatewayCliConstants.CLI_LIB + File.separator + "platform"
                + File.separator + GatewayCliConstants.GW_DIST_BIN + File.separator + "ballerina";
        homeDirectory = path + File.separator + project;

        String[] cmdArray = new String[] { "bash", balCommand, "build" };
        String[] args2 = new String[] { "src", "-o", project, "--experimental" };
        String[] cmdArgs = Stream.concat(Arrays.stream(cmdArray), Arrays.stream(args2)).toArray(String[]::new);
        Process process = Runtime.getRuntime().exec(cmdArgs, null, new File(homeDirectory));

        new ServerLogReader("errorStream", process.getErrorStream()).start();
        new ServerLogReader("inputStream", process.getInputStream()).start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Error occurred when building.");
        }
    }

    public String getHomeDirectory() {
        return homeDirectory;
    }

    public void setHomeDirectory(String homeDirectory) {
        this.homeDirectory = homeDirectory;
    }

    private CLIExecutor() {
    }

    public static CLIExecutor getInstance() {
        if (instance == null) {
            instance = new CLIExecutor();
        }
        return instance;
    }

    public String getCliHome() {
        return cliHome;
    }

    public void setCliHome(String cliHome) {
        this.cliHome = cliHome;
    }

    public String getLabelBalx(String project) {
        return homeDirectory + File.separator + "target" + File.separator + project + ".balx";
    }
}