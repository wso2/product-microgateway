/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.apimgt.gateway.cli.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.cli.cipher.AESCipherTool;
import org.wso2.apimgt.gateway.cli.cipher.AESCipherToolException;
import org.wso2.apimgt.gateway.cli.codegen.CodeGenerationContext;
import org.wso2.apimgt.gateway.cli.config.TOMLConfigParser;
import org.wso2.apimgt.gateway.cli.constants.CliConstants;
import org.wso2.apimgt.gateway.cli.constants.TokenManagementConstants;
import org.wso2.apimgt.gateway.cli.exception.CLIInternalException;
import org.wso2.apimgt.gateway.cli.exception.CLIRuntimeException;
import org.wso2.apimgt.gateway.cli.exception.CliLauncherException;
import org.wso2.apimgt.gateway.cli.exception.ConfigParserException;
import org.wso2.apimgt.gateway.cli.hashing.HashUtils;
import org.wso2.apimgt.gateway.cli.model.config.Config;
import org.wso2.apimgt.gateway.cli.model.config.ContainerConfig;
import org.wso2.apimgt.gateway.cli.model.rest.APICorsConfigurationDTO;
import org.wso2.apimgt.gateway.cli.model.rest.ext.ExtendedAPI;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

/**
 * Utility functions providing tasks related to MGW toolkit.
 */
public final class CmdUtils {

    private static final Logger logger = LoggerFactory.getLogger(CmdUtils.class);
    private static Config config;
    private static ContainerConfig containerConfig;
    private static CodeGenerationContext codeGenerationContext;
    private static boolean verboseLogsEnabled = setVerboseEnabled();
    private static final String openAPISpec2 = "2";
    private static final PrintStream OUT = System.out;
    private static final PrintStream ERR = System.err;
    private static String consoleMessages = "";
    private static String callHomeMessage = "";

    private CmdUtils() {

    }

    public static Config getConfig() {
        return config;
    }

    public static void setConfig(Config configFromFile) {
        config = configFromFile;
    }

    public static CodeGenerationContext getCodeGenerationContext() {
        return codeGenerationContext;
    }

    public static void setCodeGenerationContext(CodeGenerationContext codeGenerationContext) {
        CmdUtils.codeGenerationContext = codeGenerationContext;
    }

    /**
     * Read file as string
     *
     * @param path       to the file
     * @param inResource whether file is in resources directory of jar or not
     * @return file content
     * @throws IOException if file read went wrong
     */
    public static String readFileAsString(String path, boolean inResource) throws IOException {
        InputStream is;
        if (inResource) {
            path = getUnixPath(path);
            is = ClassLoader.getSystemResourceAsStream(path);
        } else {
            is = new FileInputStream(new File(path));
        }
        InputStreamReader inputStreamREader = null;
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();
        try {
            inputStreamREader = new InputStreamReader(is, StandardCharsets.UTF_8);
            br = new BufferedReader(inputStreamREader);
            String content = br.readLine();
            if (content == null) {
                return sb.toString();
            }

            sb.append(content);

            while ((content = br.readLine()) != null) {
                sb.append('\n').append(content);
            }
        } finally {
            if (inputStreamREader != null) {
                try {
                    inputStreamREader.close();
                } catch (IOException ignore) {
                }
            }
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ignore) {
                }
            }
        }
        return sb.toString();
    }

    /**
     * Create usage exception
     *
     * @param errorMsg error message
     * @return created usage exception
     */
    public static CliLauncherException createUsageException(String errorMsg) {
        CliLauncherException launcherException = new CliLauncherException();
        launcherException.addMessage("micro-gw: " + errorMsg);
        launcherException.addMessage("Run 'micro-gw help <command>' for usage.");
        return launcherException;
    }

    /**
     * Encrypt given value with provided secret
     *
     * @param value  value to encrypt
     * @param secret encryption key
     * @return encrypted value
     */
    public static String encrypt(String value, String secret) {
        try {
            AESCipherTool cipherTool = new AESCipherTool(secret);
            return cipherTool.encrypt(value);
        } catch (AESCipherToolException e) {
            throw createUsageException("failed to encrypt client secret");
        }
    }

    /**
     * Decrypt given value with provided secret
     *
     * @param value  value to decrypt
     * @param secret decryption key
     * @return decrypted value
     */
    public static String decrypt(String value, String secret) {
        try {
            AESCipherTool cipherTool = new AESCipherTool(secret);
            return cipherTool.decrypt(value);
        } catch (AESCipherToolException e) {
            throw createUsageException("failed to decrypt client secret");
        }
    }

    /**
     * Returns current user dir
     *
     * @return current user dir
     */
    public static String getUserDir() {
        String currentDirProp = System.getProperty(CliConstants.SYS_PROP_CURRENT_DIR);
        if (currentDirProp != null) {
            return currentDirProp;
        } else {
            return System.getProperty(CliConstants.SYS_PROP_USER_DIR);
        }
    }

    /**
     * Get cli home location
     *
     * @return cli home location
     */
    public static String getCLIHome() {
        return System.getProperty(CliConstants.CLI_HOME);
    }

    /**
     * Get cacerts location.
     *
     * @return cacerts location
     */
    public static String getCacertsLocation() {
        return System.getProperty(CliConstants.CACERTS_DIR);
    }

    /**
     * Get cacerts password.
     *
     * @return cacerts password
     */
    public static String getCacertsPassword() {
        String cacertsPassword = "";
        String password = System.getenv("CACERTS_PASS");

        cacertsPassword = password != null ? password : CliConstants.DEFAULT_CACERTS_PASS;
        return cacertsPassword;
    }

    /**
     * Get cli library location
     *
     * @return cli lib location
     */
    public static String getCLILibPath() {
        return getCLIHome() + File.separator + CliConstants.CLI_LIB;
    }

    /**
     * Get resources file directory path
     *
     * @return resources file directory path
     */
    public static String getResourceFolderLocation() {
        return System.getProperty(CliConstants.CLI_HOME) + File.separator
                + CliConstants.GW_DIST_RESOURCES;
    }

    /**
     * Get grpc directory location inside Resources Directory.
     *
     * @return protobuf directory location
     */
    public static String getResourcesGrpcDirLocation() {
        return getResourceFolderLocation() + File.separator + CliConstants.RESOURCES_GRPC_DIR;
    }

    /**
     * Get resources file directory path
     *
     * @return resources file directory path
     */
    public static String getLoggingPropertiesFileLocation() {
        return System.getProperty(CliConstants.CLI_HOME) + File.separator + CliConstants.CLI_CONF
                + File.separator + CliConstants.LOGGING_PROPERTIES_FILENAME;
    }

    /**
     * Get filters folder location
     *
     * @return filters folder location
     */
    public static String getDefinitionsLocation() {
        return getResourceFolderLocation() + File.separator + CliConstants.GW_DIST_DEFINITIONS;
    }

    /**
     * Get filters folder location
     *
     * @return filters folder location
     */
    public static String getFiltersFolderLocation() {
        return getResourceFolderLocation() + File.separator + CliConstants.GW_DIST_FILTERS;
    }

    /**
     * Get temp folder location
     *
     * @param projectName name of the project
     * @return temp folder location
     */
    private static String getProjectTempFolderLocation(String projectName) {
        return getProjectDirectoryPath(projectName) + File.separator + CliConstants.TEMP_DIR_NAME;
    }

    /**
     * Set api definition file.
     *
     * @param projectName name of the project
     * @param apiDefinition api definition file local path
     * @param headers api definition download request headers
     * @param values api definition download values for request headers
     * @param insecure insecure url connection
     */
    private static void setApiDefinition(String projectName, String apiDefinition,
                                         String headers, String values, boolean insecure) throws IOException {
        String apiDefinitionsDir = getProjectAPIFilesDirectoryPath(projectName);
        String filePath;

        if (isURL(apiDefinition)) {
            ArrayList<String> headersList = new ArrayList<>();
            ArrayList<String> valuesList = new ArrayList<>();
            if (!StringUtils.isBlank(headers) && !StringUtils.isBlank(values)) {
                headersList = new ArrayList(Arrays.asList(headers.split(",")));
                valuesList = new ArrayList(Arrays.asList(values.split(",")));
                if (headersList.size() > 0 && (headersList.size() != valuesList.size())) {
                    throw new CLIRuntimeException("Provided number of header and number of values is different");
                }
                logger.debug("Request headers : " + headers + " values : " + values + "are provided.");
            }
            filePath = downloadFile(apiDefinition, apiDefinitionsDir, headersList, valuesList, insecure);
        } else {
            //validate api-definition file path
            File apiDefinitionFile = new File(apiDefinition);
            if (!apiDefinitionFile.exists()) {
                throw CmdUtils.createUsageException(
                        "Error while getting the open API definition. Probably the file path '"
                                + apiDefinition + "' is invalid.");
            }

            filePath = apiDefinitionsDir + File.separator + Paths.get(apiDefinition).getFileName();
            Files.copy(Paths.get(apiDefinition), Paths.get(filePath));
            logger.debug("Api definition is successfully copied to :" + filePath);
        }

        if (!(filePath.endsWith(CliConstants.JSON_EXTENSION) || filePath.endsWith(CliConstants.YAML_EXTENSION))) {
            logger.debug("API definition file name has no .json or .yaml extension");
            if (addExtension(filePath)) {
                logger.debug("API definition renamed.");
            }
        }
    }

    /**
     * Url validation, Allow any url with https and http.
     * Allow any url without fully qualified domain.
     *
     * @param url Url as string
     * @return boolean type stating validated or not
     */
    private static boolean isURL(String url) {
        Pattern pattern = Pattern.compile("^(http|https)://(.)+", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(url);
        return matcher.matches();
    }

    /**
     * Identify a Json content.
     *
     * @param content content
     */
    private static boolean isJson(String content) {
        try {
            new JsonParser().parse(content);
        } catch (JsonParseException e) {
            return false;
        }
        return true;
    }

    /**
     * Identify a Yaml content.
     *
     * @param content content
     */
    private static boolean isYaml(String content) {
        ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
        try {
            yamlReader.readValue(content, Object.class);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    /**
     * Add yaml and json file extensions.
     *
     * @param filePath file
     */
    private static boolean addExtension(String filePath) {
        String content;
        File file = new File(filePath);
        try {
            content = readFileAsString(filePath, false);
            if (isJson(content)) {
                File newFile = new File(filePath + CliConstants.JSON_EXTENSION);
                logger.debug("File identified as a " + CliConstants.JSON_EXTENSION + " file.");
                return file.renameTo(newFile);
            } else if (isYaml(content)) {
                File newFile = new File(filePath + CliConstants.YAML_EXTENSION);
                logger.debug("File identified as a " + CliConstants.YAML_EXTENSION + " file.");
                return file.renameTo(newFile);
            } else {
                logger.debug("Failed to identify format");
                return false;
            }
        } catch (IOException e) {
            throw new CLIRuntimeException("Error while reading file: " + filePath);
        }
    }

    /**
     * Create a project structure for a particular project name.
     *
     * @param projectName name of the project
     */
    public static void createProjectStructure(String projectName, String apiDefinition, String headers, String values,
                                              boolean insecure) throws IOException {
        File projectDir = createDirectory(getUserDir() + File.separator + projectName, false);

        String interceptorsPath = projectDir + File.separator + CliConstants.PROJECT_INTERCEPTORS_DIR;
        createDirectory(interceptorsPath, false);
        createFile(interceptorsPath, CliConstants.KEEP_FILE, true);

        String externalLibPath = projectDir + File.separator + CliConstants.CLI_LIB;
        createDirectory(externalLibPath, false);
        createFile(externalLibPath, CliConstants.KEEP_FILE, true);

        String extensionsPath = projectDir + File.separator + CliConstants.PROJECT_EXTENSIONS_DIR;
        createDirectory(extensionsPath, false);

        String confDirPath = projectDir + File.separator + CliConstants.PROJECT_CONF_DIR;
        createDirectory(confDirPath, false);

        String definitionsPath = projectDir + File.separator + CliConstants.PROJECT_API_DEFINITIONS_DIR;
        createDirectory(definitionsPath, false);
        if (!StringUtils.isEmpty(apiDefinition)) {
            setApiDefinition(projectName, apiDefinition, headers, values, insecure);
        }

        String grpcDefinitionsPath = projectDir + File.separator + CliConstants.PROJECT_GRPC_DEFINITIONS_DIR;
        createDirectory(grpcDefinitionsPath, false);

        createFile(projectDir.getPath(), CliConstants.PROJECT_POLICIES_FILE, true);
        String policyResPath = getDefinitionsLocation() + File.separator + CliConstants.GW_DIST_POLICIES_FILE;
        File policyResFile = new File(policyResPath);
        File policesFile = new File(projectDir + File.separator + CliConstants.PROJECT_POLICIES_FILE);

        String extensionResPath = getFiltersFolderLocation() + File.separator +
                CliConstants.GW_DIST_EXTENSION_FILTER;
        File extensionResFile = new File(extensionResPath);
        File extensionFile = new File(extensionsPath + File.separator +
                CliConstants.GW_DIST_EXTENSION_FILTER);

        String tokenRevocationResPath = getFiltersFolderLocation() + File.separator +
                CliConstants.GW_DIST_TOKEN_REVOCATION_EXTENSION;
        File tokenRevocationResFile = new File(tokenRevocationResPath);
        File tokenRevocationFile = new File(extensionsPath + File.separator +
                CliConstants.GW_DIST_TOKEN_REVOCATION_EXTENSION);

        String startUpExtensionResourcePath = getFiltersFolderLocation() + File.separator +
                CliConstants.GW_DIST_START_UP_EXTENSION;
        File startUpExtensionResFile = new File(startUpExtensionResourcePath);
        File startUpExtensionFile = new File(extensionsPath + File.separator +
                CliConstants.GW_DIST_START_UP_EXTENSION);

        if (Files.exists(extensionResFile.toPath())) {
            FileUtils.copyFile(extensionResFile, extensionFile);
        } else {
            throw new CLIRuntimeException("Extension filter not found in CLI_HOME");
        }
        if (Files.exists(tokenRevocationResFile.toPath())) {
            FileUtils.copyFile(tokenRevocationResFile, tokenRevocationFile);
        } else {
            throw new CLIRuntimeException("Token revocation extension not Found in CLI_HOME");
        }
        if (Files.exists(startUpExtensionResFile.toPath())) {
            FileUtils.copyFile(startUpExtensionResFile, startUpExtensionFile);
        } else {
            throw new CLIRuntimeException("Start up extension not Found in CLI_HOME");
        }
        if (Files.exists(policyResFile.toPath())) {
            FileUtils.copyFile(policyResFile, policesFile);
        } else {
            throw new CLIRuntimeException("Policy definition not found in CLI_HOME");
        }

    }

    /**
     * Save openAPI definition (developer first approach)
     *
     * @param projectName   project name
     * @param apiDefinition api Definition as String
     */
    public static void saveSwaggerDefinition(String projectName, String apiDefinition, String apiId, String extension) {
        if (apiDefinition.isEmpty()) {
            throw new CLIInternalException("No swagger definition is provided to generate API");
        }
        try {
            Path genPath = Paths.get(CmdUtils.getProjectGenDirectoryPath(projectName));
            Path apiDefPath = Paths.get(CmdUtils.getProjectGenAPIDefinitionPath(projectName));
            if (Files.notExists(genPath)) {
                Files.createDirectory(genPath);
                Files.createDirectory(apiDefPath);
            }
            writeContent(apiDefinition, new File(getProjectGenSwaggerPath(projectName, apiId, extension)));
        } catch (IOException e) {
            throw new CLIInternalException("Error while copying the swagger to the project directory");
        }
    }

    private static void saveSwaggerDefinitionForSingleAPI(String projectName, ExtendedAPI api, boolean isExpand) {
        String swaggerString = OpenAPICodegenUtils.generateSwaggerString(api, isExpand);
        String apiId = HashUtils.generateAPIId(api.getName(), api.getVersion());
        String extension = openAPISpec2.equals(OpenAPICodegenUtils.findSwaggerVersion(api.getApiDefinition(), false))
                ? CliConstants.API_SWAGGER : CliConstants.API_OPENAPI_YAML;
        CmdUtils.saveSwaggerDefinition(projectName, swaggerString, apiId, extension);
    }

    /**
     * Save swagger definition for multiple APIs.
     *
     * @param projectName project name
     * @param apis        API object List
     */
    public static void saveSwaggerDefinitionForMultipleAPIs(String projectName, List<ExtendedAPI> apis,
            boolean isExpand) {
        for (ExtendedAPI api : apis) {
            saveSwaggerDefinitionForSingleAPI(projectName, api, isExpand);
            OUT.println("ID for API with name " + api.getName() + " : "
                    + HashUtils.generateAPIId(api.getName(), api.getVersion()));
        }
    }

    /**
     * Load the stored resource hash content from the CLI temp folder
     *
     * @param projectName name of the project
     * @return stored resource hash content from the CLI temp folder
     * @throws IOException error while loading stored resource hash content
     */
    public static String loadStoredResourceHashes(String projectName) throws IOException {
        String resourceHashFileLocation = getResourceHashHolderFileLocation(projectName);
        String content = null;
        if (new File(resourceHashFileLocation).exists()) {
            content = CmdUtils.readFileAsString(resourceHashFileLocation, false);
        }
        return content;
    }

    /**
     * Saves the resource hash content to the CLI temp folder
     *
     * @param content     resource hash content
     * @param projectName name of the project
     * @throws IOException error while saving resource hash content
     */
    public static void storeResourceHashesFileContent(String content, String projectName) throws IOException {
        String tempDirPath = getProjectTempFolderLocation(projectName);
        createDirectory(tempDirPath, false);

        String resourceHashesFileLocation = getResourceHashHolderFileLocation(projectName);
        File pathFile = new File(resourceHashesFileLocation);
        if (!pathFile.exists()) {
            boolean created = pathFile.createNewFile();
            if (created) {
                logger.trace("Hashed file: {} created. ", resourceHashesFileLocation);
            } else {
                logger.error("Failed to create hash file: {} ", resourceHashesFileLocation);
                throw new CLIInternalException("Error occurred while setting up the workspace structure");
            }
        }
        //Write Content
        writeContent(content, pathFile);
    }

    /**
     * Get resource hash holder file path
     *
     * @param projectName name of the project
     * @return resource hash holder file path
     */
    private static String getResourceHashHolderFileLocation(String projectName) {
        return getProjectTempFolderLocation(projectName) + File.separator
                + CliConstants.RESOURCE_HASH_HOLDER_FILE_NAME;
    }

    /**
     * Returns location of the main configuration file of given project root
     *
     * @return path configuration file
     */
    public static String getMainConfigLocation() {
        return getCLIHome() + File.separator + CliConstants.GW_DIST_CONF + File.separator
                + CliConstants.MAIN_CONFIG_FILE_NAME;
    }

    /**
     * Returns path to the project conf folder
     *
     * @return path to the project conf folder
     */
    private static String getProjectConfigDirPath(String projectName) {
        return getProjectDirectoryPath(projectName) + File.separator + CliConstants.PROJECT_CONF_DIR;
    }

    /**
     * Returns location of the deployment configuration file of given project
     *
     * @param projectName name of the project
     * @return path to deployment configuration file
     */
    public static String getDeploymentConfigLocation(String projectName) {
        return getProjectConfigDirPath(projectName) + File.separator + CliConstants.DEPLOYMENT_CONFIG_FILE_NAME;
    }

    /**
     * Returns path to the given project in the current working directory
     *
     * @param projectName name of the project
     * @return path to the given project in the current working directory
     */
    public static String getProjectDirectoryPath(String projectName) {
        return getUserDir() + File.separator + projectName;
    }

    /**
     * Returns path to the /gen of a given project in the current working directory
     *
     * @param projectName name of the project
     * @return path to the /src of a given project in the current working directory
     */
    public static String getProjectGenDirectoryPath(String projectName) {
        return getProjectDirectoryPath(projectName) + File.separator
                + CliConstants.PROJECT_GEN_DIR;
    }

    /**
     * Returns path to the /target/gen of a given project in the current working directory.
     *
     * @param projectName name of the project
     * @return path to the /target/gen of a given project in the current working directory
     */
    public static String getProjectTargetGenDirectoryPath(String projectName) {
        return getProjectDirectoryPath(projectName) + File.separator + CliConstants.PROJECT_TARGET_DIR
                + File.separator + CliConstants.PROJECT_GEN_DIR;
    }

    /**
     * Returns path to the /target/gen/gRPCSrc of a given project in the current working directory.
     *
     * @param projectName name of the project
     * @return path to the /target/gen/gRPCSrc of a given project in the current working directory
     */
    public static String getProjectTargetGenGrpcSrcDirectory(String projectName) {
        return getProjectTargetGenDirectoryPath(projectName) + File.separator + CliConstants.GEN_GRPC_SRC_DIR;
    }

    /**
     * Returns path to the /target/gen/gRPCSrc/OpenAPIs of a given project in the current working directory.
     *
     * @param projectName name of the project
     * @return path to the /target/gen/gRPCSrc/OpenAPIs of a given project in the current working directory
     */
    public static String getProjectTargetGenGrpcSrcOpenAPIsDirectory(String projectName) {
        return getProjectTargetGenGrpcSrcDirectory(projectName) + File.separator +
                CliConstants.GEN_GRPC_SRC_OPENAPI_DIR;
    }

    /**
     * Returns path to the /target/gen/gRPCSrc/desc of a given project in the current working directory.
     *
     * @param projectName name of the project
     * @return path to the /target/gen/gRPCSrc/desc of a given project in the current working directory
     */
    public static String getProjectTargetGenGrpcSrcDescDirectory(String projectName) {
        return getProjectTargetGenGrpcSrcDirectory(projectName) + File.separator + CliConstants.GEN_GRPC_SRC_DESC_DIR;
    }

    /**
     * Returns the path to ballerina project module inside of a given mgw project
     * in the current working directory.
     *
     * @param projectName name of the mgw project
     * @return path to ballerina project module
     */
    public static String getProjectTargetModulePath(String projectName) {
        return getProjectTargetGenDirectoryPath(projectName) + File.separator +
                CliConstants.GEN_SRC_DIR + File.separator + projectName;
    }

    /**
     * Returns path to the /gen/src of a given project in the current working directory
     *
     * @param projectName name of the project
     * @return path to the /src of a given project in the current working directory
     */
    public static String getProjectGenSrcDirectoryPath(String projectName) {
        return getProjectTargetGenDirectoryPath(projectName) + File.separator
                + CliConstants.GEN_SRC_DIR;
    }

    /**
     * Returns path to the /extensions of a given project in the current working directory
     *
     * @param projectName name of the project
     * @return path to the /extensions of a given project in the current working directory
     */
    public static String getProjectExtensionsDirectoryPath(String projectName) {
        return getProjectDirectoryPath(projectName) + File.separator + CliConstants.PROJECT_EXTENSIONS_DIR;
    }

    /**
     * Returns the path to mgw project's 'interceptors' directory.
     * Project should be located in the current directory.
     *
     * @param projectName name of the project
     * @return path to project interceptors directory
     */
    public static String getProjectInterceptorsPath(String projectName) {
        return getProjectDirectoryPath(projectName) + File.separator
                + CliConstants.PROJECT_INTERCEPTORS_DIR;
    }

    /**
     * Returns the path to target ballerina project's 'interceptors' directory, inside of a
     * given mgw project in the current working directory.
     *
     * @param projectName name of the project
     * @return path to target interceptors directory
     */
    public static String getProjectTargetInterceptorsPath(String projectName) {
        return getProjectTargetModulePath(projectName) + File.separator + CliConstants.PROJECT_INTERCEPTORS_DIR;
    }

    /**
     * Returns path to the /gen/api-definition of a given project in the current working directory
     *
     * @param projectName   name of the project
     * @param apiId         md5 hash value of apiName:apiVersion
     * @param extensionType The file extension type. (ex : yaml or json)
     * @return path to the /gen/api-definition of a given project in the current working directory
     */
    public static String getProjectGenSwaggerPath(String projectName, String apiId, String extensionType) {
        return getProjectDirectoryPath(projectName) + File.separator +
                CliConstants.PROJECT_GEN_DIR + File.separator +
                CliConstants.PROJECT_API_DEFINITIONS_DIR + File.separator + apiId
                + extensionType;
    }

    /**
     * Returns path to the /gen/api-definition of a given project in the current working directory
     *
     * @param projectName name of the project
     * @param apiId       md5 hash value of apiName:apiVersion
     * @return path to the /gen/api-definition of a given project in the current working directory
     */
    public static String getProjectGenSwaggerPath(String projectName, String apiId) {
        return getProjectDirectoryPath(projectName) + File.separator +
                CliConstants.PROJECT_GEN_DIR + File.separator +
                CliConstants.PROJECT_API_DEFINITIONS_DIR + File.separator + apiId
                + CliConstants.API_SWAGGER;
    }

    /**
     * Returns path to the /gen/api-definition of a given project in the current working directory.
     *
     * @param projectName name of the project
     * @return path to the /gen/api-definition of a given project in the current working directory
     */
    public static String getProjectGenAPIDefinitionPath(String projectName) {
        return getProjectDirectoryPath(projectName) + File.separator +
                CliConstants.PROJECT_GEN_DIR + File.separator +
                CliConstants.PROJECT_API_DEFINITIONS_DIR;
    }

    /**
     * Returns path to the /api-definition of a given project the current working directory.
     * @param projectName name of the project
     * @return path to the /api-definition of the given project in the current working directory
     */
    public static String getAPIDefinitionPath(String projectName) {
        return getProjectDirectoryPath(projectName) + File.separator + CliConstants.PROJECT_API_DEFINITIONS_DIR;

    }

    /**
     * Returns path to the /grpc_definitions of a given project in the current directory.
     *
     * @param projectName project name
     * @return path to the grpc_defintions of a given project in the current working directory
     */
    public static String getGrpcDefinitionsDirPath(String projectName) {
        return getProjectDirectoryPath(projectName) + File.separator +
                CliConstants.PROJECT_GRPC_DEFINITIONS_DIR;
    }

    /**
     * Returns path to the /grpc_service/client of a given project in the current working directory.
     *
     * @return path to the /grpc_service/client of a given project in the current working directory
     */
    public static String getProjectGrpcDirectoryPath() {
        return getUserDir() + File.separator
                + CliConstants.PROJECT_GRPC_SERVICE_DIR + File.separator +
                CliConstants.PROJECT_GRPC_CLIENT_DIR;
    }

    /**
     * Returns path to the /grpc_service of a given project in the current working directory.
     *
     * @return path to the /grpc_service of a given project in the current working directory
     */
    public static String getProjectGrpcSoloDirectoryPath() {
        return getUserDir() + File.separator
                + CliConstants.PROJECT_GRPC_SERVICE_DIR;
    }

    /**
     * This function recursively copy all the sub folder and files from source to destination file paths.
     *
     * @param source      source location
     * @param destination destination location
     * @throws IOException error while copying folder to destination
     */
    public static void copyFolder(String source, String destination) throws IOException {
        File sourceFolder = new File(source);
        File destinationFolder = new File(destination);
        copyFolder(sourceFolder, destinationFolder);
    }

    /**
     * This function deletes the existing destination folder and recursively copy all the sub folder and files from
     * source to destination file paths.
     *
     * @param source      source location
     * @param destination destination location
     * @throws IOException error while copying folder to destination
     */
    public static void copyAndReplaceFolder(String source, String destination) throws IOException {
        File sourceFolder = new File(source);
        File destinationFolder = new File(destination);
        if (destinationFolder.exists()) {
            FileUtils.deleteDirectory(destinationFolder);
        }
        copyFolder(sourceFolder, destinationFolder);
    }

    /**
     * Copy files to resources directory
     *
     * @param sourcePath      source directory path
     * @param destinationPath destionation directory path
     * @throws IOException if file copy went wrong
     */
    public static void copyFilesToSources(String sourcePath, String destinationPath) throws IOException {
        Files.copy(Paths.get(sourcePath), Paths.get(destinationPath), StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * This function recursively copy all the sub folder and files from sourceFolder to destinationFolder
     *
     * @param sourceFolder      source location
     * @param destinationFolder destination location
     * @throws IOException error while copying folder to destination
     */
    private static boolean copyFolder(File sourceFolder, File destinationFolder) throws IOException {
        if (sourceFolder == null || destinationFolder == null || !sourceFolder.exists()) {
            return false;
        }
        //Check if sourceFolder is a directory or file
        //If sourceFolder is file; then copy the file directly to new location
        if (sourceFolder.isDirectory()) {
            //Verify if destinationFolder is already present; If not then create it
            if (!destinationFolder.exists()) {
                boolean created = destinationFolder.mkdir();
                if (created) {
                    logger.trace("Directory: {} created. ", destinationFolder.getAbsolutePath());
                } else {
                    logger.error("Failed to create directory: {} ", destinationFolder.getAbsolutePath());
                    throw new CLIInternalException("Error occurred while setting up the workspace structure");
                }
            }

            //Get all files from source directory
            String[] files = sourceFolder.list();

            if (files != null) {
                //Iterate over all files and copy them to destinationFolder one by one
                for (String file : files) {
                    File srcFile = new File(sourceFolder, file);
                    File destFile = new File(destinationFolder, file);

                    //Recursive function call
                    copyFolder(srcFile, destFile);
                }
            }
        } else {
            //Copy the file content from one place to another
            Files.copy(sourceFolder.toPath(), destinationFolder.toPath(), StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.COPY_ATTRIBUTES);
            //Make it writable, so that next time can clear the files without a permission issue
            boolean success = destinationFolder.setWritable(true);
            if (!success) {
                logger.debug("Setting write permission failed for {}", destinationFolder.getAbsolutePath());
            }
        }

        return true;
    }

    /**
     * Creates a new folder if not exists
     *
     * @param path folder path
     * @param overwrite if `true` existing directory will be removed
     *                  and new directory will be created in {@code path}.
     * @return created directory
     * @throws IOException Failed delete or create the directory in {@code path}
     */
    public static File createDirectory(String path, boolean overwrite) throws IOException {
        File dir = new File(path);
        if (overwrite && dir.exists() && dir.isDirectory()) {
            FileUtils.deleteDirectory(dir);
        }

        if (!dir.exists() && !dir.isDirectory()) {
            boolean created = dir.mkdir();
            if (created) {
                logger.trace("Directory: {} created. ", path);
            } else {
                throw new CLIInternalException("Failed to create directory: " + path);
            }
        }

        return dir;
    }

    /**
     * Create new file in a given location.
     *
     * @param path location of the new file.
     * @param overwrite if `true` existing file with the same name will be replaced.
     * @param fileName name of the new file.
     * @return created file
     * @throws IOException Failed delete or create the file in {@code path}
     */
    public static File createFile(String path, String fileName, boolean overwrite) throws IOException {
        String filePath = path + File.separator + fileName;
        File file = new File(filePath);
        if (overwrite && file.exists() && file.isFile()) {
            boolean isDeleted = file.delete();
            if (!isDeleted) {
                throw new CLIInternalException("Failed to overwrite file: " + filePath);
            }
        }

        if (!file.exists() && !file.isFile()) {
            boolean isCreated = file.createNewFile();
            if (isCreated) {
                logger.trace("File: {} created.", filePath);
            } else {
                throw new CLIInternalException("Failed to create file: " + filePath);
            }
        }

        return file;
    }

    /**
     * Write content to a specified file
     *
     * @param content content to be written
     * @param file    file object initialized with path
     * @throws IOException error while writing content to file
     */
    public static void writeContent(String content, File file) throws IOException {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(content);
            writer.flush();
        }
    }

    /**
     * Create initial deployment configuration file. If external file is provided using 'deploymentConfPath',
     * it will be taken as the deployment configuration. Otherwise, a default configuration will be copied.
     *
     * @param projectName        project name
     * @param deploymentConfPath path to deployment config
     * @throws IOException if file create went wrong
     */
    public static void createDeploymentConfig(String projectName, String deploymentConfPath)
            throws IOException {

        String depConfig = getProjectConfigDirPath(projectName) + File.separator
                + CliConstants.DEPLOYMENT_CONFIG_FILE_NAME;
        File file = new File(depConfig);

        if (deploymentConfPath == null) {
            if (!file.exists()) {
                String defaultConfig;
                boolean created = file.createNewFile();
                if (created) {
                    logger.debug("Deployment configuration file: {} created.", depConfig);
                } else {
                    throw new CLIInternalException("Failed to create the deployment configuration file: " + depConfig);
                }
                //Write Content
                defaultConfig = readFileAsString(CliConstants.DEFAULT_DEPLOYMENT_CONFIG_FILE_NAME, true);
                writeContent(defaultConfig, file);
            }
        } else {
            File inputDepConfFile = new File(deploymentConfPath);
            if (inputDepConfFile.exists()) {
                String inputConfigContent = readFileAsString(deploymentConfPath, false);
                //validate the provided file
                try {
                    TOMLConfigParser.parse(deploymentConfPath, ContainerConfig.class);
                } catch (ConfigParserException | IllegalStateException e) {
                    throw new CLIRuntimeException(
                            "Error while reading deployment configuration file: " + deploymentConfPath
                                    + ". The content is invalid.", e);
                }
                writeContent(inputConfigContent, file);
            } else {
                throw new CLIRuntimeException(
                        "Error while reading deployment configuration file. Probably the file path '"
                                + deploymentConfPath + "' is invalid.");
            }
        }
    }

    /**
     * Download file.
     *
     * @param source source URL
     * @param destination destination path to save file
     * @param headers api definition download request headers
     * @param values api definition download values for request headers
     */
    private static String downloadFile(String source, String destination, ArrayList<String> headers,
                                     ArrayList<String> values, boolean insecure) throws IOException {
        URL url;
        if (insecure) {
            useInsecureSSL();
        }
        HttpURLConnection urlConn = null;
        FileOutputStream outputStream = null;
        String saveFilePath;
        try {
            url = new URL(source);
            urlConn = (HttpURLConnection) url.openConnection();
            urlConn.setRequestMethod(HttpGet.METHOD_NAME);
            if (headers.size() > 0) {
                for (int i = 0; i < headers.size(); i++) {
                    urlConn.setRequestProperty(headers.get(i), values.get(i));
                }
            }
            int responseCode = urlConn.getResponseCode();
            if (responseCode == HttpStatus.SC_OK) {
                String fileName = "";
                String disposition = urlConn.getHeaderField("Content-Disposition");

                if (disposition != null) {
                    // extracts file name from header field
                    int index = disposition.indexOf("filename=");
                    if (index > 0) {
                        fileName = disposition.substring(index + 10, disposition.length() - 1);
                    }
                } else {
                    // extracts file name from URL
                    fileName = source.substring(source.lastIndexOf("/") + 1);
                }
                // opens input stream from the HTTP connection
                InputStream inputStream = urlConn.getInputStream();
                saveFilePath = destination + File.separator + fileName;
                // opens an output stream to save into file
                outputStream = new FileOutputStream(saveFilePath);

                int bytesRead;
                byte[] buffer = new byte[1024];
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                inputStream.close();
            } else {
                throw new CLIInternalException("Error occurred while downloading file. Status code: " + responseCode);
            }
        } catch (IOException e) {
            throw new CLIRuntimeException("Error while downloading file from " + source + ": " + e.getMessage());
        } finally {
            if (outputStream != null) {
                outputStream.close();
            }
            if (urlConn != null) {
                urlConn.disconnect();
            }
        }
        return saveFilePath;
    }

    public static ContainerConfig getContainerConfig() {
        return containerConfig;
    }

    public static void setContainerConfig(ContainerConfig containerConfig) {
        overrideContainerConfigs(containerConfig);
        CmdUtils.containerConfig = containerConfig;
    }

    private static void overrideContainerConfigs(ContainerConfig containerConfig) {
        if (containerConfig != null && containerConfig.getKubernetes() != null
                && containerConfig.getKubernetes().getSecureKubernetesIngress() != null) {
            containerConfig.getKubernetes().getSecureKubernetesIngress().setKeyStorePassword(CodegenUtils
                    .resolveValue(containerConfig.getKubernetes().getSecureKubernetesIngress().getKeyStorePassword()));
        }
    }

    public static void saveConfig(Config config, String configPath) {
        try {
            TOMLConfigParser.write(configPath, config);
        } catch (ConfigParserException e) {
            ERR.println("Error occurred while parsing configuration, when persisting.");
        }
    }

    public static APICorsConfigurationDTO getDefaultCorsConfig() {
        APICorsConfigurationDTO corsConfigurationDTO = new APICorsConfigurationDTO();
        corsConfigurationDTO.setCorsConfigurationEnabled(true);
        corsConfigurationDTO.setAccessControlAllowOrigins(CliConstants.ACCESS_CONTROL_ALLOW_ORIGINS);
        corsConfigurationDTO.setAccessControlAllowMethods(CliConstants.ACCESS_CONTROL_ALLOW_METHODS);
        corsConfigurationDTO.setAccessControlAllowHeaders(CliConstants.ACCESS_CONTROL_ALLOW_HEADERS);
        corsConfigurationDTO.setAccessControlAllowCredentials(CliConstants.ACCESS_CONTROL_ALLOW_CREDENTIALS);
        return corsConfigurationDTO;
    }

    /**
     * Replace backslashes `\` in windows path string with forward slashes `/`
     *
     * @param path Location of a resource (file or directory)
     * @return {String} File path with unix style file separator
     */
    public static String getUnixPath(String path) {
        return path.replace(File.separator, "/");
    }

    /**
     * Delete project folder
     *
     * @param projectPath project path
     */
    public static void deleteProject(String projectPath) {
        File file = new File(projectPath);
        try {
            FileUtils.deleteDirectory(file);
        } catch (IOException e) {
            // not throwing the error because deleting failed project is not
            // a critical task. This can be deleted manually if not used.
            logger.error("Failed to delete project : {} ", projectPath, e);
        }
    }

    /**
     * Prompts for a test input.
     *
     * @param outStream Print Stream
     * @param msg       message
     * @return user entered text
     */
    public static String promptForTextInput(PrintStream outStream, String msg) {
        outStream.println(msg);
        return System.console().readLine();
    }

    /**
     * Returns path to the /API-Files of a given project in the current working directory
     *
     * @param projectName name of the project
     * @return path to the /API-Files of a given project in the current working directory
     */
    public static String getProjectAPIFilesDirectoryPath(String projectName) {
        return getProjectDirectoryPath(projectName) + File.separator +
                CliConstants.PROJECT_API_DEFINITIONS_DIR;
    }

    /**
     * Returns the path to the swagger for a defined version of an API
     *
     * @param projectName name of the project
     * @param apiId       md5 hash value of apiName:apiVersion
     * @return path to the swagger for a defined version of an API
     */
    public static String getProjectSwaggerFilePath(String projectName, String apiId) {
        return getProjectAPIFilesDirectoryPath(projectName) + File.separator + apiId + File.separator +
                CliConstants.API_SWAGGER;
    }

    /**
     * get the path of the protoc executable.
     *
     * @return the absolute path of the protoc executable
     */
    public static String getProtocDirPath() {
        return getResourceFolderLocation() + File.separator + CliConstants.RESOURCES_GRPC_DIR;
    }

    /**
     * descriptor path of the grpc definition.
     *
     * @param projectName   project name
     * @param protoFileName protobuf file name
     * @return descriptor path
     */
    public static String getProtoDescriptorPath(String projectName, String protoFileName) {
        String fileName = protoFileName.substring(0, protoFileName.length() - 6);
        return getProjectTargetGenGrpcSrcDescDirectory(projectName) + File.separator + fileName + ".desc";
    }

    /**
     * To print the message if verbose flag is set
     *
     * @param msg Message
     */
    public static void printVerbose(String msg) {
        if (verboseLogsEnabled) {
            OUT.println("micro-gw: [verbose] " + msg);
        }
    }

    /**
     * To read the system variable VERBOSE_ENABLED
     *
     * @return true if verbose flag is enabled
     */
    private static Boolean setVerboseEnabled() {
        String value = System.getProperty("VERBOSE_ENABLED");
        //bat file provides T and shell script provides true
        return value != null && (value.equals("T") || value.equalsIgnoreCase("true"));
    }

    /**
     * method use to trust all certificates using insecure ssl.
     */
    public static void useInsecureSSL() {

        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        SSLContext context;
        try {
            context = SSLContext.getInstance(TokenManagementConstants.TLS);
            context.init(null, new X509TrustManager[] { new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }

                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new java.security.cert.X509Certificate[0];
                }
            } }, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            String message = "Error while setting in secure ssl options";
            logger.error(message, e);
        }
    }

    public static String getMicroGWConfResourceLocation() {
        return getCLIHome() + File.separator + CliConstants.GW_DIST_RESOURCES + File.separator
                + CliConstants.GW_DIST_CONF;
    }

    public static List<String> getExternalJarDependencies(String projectName) {
        List<String> jars = new ArrayList<>();
        String extJarDir = getProjectDirectoryPath(projectName) + File.separator + CliConstants.CLI_LIB;
        File[] jarFiles = new File(extJarDir).listFiles();
        if (jarFiles != null) {
            for (File file : jarFiles) {
                if (file.getName().endsWith(CliConstants.EXTENSION_JAR)) {
                    jars.add(file.getAbsolutePath());
                }
            }
        }
        return jars;
    }

    /**
     * Formats a message based on the type of OS. On windows there will not
     * be any formatting since Windows CMD may not output ASCII formatting.
     * On other OSs message will be printed in bold.
     *
     * @param message message to be printed in the console
     */
    public static String format(String message) {
        if (SystemUtils.IS_OS_WINDOWS) {
            return message;
        } else {
            return "\033[0;1m" + message + "\033[0m";
        }
    }

    public static void appendMessagesToConsole(String msg) {
        consoleMessages += msg;
    }

    public static void printMessagesToConsole() {
        if (!"".equals(consoleMessages)) {
            OUT.println(consoleMessages);
        }
    }

    public static void setCallHomeMessage(String message) {
        callHomeMessage = message;
    }

    public static void printCallHomeMessage() {
        if (!"".equals(callHomeMessage)) {
            OUT.println(callHomeMessage);
        }
    }

}
