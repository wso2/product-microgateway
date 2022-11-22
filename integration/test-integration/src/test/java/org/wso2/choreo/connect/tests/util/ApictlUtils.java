package org.wso2.choreo.connect.tests.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.choreo.connect.tests.context.CCTestException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class ApictlUtils {
    public static final String APICTL = "apictl";
    public static final String VERSION = "version";
    public static final String INIT = "init";
    public static final String MG = "mg";
    public static final String ADD = "add";
    public static final String ENV = "env";
    public static final String REMOVE = "remove";
    public static final String LOGIN = "login";
    public static final String LOGOUT = "logout";
    public static final String DEPLOY = "deploy";
    public static final String UNDEPLOY = "undeploy";
    public static final String API = "api";

    public static final String OAS_FLAG = "--oas";
    public static final String ADAPTER_FLAG = "--adapter";
    public static final String USER_FLAG = "-u";
    public static final String PASSWORD_FLAG = "-p";
    public static final String FILE_FLAG = "-f";
    public static final String ENV_FLAG = "-e";
    public static final String GATEWAY_ENV_FLAG = "-g";
    public static final String VHOST_FLAG = "-t";
    public static final String OVERRIDE_FLAG = "-o";
    public static final String NAME_FLAG = "-n";
    public static final String VERSION_FLAG = "-v";

    public static final String PROJECT_INITIALIZED_RESPONSE = "Project initialized";
    public static final String ALREADY_EXISTS_RESPONSE = " already exists";
    public static final String SUCCESSFUL_ADD_ENV_RESPONSE = "Successfully added environment";
    public static final String SUCCESSFUL_LOGIN_RESPONSE = "Successfully logged in";
    public static final String SUCCESSFUL_LOGOUT_RESPONSE = "Logged out";
    public static final String SUCCESSFULLY_DEPLOYED_RESPONSE = "Successfully deployed";
    public static final String SUCCESSFULLY_UNDEPLOYED_RESPONSE = "API undeployed";

    public static final String ENDPOINT_CERTIFICATES = "Endpoint-certificates";
    public static final String INTERCEPTORS = "interceptors";
    public static final String DEPLOYMENT_ENVIRONMENTS_YAML = "deployment_environments.yaml";

    public static final String APICTL_PATH = File.separator + "apictl" + File.separator;
    public static final String API_PROJECTS_PATH = File.separator + "apiProjects" + File.separator;
    public static final String OPENAPIS_PATH = TestConstant.TEST_RESOURCES_PATH + File.separator +
            "openAPIs" + File.separator;
    public static final String BACKEND_CERTS_PATH = TestConstant.TEST_RESOURCES_PATH + File.separator +
            "certs" + File.separator;
    public static final String DEPLOYMENT_ENVIRONMENTS_YAML_PATH = TestConstant.TEST_RESOURCES_PATH + File.separator +
            "deploymentEnvironments" + File.separator;
    public static final String API_YAML_PATH = TestConstant.TEST_RESOURCES_PATH + File.separator + "apiYaml" +
            File.separator;
    public static final String MGW_ADAPTER_CERTS_PATH = TestConstant.CC_TEMP_PATH + TestConstant.DOCKER_COMPOSE_DIR
            + File.separator + "resources" + File.separator + "adapter" + File.separator + "security"
            + File.separator + "truststore" + File.separator;
    public static final String APICTL_CERTS_PATH = File.separator +
            ".wso2apictl" + File.separator + "certs" + File.separator;

    private static final Logger log = LoggerFactory.getLogger(ApictlUtils.class);

    /**
     * Get the version of apictl downloaded for integration tests
     *
     * @return downloaded version of apictl
     * @throws IOException if the runtime fails to execute the apictl command
     */
    public static String getVersion() throws IOException {
        String[] cmdArray = new String[]{VERSION};
        String[] argsArray = {""};
        String[] responseLines = runApictlCommand(cmdArray, argsArray, 1);
        return responseLines[0].split(" ")[1];
    }

    /**
     * Create and zip an API project - To be used when directly creating an API via
     * the microgateway REST API rather than via the apictl deploy command
     *
     * @param openApiFile openAPI file to create the API project from
     * @param apiProjectName expected name of the project that gets created
     * @param backendCert name of the backend cert file that should be included in the
     *                    Endpoint-certificates folder in the API project
     * @return absolute path of the zipped API project
     * @throws IOException if the runtime fails to execute the apictl command
     * @throws CCTestException if apictl was unable to create the project
     */
    public static String createProjectZip(String openApiFile, String apiProjectName, String backendCert) throws IOException, CCTestException {
        try {
            createProject(openApiFile, apiProjectName, backendCert, null, null,null);
        } catch (CCTestException e) {
            if (!e.getMessage().equals("Project already exists")) {
                throw e;
            }
        }
        String apiProjectPath = Utils.getTargetDirPath() + API_PROJECTS_PATH + apiProjectName;
        ZipDir.createZipFile(apiProjectPath);
        log.info("Created API project zip " + apiProjectName);
        return apiProjectPath + ".zip";
    }

    /**
     * Create an API project - To be used before deploying an API via the apictl deploy command
     *
     * @param openApiFile openAPI file to create the API project from
     * @param apiProjectName expected name of the project that gets created
     * @throws IOException if the runtime fails to execute the apictl command
     * @throws CCTestException if apictl was unable to create the project
     */
    public static void createProject(String openApiFile, String apiProjectName) throws IOException, CCTestException {
        createProject(openApiFile, apiProjectName, null, null, null,null);
    }

    /**
     * Create an API project - To be used before deploying an API via the apictl deploy command
     *
     * @param openApiFile openAPI file to create the API project from
     * @param apiProjectName expected name of the project that gets created
     * @param backendCert name of the backend cert file that should be included in the
     *                    Endpoint-certificates folder of the API project
     * @param deployEnvYamlFile deployment_environments.yaml file of API project
     * @param interceptorCert name of the interceptor cert file that should be included in the
     *                    Endpoint-certificates/interceptor folder of the API project
     * @throws IOException if the runtime fails to execute the apictl command
     * @throws CCTestException if apictl was unable to create the project
     */
    public static void createProject(String openApiFile, String apiProjectName, String backendCert, String deployEnvYamlFile,
                                     String interceptorCert, String apiYamlFile)
            throws IOException, CCTestException {
        String targetDir = Utils.getTargetDirPath();
        String openApiFilePath;
        if(openApiFile.startsWith("https://") || openApiFile.startsWith("http://")) {
            openApiFilePath = openApiFile;
        } else {
            openApiFilePath = targetDir + OPENAPIS_PATH + openApiFile;
        }
        String projectPathToCreate = targetDir + API_PROJECTS_PATH + apiProjectName;

        //apictl init <projectPath> --oas <openApiFilePath>
        String[] cmdArray = { INIT };
        String[] argsArray = { projectPathToCreate, OAS_FLAG, openApiFilePath };
        String[] responseLines = runApictlCommand(cmdArray, argsArray, 2);

        if (responseLines[1]!= null && !PROJECT_INITIALIZED_RESPONSE.equals(responseLines[1].trim())) {
            if ((projectPathToCreate + ALREADY_EXISTS_RESPONSE).equals(responseLines[0].trim())) {
                throw new CCTestException("Project already exists");
            } else {
                throw new CCTestException("Could not initialize API project: " + apiProjectName
                        + " using the API definition: " + openApiFile);
            }
        }
        if (backendCert != null) {
            Utils.copyFile(
                    targetDir + BACKEND_CERTS_PATH + backendCert,
                    projectPathToCreate + File.separator + ENDPOINT_CERTIFICATES
                            + File.separator + "backend.crt");
        }
        if (deployEnvYamlFile != null) {
            Utils.copyFile(
                    targetDir + DEPLOYMENT_ENVIRONMENTS_YAML_PATH + deployEnvYamlFile,
                    projectPathToCreate + File.separator + DEPLOYMENT_ENVIRONMENTS_YAML);
        }
        if (apiYamlFile != null) {
            Utils.copyFile(targetDir + API_YAML_PATH + apiYamlFile, projectPathToCreate +
                    File.separator + "api.yaml");
        }
        if (interceptorCert != null) {
            Utils.copyFile(
                    targetDir + BACKEND_CERTS_PATH + interceptorCert,
                    projectPathToCreate + File.separator + ENDPOINT_CERTIFICATES
                            + File.separator + INTERCEPTORS + File.separator + "interceptor.crt");
        }
        log.info("Created API project " + apiProjectName);
    }

    /**
     * Add a microgateway adapter env to apictl
     *
     * @param mgwEnv name of the apictl mgw env
     * @throws CCTestException if apictl was unable to add the env
     */
    public static void addEnv(String mgwEnv) throws CCTestException {
        String[] cmdArray = { MG, ADD, ENV };
        String[] argsArray = { mgwEnv, ADAPTER_FLAG, "https://localhost:9843" };
        try {
            String[] responseLines = runApictlCommand(cmdArray, argsArray, 1);
            if (responseLines[0]!= null && !responseLines[0].startsWith(SUCCESSFUL_ADD_ENV_RESPONSE)) {
                throw new CCTestException("Unable to add microgateway adapter env to apictl");
            }
        } catch (IOException e) {
            throw new CCTestException("Unable to add microgateway adapter env to apictl", e);
        }
        // copy mgw public cert to apictl's cert folder
        //${home_dir}/security/truststore/mg.pem -> ${home_dir}/.wso2apictl/certs/mg.pem
        String targetDir = Utils.getTargetDirPath();
        Utils.copyFile(
                targetDir + MGW_ADAPTER_CERTS_PATH + "mg.pem",
                System.getProperty("user.home") + APICTL_CERTS_PATH + "mg.pem");
        log.info("Added apictl microgateway environment: " + mgwEnv);
    }

    /**
     * Remove a microgateway adapter env from apictl
     *
     * @param mgwEnv name of the apictl mgw env
     * @throws CCTestException if apictl was unable to remove the env
     */
    public static void removeEnv(String mgwEnv) throws CCTestException {
        String[] cmdArray = { MG, REMOVE, ENV };
        String[] argsArray = { mgwEnv };
        try {
            log.info("Removing apictl microgateway environment: " + mgwEnv);
            runApictlCommand(cmdArray, argsArray, 1);
        } catch (IOException e) {
            throw new CCTestException("Unable to remove microgateway adapter env from apictl", e);
        }
    }

    /**
     * Login to a microgateway adapter env in apictl
     *
     * @param mgwEnv name of the apictl mgw env
     * @throws CCTestException if apictl was unable to login to the env
     *                  i.e. if apictl was unable to get an access token from mgw and save
     */
    public static void login(String mgwEnv) throws CCTestException {
        String[] cmdArray = { MG, LOGIN };
        String[] argsArray = { mgwEnv, USER_FLAG, "admin", PASSWORD_FLAG, "admin" };
        try {
            String[] responseLines = runApictlCommand(cmdArray, argsArray, 2);

            if (responseLines[1]!= null && !responseLines[1].startsWith(SUCCESSFUL_LOGIN_RESPONSE)) {
                throw new CCTestException("Unable to login to apictl microgateway adapter env:"
                        + mgwEnv);
            }
        } catch (IOException e) {
            throw new CCTestException("Unable to login to apictl microgateway adapter env:"
                    + mgwEnv, e);
        }
        log.info("Logged into apictl microgateway environment: " + mgwEnv);
    }

    /**
     * Logout from a microgateway adapter env in apictl
     *
     * @param mgwEnv name of the apictl mgw env
     * @throws CCTestException if apictl was unable to logout from the env
     */
    public static void logout(String mgwEnv) throws CCTestException {
        String[] cmdArray = { MG, LOGOUT };
        String[] argsArray = { mgwEnv };
        try {
            String[] responseLines = runApictlCommand(cmdArray, argsArray, 1);
            if (responseLines[0]!= null && !responseLines[0].startsWith(SUCCESSFUL_LOGOUT_RESPONSE)) {
                throw new CCTestException("Unable to logout from apictl microgateway adapter env: "
                    + mgwEnv);
            }
        } catch (IOException e) {
            throw new CCTestException("Unable to logout out from apictl microgateway adapter env: "
                    + mgwEnv, e);
        }
        log.info("Logged out from apictl microgateway environment: " + mgwEnv);
    }

    /**
     * Deploy an API via apictl
     *
     * @param apiProjectName API project that represents the API
     * @param mgwEnv name of the apictl mgw env
     * @throws CCTestException if apictl was unable to deploy the API to the apictl mgw env
     */
    public static void deployAPI(String apiProjectName, String mgwEnv) throws CCTestException {
        String targetDir = Utils.getTargetDirPath();
        String projectPath = targetDir + API_PROJECTS_PATH + apiProjectName;

        String[] cmdArray = { MG, DEPLOY, API };
        String[] argsArray = { FILE_FLAG, projectPath, ENV_FLAG, mgwEnv, OVERRIDE_FLAG };
        try {
            String[] responseLines = runApictlCommand(cmdArray, argsArray, 1);
            if (responseLines[0]!= null && !responseLines[0].startsWith(SUCCESSFULLY_DEPLOYED_RESPONSE)) {
                throw new CCTestException("Unable to deploy API project: "
                        + apiProjectName + " to microgateway adapter environment: " + mgwEnv);
            }
        } catch (IOException e) {
            throw new CCTestException("Unable to deploy API project: "
                    + apiProjectName + " to microgateway adapter environment: " + mgwEnv, e);
        }
        log.info("Deployed API project: " + apiProjectName + " to microgateway adapter environment: " + mgwEnv);
    }

    /**
     * Undeploy an API via apictl
     *
     * @param apiName name of the API (in api.yaml) to undeploy
     * @param apiVersion version of the API
     * @param mgwEnv name of the apictl mgw env the API was deployed
     * @param vhost vhost of the API to be undeployed from
     * @throws CCTestException if apictl was unable to undeploy the API
     */
    public static void undeployAPI(String apiName, String apiVersion, String mgwEnv, String vhost) throws CCTestException {
        undeployAPI(apiName, apiVersion, mgwEnv, vhost, null);
    }

    /**
     * Undeploy an API via apictl
     *
     * @param apiName name of the API (in api.yaml) to undeploy
     * @param apiVersion version of the API
     * @param mgwEnv name of the apictl mgw env the API was deployed
     * @param vhost vhost of the API to be undeployed from
     * @param gatewayEnv gatewayEnv of the API to be undeployed from
     * @throws CCTestException if apictl was unable to undeploy the API
     */
    public static void undeployAPI(String apiName, String apiVersion, String mgwEnv, String vhost, String gatewayEnv)
            throws CCTestException {
        String[] cmdArray = { MG, UNDEPLOY, API };
        List<String> args = new ArrayList<>(Arrays.asList(NAME_FLAG, apiName, VERSION_FLAG, apiVersion, ENV_FLAG,
                mgwEnv));
        if (gatewayEnv != null ) {
            args.add(GATEWAY_ENV_FLAG);
            args.add(gatewayEnv);
        }

        String loggedVhost = "<EMPTY_VHOST>";
        if (StringUtils.isNotEmpty(vhost)) {
            loggedVhost = vhost;
            args.addAll(Arrays.asList(VHOST_FLAG, vhost));
        }
        String[] argsArray = args.toArray(String[]::new);
        try {
            String[] responseLines = runApictlCommand(cmdArray, argsArray, 1);
            if (responseLines[0]!= null && !responseLines[0].startsWith(SUCCESSFULLY_UNDEPLOYED_RESPONSE)) {
                throw new CCTestException("Unable to undeploy API: "
                        + apiName + " from microgateway adapter environment: " + mgwEnv + " vhost: " + loggedVhost);
            }
        } catch (IOException e) {
            throw new CCTestException("Unable to undeploy API: "
                    + apiName + " from microgateway adapter environment: " + mgwEnv + " vhost: " + loggedVhost, e);
        }
        log.info("Undeployed API project: " + apiName + " from microgateway adapter environment: " + mgwEnv + " vhost: " + loggedVhost);
    }

    /**
     * Execute an apictl command
     *
     * @param cmdArray array of apictl microgateway commands
     * @param argsArray array of arguments for the command
     * @param numberOfLinesToRead number of lines to read from the command line response
     * @return String array consisting the lines that usually gets printed after executing
     *          the apictl command
     * @throws IOException if the runtime fails to execute the apictl command
     */
    private static String[] runApictlCommand(String[] cmdArray, String[] argsArray,
                                             int numberOfLinesToRead) throws IOException {
        String targetDir = Utils.getTargetDirPath();
        String[] apictl = { targetDir + APICTL_PATH + APICTL };
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

    /**
     * Merge any number of String arrays to one new String array
     *
     * @param stringArrays String arrays to merge
     * @return a new array with the content of the given arrays
     */
    public static String[] concat(String[]... stringArrays) {
        return Arrays.stream(stringArrays).flatMap(Stream::of).toArray(String[]::new);
    }
}
