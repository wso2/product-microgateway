package org.wso2.apimgt.gateway.cli.protobuf;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.ExtensionRegistry;
import io.swagger.v3.oas.models.OpenAPI;
import org.wso2.apimgt.gateway.cli.model.route.EndpointListRouteDTO;
import org.wso2.apimgt.gateway.cli.model.route.EndpointType;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * Class for generate file descriptors for proto files and create OpenAPI objects out of those descriptors.
 */
public class ProtobufParser {

    /**
     * Compile the protobuf and generate descriptor file.
     *
     * @param exePath        protoc.exe path
     * @param protoPath      protobuf file path
     * @param descriptorPath descriptor file path
     * @return {@link DescriptorProtos.FileDescriptorSet} object
     */
    public static DescriptorProtos.FileDescriptorProto generateRootFileDescriptor(String exePath, String protoPath,
                                                                                  String descriptorPath) {
        String command = new ProtocCommandBuilder
                (exePath, protoPath, resolveProtoFolderPath(protoPath), descriptorPath).build();
        generateDescriptor(command);
        File initialFile = new File(descriptorPath);
        try (InputStream targetStream = new FileInputStream(initialFile)) {
            ExtensionRegistry extensionRegistry = ExtensionRegistry.newInstance();
            //to register all custom extensions in order to parse properly
            ExtensionHolder.registerAllExtensions(extensionRegistry);
            DescriptorProtos.FileDescriptorSet set = DescriptorProtos.FileDescriptorSet.parseFrom(targetStream,
                    extensionRegistry);
            if (set.getFileList().size() > 0) {
                return set.getFile(0);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading generated descriptor file '" + descriptorPath + "'.", e);
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
            throw new RuntimeException("Error in executing protoc command '" + command + "'.", e);
        }
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException("Process not successfully completed. Process is interrupted while" +
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
                throw new RuntimeException(errMsg.toString());
            } catch (IOException e) {
                throw new RuntimeException("Invalid command syntax.", e);
            }
        }
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
     * @param descriptor
     * @return
     */
    public static OpenAPI generateOpenAPI(DescriptorProtos.FileDescriptorProto descriptor) {
        if (descriptor == null) {
            throw new RuntimeException("descriptor is not available");
        }
        ProtoOpenAPIGenerator protoOpenAPIGenerator = new ProtoOpenAPIGenerator();
        descriptor.getServiceList().forEach(service -> {
            protoOpenAPIGenerator.addOpenAPIInfo(descriptor.getPackage() + "." + service.getName());
            //set endpoint configurations
            protoOpenAPIGenerator.addAPIProdEpExtension(generateEpList(service.getOptions()
                    .getExtension(ExtensionHolder.xWso2ProductionEndpoints)));
            protoOpenAPIGenerator.addAPISandEpExtension(generateEpList(service.getOptions()
                    .getExtension(ExtensionHolder.xWso2SandboxEndpoints)));
            //set API level security
            List<ExtensionHolder.Security> securityList = service.getOptions()
                    .getExtension(ExtensionHolder.xWso2Security);
            if (securityList.contains(ExtensionHolder.Security.NONE)) {
                protoOpenAPIGenerator.disableAPISecurity();
            }
            if (securityList.contains(ExtensionHolder.Security.BASIC)) {
                protoOpenAPIGenerator.addAPIBasicSecurityRequirement();
            }
            if (securityList.contains(ExtensionHolder.Security.OAUTH2) ||
                    securityList.contains(ExtensionHolder.Security.JWT)) {
                protoOpenAPIGenerator.addAPIOauth2SecurityRequirement();
            }
            //set API level throttling tier
            String throttlingTier = service.getOptions().getExtension(ExtensionHolder.xWso2ThrottlingTier);
            protoOpenAPIGenerator.setAPIThrottlingTier(throttlingTier);
            service.getMethodList().forEach(method -> {
                //set operation level scopes and throttling tiers
                String methodScopesString = method.getOptions().getExtension(ExtensionHolder.xWso2MethodScopes);
                String methodThrottlingTier = method.getOptions()
                        .getExtension(ExtensionHolder.xWso2MethodThrottlingTier);
                protoOpenAPIGenerator.addOpenAPIPath(method.getName(), methodScopesString.split(","),
                        methodThrottlingTier);
            });
        });
        return protoOpenAPIGenerator.getOpenAPI();
    }

    /**
     * Convert protobuf endpoint configuration ({@link ExtensionHolder.Endpoints}) to the openAPI based endpoint
     * configuration. ({@link EndpointListRouteDTO})
     *
     * @param protoEps {@link ExtensionHolder.Endpoints} object.
     * @return {@link EndpointListRouteDTO} object.
     */
    private static EndpointListRouteDTO generateEpList(ExtensionHolder.Endpoints protoEps) {
        EndpointListRouteDTO epList = new EndpointListRouteDTO();

        protoEps.getEndpointList().forEach(endpoint -> epList.addEndpoint(endpoint.getUrl()));
        if (protoEps.getType() == ExtensionHolder.EndpointType.FAILOVER) {
            epList.setType(EndpointType.failover);
        }
        if (epList.getEndpoints() == null) {
            return null;
        }
        return epList;
    }
}
