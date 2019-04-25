package org.wso2.apimgt.gateway.cli.cmd;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.cli.exception.CLIRuntimeException;
import org.wso2.apimgt.gateway.cli.utils.GatewayCmdUtils;
import org.wso2.apimgt.gateway.cli.utils.OpenAPICodegenUtils;
import org.wso2.apimgt.gateway.cli.utils.RouteUtils;

import java.io.PrintStream;

@Parameters(commandNames = "add route", commandDescription = "add api to the microgateway")
public class AddRouteCmd implements GatewayLauncherCmd {
    private static final Logger logger = LoggerFactory.getLogger(AddRouteCmd.class);
    private static PrintStream outStream = System.out;

    @Parameter(names = {"--project"}, hidden = true)
    private String projectName;

    @Parameter(names = "--java.debug", hidden = true)
    private String javaDebugPort;

    @Parameter(names = {"-e", "--endpoint"}, hidden = true)
    private String endpoint;

    @Parameter(names = {"-ec", "--endpoint-config"}, hidden = true)
    private String endpointConfig;

    @Parameter(names = {"-r", "--resource"}, hidden = true)
    private String resource_id;

    @Override
    public void execute() {

        projectName = GatewayCmdUtils.buildProjectName(projectName);
        RouteUtils.setRoutesConfigPath(GatewayCmdUtils.getProjectMgwDefinitionFilePath(projectName));

        if (resource_id == null || resource_id.isEmpty()) {
            if ((resource_id = GatewayCmdUtils.promptForTextInput(outStream, "Enter Resource ID: "))
                    .trim().isEmpty()) {
                throw GatewayCmdUtils.createUsageException("Micro gateway add route failed: " +
                        "resource_id is not provided");
            }
        }
        if (OpenAPICodegenUtils.getResource(projectName, resource_id) == null) {
            throw new CLIRuntimeException("Provided resource id is not available");
        }
        String endpointConfigString;
        if (StringUtils.isEmpty(endpointConfig)) {
            if (StringUtils.isEmpty(endpoint)) {
                /*
                 * if an endpoint config or an endpoint is not provided as an argument, it is prompted from
                 * the user
                 */
                if ((endpoint = GatewayCmdUtils.promptForTextInput(outStream, "Enter Endpoint URL for Resource " +
                        resource_id + ": ")).trim().isEmpty()) {
                    throw GatewayCmdUtils.createUsageException("Micro gateway setup failed: empty endpoint.");
                }
            }
            endpointConfigString = "{\"prod\": {\"type\": \"http\", \"endpoints\" : [\"" + endpoint.trim() + "\"]}}";
        } else {
            endpointConfigString = OpenAPICodegenUtils.readJson(endpointConfig);
        }
        if (RouteUtils.hasResourceInRoutesConfig(resource_id)) {
            String UserResponse;
            if ((UserResponse = GatewayCmdUtils.promptForTextInput(outStream, "For the provided resource id " +
                    resource_id + " enpoint configuration already exist. Do you need to overwrite ? yes[y] or no[n] :"))
                    .trim().isEmpty()) {
                throw new CLIRuntimeException("No argument is provided.");
            }
            if (UserResponse.toLowerCase().equals("n") || UserResponse.toLowerCase().equals("no")) {
                outStream.println("Add route command is aborted :" + resource_id);
            } else if (UserResponse.toLowerCase().equals("y") || UserResponse.toLowerCase().equals("yes")) {
                RouteUtils.saveResourceRoute(resource_id, endpointConfigString);
                outStream.println("Successfully added route for resource ID : " + resource_id);
            } else {
                throw new CLIRuntimeException("Provided argument is not valid :" + UserResponse);
            }
        } else {
            RouteUtils.saveResourceRoute(resource_id, endpointConfigString);
            outStream.println("Successfully added route for resource ID : " + resource_id);
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
