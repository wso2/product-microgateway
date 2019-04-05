package org.wso2.apimgt.gateway.cli.cmd;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.cli.exception.CLIRuntimeException;
import org.wso2.apimgt.gateway.cli.utils.GatewayCmdUtils;
import org.wso2.apimgt.gateway.cli.utils.RouteUtils;

import java.io.File;
import java.io.PrintStream;
import java.util.List;

@Parameters(commandNames = "function", commandDescription = "add function")
public class FunctionCmd implements GatewayLauncherCmd {

    private static final Logger logger = LoggerFactory.getLogger(FunctionCmd.class);
    private static PrintStream outStream = System.out;

    @Parameter(hidden = true, required = true)
    private List<String> mainArgs;

    @Parameter(names = {"-add", "--add function"}, hidden = true)
    private Boolean addFunction =false;

    @Parameter(names = {"-update", "--update function"}, hidden = true)
    private Boolean updateFunction = false;

    @Parameter(names = {"-l", "--fileName"}, hidden = true)
    private String fileLocation;

    @Parameter(names = {"-ai", "--api id"}, hidden = true)
    private String apiID;

    @Parameter(names = {"-ri", "--resource id"}, hidden = true)
    private String resourceID;

    @Parameter(names = {"-in", "--function in"}, hidden = true)
    private String inFunction;

    @Parameter(names = {"-out", "--function out"}, hidden = true)
    private String outFunction;

    @Override
    public void execute() {


        String projectName = GatewayCmdUtils.getProjectName(mainArgs);
        File projectLocation = new File(GatewayCmdUtils.getProjectDirectoryPath(projectName) + "/src");

        if (!projectLocation.exists()) {
            throw new CLIRuntimeException("Project " + projectName + " does not exist.");
        }
        //copy .bal file to the project directory
        try {
            File source = new File(fileLocation);
            FileUtils.copyFileToDirectory(source, projectLocation);
            outStream.println("file copied");
        } catch (Exception e) {
            logger.error("error occured while copying file:" + e);
        }

        if (addFunction || updateFunction) {


            if(inFunction != null){
                if(apiID != null){
                    //api level inFunction
                    RouteUtils.addFunction(inFunction,"in",apiID,GatewayCmdUtils.getProjectRoutesConfFilePath(projectName),projectName); }
                else if(resourceID != null){
                    //add resource level inFunction
                }
                else{
                //global level in function
                RouteUtils.AddGlobalFunction(GatewayCmdUtils.getProjectRoutesConfFilePath(projectName),inFunction,"in"); }
            }

            else if(outFunction != null){
                //api level outFunction
                if(apiID != null){
                    RouteUtils.addFunction(outFunction,"out",apiID,GatewayCmdUtils.getProjectRoutesConfFilePath(projectName),projectName);}
                else if(resourceID != null){
                    //add resource level outFunction
                }
                else {
                //global level outFunction
                RouteUtils.AddGlobalFunction(GatewayCmdUtils.getProjectRoutesConfFilePath(projectName),outFunction,"out"); }
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
