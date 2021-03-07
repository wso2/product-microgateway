/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.apimgt.gateway.cli.protobuf;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.ExtensionRegistry;
import io.swagger.v3.oas.models.OpenAPI;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.cli.exception.CLICompileTimeException;
import org.wso2.apimgt.gateway.cli.exception.CLIInternalException;
import org.wso2.apimgt.gateway.cli.exception.CLIRuntimeException;
import org.wso2.apimgt.gateway.cli.model.route.EndpointListRouteDTO;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Class for generate file descriptors for proto files and create OpenAPI objects out of those descriptors.
 */
public class ProtobufParser {
    private static final Logger logger = LoggerFactory.getLogger(ProtobufParser.class);
    /**
     * Compile the protobuf and generate descriptor file.
     *
     * @param protoPath      protobuf file path
     * @param descriptorPath descriptor file path
     * @return {@link DescriptorProtos.FileDescriptorSet} object
     */
    private static DescriptorProtos.FileDescriptorProto generateRootFileDescriptor(String protoPath,
                                                                                  String descriptorPath) {
        String command = new ProtocCommandBuilder
                (protoPath, resolveProtoFolderPath(protoPath), descriptorPath).build();
        generateDescriptor(command);
        File initialFile = new File(descriptorPath);
        try (InputStream targetStream = new FileInputStream(initialFile)) {
            ExtensionRegistry extensionRegistry = ExtensionRegistry.newInstance();
            //to register all custom extensions in order to parse properly
            ExtensionHolder.registerAllExtensions(extensionRegistry);
            DescriptorProtos.FileDescriptorSet set = DescriptorProtos.FileDescriptorSet.parseFrom(targetStream,
                    extensionRegistry);
            logger.debug("Descriptor file is parsed successfully. file:" , descriptorPath);
            if (set.getFileList().size() > 0) {
                return set.getFile(0);
            }
        } catch (IOException e) {
            throw new CLIInternalException("Error reading generated descriptor file '" + descriptorPath + "'.", e);
        }
        return null;
    }

    /**
     * Execute command and generate file descriptor.
     *
     * @param command protoc executor command.
     */
    private static void generateDescriptor(String command) {
        boolean isWindows = System.getProperty("os.name")
                .toLowerCase(Locale.ENGLISH).startsWith("windows");
        ProcessBuilder builder = new ProcessBuilder();
        if (isWindows) {
            builder.command("cmd.exe", "/c", command);
        } else {
            builder.command("sh", "-c", command);
        }
        builder.directory(new File(System.getProperty("user.home")));
        Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            throw new CLIInternalException("Error in executing protoc command '" + builder.command() + "'.", e);
        }
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            throw new CLIRuntimeException("Process is not completed. Process is interrupted while" +
                    " running the protoc executor.", e);
        }
        if (process.exitValue() != 0) {
            try (BufferedReader bufferedReader = new BufferedReader(new
                    InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String err;
                StringBuilder errMsg = new StringBuilder();
                while ((err = bufferedReader.readLine()) != null) {
                    errMsg.append(System.lineSeparator()).append(err);
                }
                throw new CLIRuntimeException(errMsg.toString());
            } catch (IOException e) {
                throw new CLIInternalException("Invalid command syntax.", e);
            }
        }
        logger.debug("Descriptor file is generation command : \"" + command + "\" is executed successfully.");
    }

    /**
     * Resolve proto folder path from Proto file path.
     *
     * @param protoPath Proto file path
     * @return Parent folder path of proto file.
     */
    private static String resolveProtoFolderPath(String protoPath) {
        int idx = protoPath.lastIndexOf(File.separator);
        String protoFolderPath = "";
        if (idx > 0) {
            protoFolderPath = protoPath.substring(0, idx);
        }
        return protoFolderPath;
    }

    /**
     * Generate OpenAPI object for the {@link DescriptorProtos.FieldDescriptorProto}.
     *
     * @param descriptor file descriptor of the protobuf
     * @return {@link OpenAPI} arraylist of openAPIs
     */
    private static ArrayList<OpenAPI> generateOpenAPIFromProto(DescriptorProtos.FileDescriptorProto descriptor,
                                                               String protoPath) {
        if (descriptor == null) {
            throw new CLIInternalException("descriptor is not available");
        }
        if (descriptor.getServiceCount() == 0) {
            return null;
        }
        ArrayList<OpenAPI> openAPIS = new ArrayList<>();
        descriptor.getServiceList().forEach(service -> {
            ProtoOpenAPI protoOpenAPI = new ProtoOpenAPI();
            if (StringUtils.isEmpty(descriptor.getPackage())) {
                protoOpenAPI.addOpenAPIInfo(service.getName());
            } else {
                protoOpenAPI.addOpenAPIInfo(descriptor.getPackage() + "." + service.getName());
            }

            //set endpoint configurations
            protoOpenAPI.addAPIProdEpExtension(generateEpList(service.getOptions()
                    .getExtension(ExtensionHolder.productionEndpoints), service.getName()));
            protoOpenAPI.addAPISandEpExtension(generateEpList(service.getOptions()
                    .getExtension(ExtensionHolder.sandboxEndpoints), service.getName()));
            //set API level security
            List<ExtensionHolder.Security> securityList = service.getOptions()
                    .getExtension(ExtensionHolder.security);
            //if nothing is mentioned regarding the security, None will be selected.
            if (securityList.size() == 0) {
                protoOpenAPI.disableAPISecurity();
            } else {
                //all the security items are added and the validation happens when retrieving the openAPI object
                if (securityList.contains(ExtensionHolder.Security.NONE)) {
                    protoOpenAPI.disableAPISecurity();
                }
                if (securityList.contains(ExtensionHolder.Security.BASIC)) {
                    protoOpenAPI.addAPIBasicSecurityRequirement();
                }
                if (securityList.contains(ExtensionHolder.Security.OAUTH2) ||
                        securityList.contains(ExtensionHolder.Security.JWT)) {
                    protoOpenAPI.addAPIOauth2SecurityRequirement();
                }
                if (securityList.contains(ExtensionHolder.Security.APIKEY)) {
                    protoOpenAPI.addAPIKeySecurityRequirement();
                }
            }
            //set API level throttling tier
            String throttlingTier = service.getOptions().getExtension(ExtensionHolder.throttlingTier);
            protoOpenAPI.setAPIThrottlingTier(throttlingTier);
            service.getMethodList().forEach(method -> {
                //set operation level scopes and throttling tiers
                String methodScopesString = method.getOptions().getExtension(ExtensionHolder.methodScopes);
                String methodThrottlingTier = method.getOptions()
                        .getExtension(ExtensionHolder.methodThrottlingTier);
                String[] methodScopes = null;
                if (!methodScopesString.isEmpty()) {
                    methodScopes = methodScopesString.split(",");
                }
                protoOpenAPI.addOpenAPIPath(method.getName(), methodScopes, methodThrottlingTier);
            });
            openAPIS.add(protoOpenAPI.getOpenAPI(service.getName()));
        });
        return openAPIS;
    }

    /**
     * Generate OpenAPI from protobuf.
     *
     * @param protoPath      protobuf file path
     * @param descriptorPath descriptor file path
     * @return {@link OpenAPI} list of OpenAPIs
     */
    public ArrayList<OpenAPI> generateOpenAPI(String protoPath, String descriptorPath) {
        DescriptorProtos.FileDescriptorProto descriptor =
                generateRootFileDescriptor(protoPath, descriptorPath);
        return generateOpenAPIFromProto(descriptor, protoPath);
    }

    /**
     * Convert protobuf endpoint configuration ({@link ExtensionHolder.Endpoints}) to the openAPI based endpoint
     * configuration ({@link EndpointListRouteDTO}).
     *
     * @param protoEps {@link ExtensionHolder.Endpoints} object.
     * @return {@link EndpointListRouteDTO} object.
     */
    private static EndpointListRouteDTO generateEpList(ExtensionHolder.Endpoints protoEps, String service) {
        EndpointListRouteDTO epList = new EndpointListRouteDTO();

        protoEps.getUrlList().forEach(epList::addEndpoint);
        try {
            epList.validateEndpoints();
        } catch (CLICompileTimeException e) {
            //todo: etcd setup needs to be tested for the protobuf scenario
            throw new CLIRuntimeException("The provided endpoint string for the gRPC \"" + service +
                    "\" is invalid.\n\t-" + e.getTerminalMsg(), e);
        }
        if (epList.getEndpoints() == null) {
            return null;
        }
        if (epList.getEndpoints().size() > 1) {
            throw new CLIRuntimeException("Multiple endpoints are not supported .service :" + service + ".");
        }
        return epList;
    }
}
