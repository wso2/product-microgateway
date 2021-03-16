package org.wso2am.micro.gw.tests.util;

import org.wso2am.micro.gw.tests.context.MicroGWTestException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.stream.Stream;

public class ApictlUtils {

    public static String getVersion() throws IOException {
        String apictl = Utils.getTargetDirPath() + TestConstant.APICTL_PATH;
        String[] args = {""};
        String[] cmdArray = new String[]{apictl, "version"};
        String[] cmdArgs = Stream.concat(Arrays.stream(cmdArray), Arrays.stream(args))
                .toArray(String[]::new);
        Process process = Runtime.getRuntime().exec(cmdArgs);
        InputStreamReader inputStreamReader = new InputStreamReader(process.getInputStream());
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String firstLine = bufferedReader.readLine();
        bufferedReader.close();
        inputStreamReader.close();
        return firstLine.split(" ")[1];
    }

    public static String createProjectZip(String openApiFile, String apiProjectName, String backendCert) throws IOException, MicroGWTestException {
        String apiProjectPath = "";
        try {
            apiProjectPath = createProject(openApiFile, apiProjectName, backendCert);
        } catch (MicroGWTestException e) {
            if (e.getMessage().equals("Project already exists")) {
                return Utils.getTargetDirPath() + TestConstant.API_PROJECTS_PATH
                        + File.separator + apiProjectName + ".zip";
            }
            throw e;
        }
        ZipDir.createZipFile(apiProjectPath);
        return apiProjectPath + ".zip";
    }

    public static String createProject(String openApiFile, String apiProjectName, String backendCert) throws IOException, MicroGWTestException {
        String targetDir = Utils.getTargetDirPath();
        String apictl = targetDir + TestConstant.APICTL_PATH;
        String openApiFilePath = targetDir + TestConstant.TEST_RESOURCES_PATH
                + File.separator + "openAPIs" + File.separator + openApiFile;
        String projectPathToCreate = targetDir + TestConstant.API_PROJECTS_PATH
                + File.separator + apiProjectName;

        //apictl init <projectPath> --oas <openApiFilePath>
        String[] cmdArray = { apictl, "init" };
        String[] argsArray = { projectPathToCreate, "--oas", openApiFilePath };

        String[] cmdWithArgs = Stream.concat(Arrays.stream(cmdArray), Arrays.stream(argsArray))
                .toArray(String[]::new);
        Process process = Runtime.getRuntime().exec(cmdWithArgs);
        InputStreamReader inputStreamReader = new InputStreamReader(process.getInputStream());
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String firstLine = bufferedReader.readLine(); //ignore first line
        String secondLine = bufferedReader.readLine();
        bufferedReader.close();
        inputStreamReader.close();
        if (!"Project initialized".equals(secondLine.trim())) {
            if ((projectPathToCreate + " already exists").equals(firstLine.trim())) {
                throw new MicroGWTestException("Project already exists");
            } else {
                throw new MicroGWTestException("Could not initialize API project: " + apiProjectName
                        + " using the API definition: " + openApiFile);
            }
        }
        if (backendCert != null) {
            Utils.copyFile(
                    targetDir + TestConstant.TEST_RESOURCES_PATH + File.separator +
                            "certs" + File.separator + backendCert,
                    projectPathToCreate + File.separator + TestConstant.ENDPOINT_CERTIFICATES
                            + File.separator + "backend.crt");
        }
        return projectPathToCreate;
    }
}
