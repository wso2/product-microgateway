package org.wso2.apimgt.gateway.cli.cmd;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.cli.exception.CLIRuntimeException;
import org.wso2.apimgt.gateway.cli.model.route.ResourceRepresentation;
import org.wso2.apimgt.gateway.cli.utils.GatewayCmdUtils;
import org.wso2.apimgt.gateway.cli.utils.RouteUtils;
import org.wso2.apimgt.gateway.cli.utils.OpenAPICodegenUtils;

import java.io.PrintStream;
import java.util.List;

@Parameters(commandNames = "list resources", commandDescription = "list routes of the microgateway")
public class ListResourcesCmd implements GatewayLauncherCmd{
    private static final Logger LOGGER = LoggerFactory.getLogger(AddAPICmd.class);
    private static PrintStream outStream = System.out;

    @Parameter(names = {"--project"}, hidden = true, required = true)
    private String projectName;

    @Parameter(hidden = true)
    private List<String> mainArgs;

    @Override
    public void execute() {
        if(projectName == null || projectName.isEmpty()){
            throw new CLIRuntimeException("Project name is not provided.");
        }
        RouteUtils.setRoutesConfigPath(GatewayCmdUtils.getProjectRoutesConfFilePath(projectName));
        if(mainArgs == null){
            printResourceDetailsForSingleAPI(OpenAPICodegenUtils.getAllResources(projectName));
        }
        else{
            String apiId = GatewayCmdUtils.getSingleArgument(mainArgs);
            printResourceDetailsForSingleAPI(OpenAPICodegenUtils.listResourcesFromSwaggerForAPI(projectName, apiId));
        }
    }

    private void printResourceDetailsForSingleAPI(List<ResourceRepresentation> resources) {
        if (resources == null || resources.size() == 0) {
            outStream.println("\nNo resources available in the project");
        } else {
            String tableStructure = "%-33s%-60s%-10s%-30s%-10s\n";
            outStream.format(tableStructure, "resourceId", "resource", "method", "api", "version");

            for (ResourceRepresentation resource : resources) {
                outStream.format(tableStructure, resource.getId(), resource.getName(), resource.getMethod(),
                        resource.getApi(), resource.getVersion());
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
