package org.wso2.apimgt.gateway.cli.protobuf;

import java.io.File;

/**
 * protobuf related constants
 */
public class ProtobufConstants {
    public static final String OS_NAME_SYSTEM_PROPERTY = "os.name";
    public static final String OS_ARCH_SYSTEM_PROPERTY = "os.arch";
    public static final String BUILD_COMMAND_NAME = "build";
    public static final String META_LOCATION = "desc_gen";
    public static final String TEMP_GOOGLE_DIRECTORY = "google";
    public static final String TEMP_PROTOBUF_DIRECTORY = "protobuf";
    public static final String TEMP_COMPILER_DIRECTORY = "compiler";

    public static final String META_DEPENDENCY_LOCATION = "desc_gen" + File.separator+ "dependencies";
    public static final String NEW_LINE_CHARACTER = System.getProperty("line.separator");
    public static final String GOOGLE_STANDARD_LIB = "google" + File.separator + "protobuf";
    public static final String EMPTY_STRING = "";

    public static final String COMPONENT_IDENTIFIER = "grpc";
    public static final String PROTOC_PLUGIN_EXE_PREFIX = ".exe";
    public static final String PROTO_SUFFIX = ".proto";
    public static final String DESC_SUFFIX = ".desc";
    public static final String PROTOC_PLUGIN_EXE_URL_SUFFIX = "http://repo1.maven.org/maven2/com/google/" +
            "protobuf/protoc/";
}
