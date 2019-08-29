package org.wso2.apimgt.gateway.cli.protobuf;

/**
 * This class is used to build the protoc compiler command to generate descriptor.
 */
public class ProtocCommandBuilder {
    private static final String EXE_PATH_PLACEHOLDER = "{{exe_file_path}}";
    private static final String PROTO_PATH_PLACEHOLDER = "{{proto_file_path}}";
    private static final String PROTO_FOLDER_PLACEHOLDER = "{{proto_folder_path}}";
    private static final String DESC_PATH_PLACEHOLDER = "{{desc_file_path}}";
    private static final String COMMAND_PLACEHOLDER = "{{exe_file_path}} --proto_path={{proto_folder_path}} " +
            "{{proto_file_path}} --descriptor_set_out={{desc_file_path}}";
    private String exePath;
    private String protoPath;
    private String protoFolderPath;
    private String descriptorSetOutPath;

    ProtocCommandBuilder(String exePath, String protoPath, String protofolderPath, String descriptorSetOutPath) {
        this.exePath = exePath;
        this.protoPath = protoPath;
        this.descriptorSetOutPath = descriptorSetOutPath;
        this.protoFolderPath = protofolderPath;
    }

    /**
     * Build the command to generate descriptor by compiling the protobuf file using protoc.exe
     *
     * @return the command to generate descriptor
     */
    public String build() {
        return COMMAND_PLACEHOLDER.replace(EXE_PATH_PLACEHOLDER, exePath)
                .replace(PROTO_PATH_PLACEHOLDER, protoPath)
                .replace(DESC_PATH_PLACEHOLDER, descriptorSetOutPath)
                .replace(PROTO_FOLDER_PLACEHOLDER, protoFolderPath);
    }
}
