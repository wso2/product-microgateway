package org.wso2am.micro.gw.tests.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2am.micro.gw.tests.context.MicroGWTestException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.stream.Stream;

public class ApictlUtils {
    private static final Logger log = LoggerFactory.getLogger(ApictlUtils.class);

    public static String getVersion() throws IOException {
        String[] cmdArray = new String[]{"version"};
        String[] argsArray = {""};
        String[] responseLines = runApictlCommand(cmdArray, argsArray, 1);
        return responseLines[0].split(" ")[1];
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
        log.info("Created API project zip" + apiProjectName);
        return apiProjectPath + ".zip";
    }

    public static String createProject(String openApiFile, String apiProjectName, String backendCert) throws IOException, MicroGWTestException {
        String targetDir = Utils.getTargetDirPath();
        String openApiFilePath = targetDir + TestConstant.TEST_RESOURCES_PATH
                + File.separator + "openAPIs" + File.separator + openApiFile;
        String projectPathToCreate = targetDir + TestConstant.API_PROJECTS_PATH
                + File.separator + apiProjectName;

        //apictl init <projectPath> --oas <openApiFilePath>
        String[] cmdArray = { "init" };
        String[] argsArray = { projectPathToCreate, "--oas", openApiFilePath };
        String[] responseLines = runApictlCommand(cmdArray, argsArray, 2);

        if (!"Project initialized".equals(responseLines[1].trim())) {
            if ((projectPathToCreate + " already exists").equals(responseLines[0].trim())) {
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
        log.info("Created API project" + apiProjectName);
        return projectPathToCreate;
    }

    public static void addEnv(String mgwEnv) throws MicroGWTestException {
        String[] cmdArray = { "mg", "add", "env" };
        String[] argsArray = { mgwEnv, "--adapter", "https://localhost:9843" };
        try {
            String[] responseLines = runApictlCommand(cmdArray, argsArray, 1);
            if (!responseLines[0].startsWith("Successfully added environment")) {
                throw new MicroGWTestException("Unable to add microgateway adapter env to apictl");
            }
        } catch (IOException e) {
            throw new MicroGWTestException("Unable to add microgateway adapter env to apictl", e);
        }
        log.info("Added apictl microgateway environment: " + mgwEnv);
    }

    public static void removeEnv(String mgwEnv) throws MicroGWTestException {
        String[] cmdArray = { "mg", "remove", "env" };
        String[] argsArray = { mgwEnv };
        try {
            String[] responseLines = runApictlCommand(cmdArray, argsArray, 1);
            if (!responseLines[0].startsWith("Successfully removed environment")) {
                throw new MicroGWTestException("Unable to remove microgateway adapter env from apictl");
            }
        } catch (IOException e) {
            throw new MicroGWTestException("Unable to remove microgateway adapter env from apictl", e);
        }
        log.info("Removed apictl microgateway environment: " + mgwEnv);
    }

    public static void login(String mgwEnv) throws MicroGWTestException {
        String[] cmdArray = { "mg", "login" };
        String[] argsArray = { mgwEnv, "-u", "admin", "-p", "admin", "-k" };
        try {
            String[] responseLines = runApictlCommand(cmdArray, argsArray, 2);
            if (!responseLines[1].startsWith("Successfully logged in")) {
                throw new MicroGWTestException("Unable to login to apictl microgateway adapter env:"
                        + mgwEnv);
            }
        } catch (IOException e) {
            throw new MicroGWTestException("Unable to login to apictl microgateway adapter env:"
                    + mgwEnv, e);
        }
        log.info("Logged into apictl microgateway environment: " + mgwEnv);
    }

    public static void logout(String mgwEnv) throws MicroGWTestException {
        String[] cmdArray = { "mg", "logout" };
        String[] argsArray = { mgwEnv };
        try {
            String[] responseLines = runApictlCommand(cmdArray, argsArray, 1);
            if (!responseLines[0].startsWith("Logged out")) {
                throw new MicroGWTestException("Unable to logout from apictl microgateway adapter env: "
                    + mgwEnv);
            }
        } catch (IOException e) {
            throw new MicroGWTestException("Unable to logout out from apictl microgateway adapter env: "
                    + mgwEnv, e);
        }
        log.info("Logged out from apictl microgateway environment: " + mgwEnv);
    }

    public static void deployAPI(String apiProjectName, String mgwEnv) throws MicroGWTestException {
        String targetDir = Utils.getTargetDirPath();
        String projectPath = targetDir + TestConstant.API_PROJECTS_PATH
                + File.separator + apiProjectName;

        String[] cmdArray = { "mg", "deploy", "api" };
        String[] argsArray = { "-f", projectPath, "-e", mgwEnv, "-o", "-k" };
        try {
            String[] responseLines = runApictlCommand(cmdArray, argsArray, 1);
            if (!responseLines[0].startsWith("Successfully deployed")) {
                throw new MicroGWTestException("Unable to deploy API project: "
                        + apiProjectName + " to microgateway adapter environment: " + mgwEnv);
            }
        } catch (IOException e) {
            throw new MicroGWTestException("Unable to deploy API project: "
                    + apiProjectName + " to microgateway adapter environment: " + mgwEnv, e);
        }
        log.info("Deployed API project: " + apiProjectName + " to microgateway adapter environment: " + mgwEnv);
    }

    public static void undeployAPI(String apiName, String apiVersion, String mgwEnv) throws MicroGWTestException {
        String[] cmdArray = { "mg", "undeploy", "api" };
        String[] argsArray = { "-n", apiName, "-v", apiVersion, "-e", mgwEnv, "-k" };
        try {
            String[] responseLines = runApictlCommand(cmdArray, argsArray, 1);
            if (!responseLines[0].startsWith("API undeployed")) {
                throw new MicroGWTestException("Unable to undeploy API: "
                        + apiName + " from microgateway adapter environment: " + mgwEnv);
            }
        } catch (IOException e) {
            throw new MicroGWTestException("Unable to undeploy API: "
                    + apiName + " to microgateway adapter environment: " + mgwEnv, e);
        }
        log.info("Deployed API project: " + apiName + " to microgateway adapter environment: " + mgwEnv);
    }

    private static String[] runApictlCommand(String[] cmdArray, String[] argsArray,
                                             int numberOfLinesToRead) throws IOException {
        String targetDir = Utils.getTargetDirPath();

        String[] apictl = { targetDir + TestConstant.APICTL_PATH };
        String[] cmdWithArgs = concat(apictl, cmdArray, argsArray);
        String[] responseLines = new String[numberOfLinesToRead];

        Process process = Runtime.getRuntime().exec(cmdWithArgs);
        try(
            InputStreamReader inputStreamReader = new InputStreamReader(process.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader)
        ) {
            for (int i=0; i< numberOfLinesToRead; i++) {
                responseLines[i] = bufferedReader.readLine();
            }
        }
        return responseLines;
    }

    public static String[] concat(String[]... stringArrays) {
        return Arrays.stream(stringArrays).flatMap(Stream::of).toArray(String[]::new);
    }
}
