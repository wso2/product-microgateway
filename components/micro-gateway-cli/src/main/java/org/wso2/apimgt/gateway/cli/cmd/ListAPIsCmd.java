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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.cli.utils.GatewayCmdUtils;
import org.wso2.apimgt.gateway.cli.utils.RouteUtils;

import java.io.PrintStream;
import java.util.List;

@Parameters(commandNames = "list apis", commandDescription = "list apis of the microgateway")
public class ListAPIsCmd implements GatewayLauncherCmd {
    private static final String API_ID = "API ID";
    private static final String API_NAME = "Title";
    private static final String API_VERSION = "Version";
    private static final String BASEPATH = "Base Path";

    @Parameter(names = {"--project"}, hidden = true)
    private String projectName;

    private static final Logger logger = LoggerFactory.getLogger(ListAPIsCmd.class);
    private static PrintStream outStream = System.out;

    @Override
    public void execute() {
        projectName = GatewayCmdUtils.buildProjectName(projectName);
        RouteUtils.setRoutesConfigPath(GatewayCmdUtils.getProjectRoutesConfFilePath(projectName));
        printAPIDetailsInTable(RouteUtils.listApis());
    }

    //to present the results in a table structure
    private void printAPIDetailsInTable(List<String[]> rows) {
        //todo: introduce constants
        if (rows == null || rows.size() == 0) {
            outStream.println("\nNo APIs in the project");
        } else {
            String tableStructure = "%-33s%-20s%-10s%-80s\n";
            outStream.format(tableStructure, API_ID, API_NAME, API_VERSION, BASEPATH);

            for (String[] row : rows) {
                outStream.format(tableStructure, row[0], row[1], row[2], row[3]);
            }
        }
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void setParentCmdParser(JCommander parentCmdParser) {

    }
}
