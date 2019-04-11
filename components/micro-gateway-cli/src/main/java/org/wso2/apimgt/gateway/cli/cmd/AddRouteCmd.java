package org.wso2.apimgt.gateway.cli.cmd;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.cli.utils.GatewayCmdUtils;
import org.wso2.apimgt.gateway.cli.utils.RouteUtils;
import org.wso2.apimgt.gateway.cli.utils.OpenAPICodegenUtils;

import java.io.PrintStream;
import java.util.List;

@Parameters(commandNames = "add-route", commandDescription = "add api/route to the microgateway")
public class AddRouteCmd implements GatewayLauncherCmd{
    private static final Logger LOGGER = LoggerFactory.getLogger(AddAPICmd.class);
    private static PrintStream OUT_STREAM = System.out;

    @Parameter(hidden = true, required = true)
    private List<String> mainArgs;

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
        String projectName = GatewayCmdUtils.getProjectName(mainArgs);
        RouteUtils.setRoutesConfigPath(GatewayCmdUtils.getProjectRoutesConfFilePath(projectName));

        if (resource_id.isEmpty()) {
            if ((resource_id = GatewayCmdUtils.promptForTextInput(OUT_STREAM,"Enter Resource ID: "))
                    .trim().isEmpty()) {
                throw GatewayCmdUtils.createUsageException("Micro gateway add route failed: " +
                        "resource_id is not provided");
            }
        }
        String endpointConfigString;
        if (StringUtils.isEmpty(endpointConfig)) {
            if (StringUtils.isEmpty(endpoint)) {
                /*
                 * if an endpoint config or an endpoint is not provided as an argument, it is prompted from
                 * the user
                 */
                if ((endpoint = GatewayCmdUtils.promptForTextInput(OUT_STREAM, "Enter Endpoint URL for Resource " + resource_id + ": "))
                        .trim().isEmpty()) {
                    throw GatewayCmdUtils.createUsageException("Micro gateway setup failed: empty endpoint.");
                }
            }
            endpointConfigString = "{\"prod\": {\"type\": \"http\", \"endpoints\" : [\"" + endpoint.trim() + "\"]}}";
        } else {
            endpointConfigString = OpenAPICodegenUtils.readApi(endpointConfig);
        }
        RouteUtils.saveResourceRoute(resource_id, endpointConfigString,
                GatewayCmdUtils.getProjectRoutesConfFilePath(projectName));
        OUT_STREAM.println("Successfully added route for resource ID : " + resource_id);
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void setParentCmdParser(JCommander parentCmdParser) {

    }
}
