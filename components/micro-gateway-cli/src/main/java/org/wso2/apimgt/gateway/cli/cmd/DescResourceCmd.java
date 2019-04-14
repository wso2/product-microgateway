package org.wso2.apimgt.gateway.cli.cmd;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.cli.exception.CLIRuntimeException;
import org.wso2.apimgt.gateway.cli.model.route.ResourceRepresentation;
import org.wso2.apimgt.gateway.cli.utils.GatewayCmdUtils;
import org.wso2.apimgt.gateway.cli.utils.OpenAPICodegenUtils;
import org.wso2.apimgt.gateway.cli.utils.RouteUtils;

import java.io.PrintStream;
import java.util.List;

@Parameters(commandNames = "desc resource", commandDescription = "describe the given resource in the microgateway")
public class DescResourceCmd implements GatewayLauncherCmd {
    private static final Logger logger = LoggerFactory.getLogger(AddAPICmd.class);
    private static PrintStream outStream = System.out;

    @Parameter(hidden = true, required = true)
    private List<String> mainArgs;

    @Parameter(names = "--java.debug", hidden = true)
    private String javaDebugPort;

    @Parameter(names = {"-p", "--project"}, hidden = true)
    private String projectName;

    @Override
    public void execute() {

        if(projectName == null || projectName.isEmpty()){
            throw new CLIRuntimeException("Project name is not provided.");
        }

        RouteUtils.setRoutesConfigPath(GatewayCmdUtils.getProjectRoutesConfFilePath(projectName));

        String resource_id = GatewayCmdUtils.getSingleArgument(mainArgs);
        ResourceRepresentation resource = OpenAPICodegenUtils.getResource(projectName, resource_id);
        if(resource != null){
            printResourceDetails(resource);
        } else{
            outStream.println("No resource available for the ID : " + resource_id);
        }
    }

    private void printResourceDetails(ResourceRepresentation resource){
        outStream.println("Resource_id : " + resource.getId());
        outStream.println("API Name : " + resource.getApi());
        outStream.println("API version : " + resource.getId());
        outStream.println("Resource : " + resource.getName());
        outStream.println("Operation: " + resource.getMethod());

        outStream.println("\nEndpointConfiguration: ");
        outStream.println(RouteUtils.getResourceAsYaml(resource.getId()));
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void setParentCmdParser(JCommander parentCmdParser) {

    }
}
