package org.wso2.apimgt.gateway.cli.cmd;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.cli.utils.GatewayCmdUtils;
import org.wso2.apimgt.gateway.cli.utils.RouteUtils;
import org.wso2.apimgt.gateway.cli.utils.OpenAPICodegenUtils;

import java.io.PrintStream;
import java.util.List;

@Parameters(commandNames = "list-routes", commandDescription = "list routes of the microgateway")
public class ListRoutesCmd implements GatewayLauncherCmd{

    @Parameter(hidden = true, required = true)
    private List<String> mainArgs;

    @Parameter(names = {"-a", "--api"}, hidden = true)
    private String apiId;

    private static final Logger LOGGER = LoggerFactory.getLogger(AddAPICmd.class);
    private static PrintStream OUT_STREAM = System.out;


    @Override
    public void execute() {
        String projectName = GatewayCmdUtils.getProjectName(mainArgs);
        RouteUtils.setRoutesConfigPath(GatewayCmdUtils.getProjectRoutesConfFilePath(projectName));
        if (apiId.isEmpty()) {
            throw new RuntimeException("API Id is not provided by the user");
        }
        printResourceDetails(OpenAPICodegenUtils
                .listResourcesFromSwagger(GatewayCmdUtils.getProjectSwaggerFilePath(projectName, apiId)));
    }

    private void printResourceDetails(List<String[]> rows) {
        if (rows == null || rows.size() == 0) {
            OUT_STREAM.println("\nNo resources available in the project");
        } else {
            String tableStructure = "%-33s%-60s%-10s\n";
            OUT_STREAM.format(tableStructure, "resourceId", "resource", "method");

            for (String[] row : rows) {
                OUT_STREAM.format(tableStructure, row[0], row[1], row[2]);
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
