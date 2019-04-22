package org.wso2.apimgt.gateway.cli.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.wso2.apimgt.gateway.cli.constants.GatewayCliConstants;
import org.wso2.apimgt.gateway.cli.exception.CLIInternalException;
import org.wso2.apimgt.gateway.cli.exception.CLIRuntimeException;
import org.wso2.apimgt.gateway.cli.hashing.HashUtils;
import org.wso2.apimgt.gateway.cli.model.rest.APIEndpointSecurityDTO;
import org.wso2.apimgt.gateway.cli.model.rest.APIMetadataDTO;
import org.wso2.apimgt.gateway.cli.model.rest.ClientCertMetadataDTO;
import org.wso2.apimgt.gateway.cli.model.rest.ext.ExtendedAPI;
import org.wso2.apimgt.gateway.cli.model.rest.policy.ApplicationThrottlePolicyDTO;
import org.wso2.apimgt.gateway.cli.model.rest.policy.ApplicationThrottlePolicyListDTO;
import org.wso2.apimgt.gateway.cli.model.rest.policy.SubscriptionThrottlePolicyDTO;
import org.wso2.apimgt.gateway.cli.model.rest.policy.SubscriptionThrottlePolicyListDTO;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class JsonProcessingUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final ObjectMapper objectMapper_yaml = new ObjectMapper(new YAMLFactory());

    //todo: decide whether we allow users to provide endpoint security details as a separate JSON
    /**
     * parse endpoint security configurations in json format {type: , name: , password: }
     * @param endpointSecurityString endpoint security definition in json
     * @return  APIEndpointSecurityDTO object (for the purpose of routes configuration file)
     */
    private static APIEndpointSecurityDTO parseEndpointSecurityDefinition(String endpointSecurityString){
        APIEndpointSecurityDTO endpointSecurity = null;
        if(endpointSecurityString != null){
            try {
                endpointSecurity = objectMapper.readValue(endpointSecurityString, APIEndpointSecurityDTO.class);
            } catch (IOException e) {
                throw new CLIInternalException("Error: endpoint security string cannot be parsed ");
            }
        }
        return endpointSecurity;
    }

    /**
     * Save subscription throttle policies in JSON format
     *
     * @param projectName project name
     * @param list        subscription throttle policies list
     */
    public static void saveSubscriptionThrottlePolicies(String projectName, List<SubscriptionThrottlePolicyDTO> list) {
        SubscriptionThrottlePolicyListDTO policyList = new SubscriptionThrottlePolicyListDTO();
        policyList.setList(list);
        try {
            objectMapper.writeValue(new File(GatewayCmdUtils.getProjectSubscriptionThrottlePoliciesFilePath(projectName)),
                    policyList);
        } catch (JsonProcessingException e) {
            throw new CLIInternalException("Error: Cannot parse the SubscriptionThrottlePolicies Object to json");
        } catch (IOException e) {
            throw new CLIInternalException("Error: cannot write to the file : " +
                    GatewayCliConstants.SUBSCRIPTION_THROTTLE_POLICIES_FILE);
        }
    }

    /**
     * Save application throttle policies in JSON format
     *
     * @param projectName project name
     * @param list        application throttle policies list
     */
    public static void saveApplicationThrottlePolicies(String projectName, List<ApplicationThrottlePolicyDTO> list) {
        ApplicationThrottlePolicyListDTO policyList = new ApplicationThrottlePolicyListDTO();
        policyList.setList(list);
        try {
            objectMapper.writeValue(new File(GatewayCmdUtils.getProjectAppThrottlePoliciesFilePath(projectName)),
                    policyList);
        } catch (JsonProcessingException e) {
            throw new CLIInternalException("Error: Cannot parse the ApplicationThrottlePolicies Object to json");
        } catch (IOException e) {
            throw new CLIInternalException("Error: cannot write to the file : " +
                    GatewayCliConstants.APPLICATION_THROTTLE_POLICIES_FILE);
        }
    }

    /**
     * Save the client certification metadata in JSON format
     *
     * @param projectName  project name
     * @param metadataList client certification metadata list
     */
    public static void saveClientCertMetadata(String projectName, List<ClientCertMetadataDTO> metadataList) {
        try {
            objectMapper.writeValue(new File(GatewayCmdUtils.getProjectClientCertMetadataFilePath(projectName)),
                    metadataList);
        } catch (JsonProcessingException e) {
            throw new CLIInternalException("Error: Cannot parse the ApplicationThrottlePolicies Object to json");
        } catch (IOException e) {
            throw new CLIInternalException("Error: cannot write to the file : " +
                    GatewayCliConstants.CLIENT_CERT_METADATA_FILE);
        }
    }

    private static void saveAPIMetadata(String projectName, ExtendedAPI api, String security) {
        APIMetadataDTO metaData = new APIMetadataDTO();
        metaData.setCorsConfigurationDTO(api.getCorsConfiguration());
        metaData.setSecurity(security);

        String apiId = HashUtils.generateAPIId(api.getName(), api.getVersion());
        try {
            String yamlString = objectMapper_yaml.writeValueAsString(metaData);
            GatewayCmdUtils.saveAPIMetadataFile(projectName, apiId, yamlString);
        } catch (JsonProcessingException e) {
            throw new CLIInternalException("Error while mapping metadata object to a yaml string");
        }
    }

    /**
     * Save Security scheme, CORS configuration as metadata for a given list of APIs.
     *
     * @param projectName project name
     * @param apis        List of API objects
     * @param security    Security type
     */
    public static void saveAPIMetadataForMultipleAPIs(String projectName, List<ExtendedAPI> apis, String security) {
        for (ExtendedAPI api : apis) {
            saveAPIMetadata(projectName, api, security);
        }
    }

    /**
     * Save Security scheme as metadata for a given list of APIs. (in developer first approach)
     *
     * @param projectName project name
     * @param apiId       List of API objects
     * @param security    Security type
     */
    public static void saveAPIMetadata(String projectName, String apiId, String security) {
        APIMetadataDTO metaData = new APIMetadataDTO();
        metaData.setSecurity(security);
        try {
            String yamlString = objectMapper_yaml.writeValueAsString(metaData);
            GatewayCmdUtils.saveAPIMetadataFile(projectName, apiId, yamlString);
        } catch (JsonProcessingException e) {
            throw new CLIInternalException("Error while mapping metadata object to a yaml string");
        }
    }

    /**
     * get API metadata object for a given API.
     *
     * @param projectName project Name
     * @param apiId       API ID
     * @return API metadata object
     */
    static APIMetadataDTO getAPIMetadata(String projectName, String apiId) {
        try {
            return objectMapper_yaml.readValue(
                    new File(GatewayCmdUtils.getAPIMetadataFilePath(projectName, apiId)), APIMetadataDTO.class);
        } catch (IOException e) {
            throw new CLIRuntimeException("Error while parsing the API metadata file");
        }

    }

    /**
     * Returns the path to the application-throttle-policies.yaml file for a defined version of an API
     * @param projectName name of the project
     * @return path to the application-throttle-policies.yaml file for a defined version of an API
     */
    public static String getProjectSubscriptionThrottlePoliciesFilePath(String projectName){
        return getProjectAPIFilesDirectoryPath(projectName) + File.separator +
                GatewayCliConstants.SUBSCRIPTION_THROTTLE_POLICIES_FILE;
    }


    /**
     * Returns the path to the client-cert-metadata.yaml for a defined version of an API
     * @param projectName name of the project
     * @return path to the client-cert-metadata.yaml for a defined version of an API
     */
    public static String getProjectClientCertMetadataFilePath(String projectName){
        return getProjectAPIFilesDirectoryPath(projectName) + File.separator +
                GatewayCliConstants.CLIENT_CERT_METADATA_FILE;
    }

    /**
     * Returns the path to the routes configuration file (routes.yaml)
     * @param projectName name of the project
     * @return path to the client-cert-metadata.yaml for a defined version of an API
     */
    public static String getProjectRoutesConfFilePath(String projectName){
        return getProjectDirectoryPath(projectName) + File.separator + GatewayCliConstants.PROJECT_DEFINITION_FILE;
    }

    /**
     * Returns path to the given project in the current working directory
     *
     * @param projectName name of the project
     * @return path to the given project in the current working directory
     */
    public static String getProjectDirectoryPath(String projectName) {
        // TODO: do we need to change this?
        return new File(projectName).getAbsolutePath();
    }

    /**
     * Returns path to the /API-Files of a given project in the current working directory
     * @param projectName name of the project
     * @return path to the /API-Files of a given project in the current working directory
     */
    public static String getProjectAPIFilesDirectoryPath(String projectName){
        return getProjectDirectoryPath(projectName) + File.separator +
                GatewayCliConstants.PROJECT_API_DEFINITIONS_DIR;
    }

    public static void saveAPIMetadataFile(String projectName, String apiId, String apiMetadataYaml){
        if(!apiMetadataYaml.isEmpty()){
            try {
                writeContent(apiMetadataYaml, new File(GatewayCmdUtils.getAPIMetadataFilePath(projectName, apiId)));
            } catch (IOException e) {
                throw new CLIInternalException("Error while copying api-metaData to the project directory");
            }
        }
    }

    /**
     * Write content to a specified file
     *
     * @param content content to be written
     * @param file    file object initialized with path
     * @throws IOException error while writing content to file
     */
    private static void writeContent(String content, File file) throws IOException {
        FileWriter writer = null;
        writer = new FileWriter(file);
        writer.write(content);
        writer.flush();
    }



}
