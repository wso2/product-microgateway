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

@Parameters(commandNames = "update route", commandDescription = "update routes configuration")
public class UpdateRouteCmd implements GatewayLauncherCmd{

    private static final Logger logger = LoggerFactory.getLogger(ListAPIsCmd.class);
    private static PrintStream outStream = System.out;

    @Parameter(names = {"-r", "--resource"}, hidden = true)
    private String resourceId;

    @Parameter(names = {"-a", "--api"}, hidden = true)
    private String apiId;

    @Parameter(names = {"--project"}, hidden = true, required = true)
    private String projectName;

    @Parameter(names = {"-e", "--endpoint"}, hidden = true)
    private String endpoint;

    @Parameter(names = {"-ec", "--endpoint-config"}, hidden = true)
    private String endpointConfig;

    @Override
    public void execute() {
        if(projectName == null || projectName.isEmpty()){
            throw new CLIRuntimeException("Project name is not provided.");
        }
        RouteUtils.setRoutesConfigPath(GatewayCmdUtils.getProjectRoutesConfFilePath(projectName));

        if((apiId == null || apiId.isEmpty()) && (resourceId == null || resourceId.isEmpty())){
            throw new CLIRuntimeException("Error: API Id or resource id is not provided.");
        }

        if((apiId != null ) && (resourceId != null)){
            throw new CLIRuntimeException("Error: Please provide one Id.");
        }

        String endpointConfigString;
        if (StringUtils.isEmpty(endpointConfig)) {
            if (StringUtils.isEmpty(endpoint)) {
                /*
                 * if an endpoint config or an endpoint is not provided as an argument, it is prompted from
                 * the user
                 */
                if ((endpoint = GatewayCmdUtils.promptForTextInput(outStream, "Enter Endpoint URL" + ": "))
                        .trim().isEmpty()) {
                    throw GatewayCmdUtils.createUsageException("Micro gateway setup failed: empty endpoint.");
                }
            }
            endpointConfigString = "{\"prod\": {\"type\": \"http\", \"endpoints\" : [\"" + endpoint.trim() + "\"]}}";
        } else {
            endpointConfigString = OpenAPICodegenUtils.readJson(endpointConfig);
        }

        if(apiId != null){
            RouteUtils.updateAPIRoute(apiId, endpointConfigString);
        } else{
            RouteUtils.updateResourceRoute(resourceId, endpointConfigString);
        }
        outStream.println("Update route command executed successfully.");
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void setParentCmdParser(JCommander parentCmdParser) {

    }
}
