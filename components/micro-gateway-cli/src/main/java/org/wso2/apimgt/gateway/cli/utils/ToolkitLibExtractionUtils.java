/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.apimgt.gateway.cli.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.cli.constants.CliConstants;
import org.wso2.apimgt.gateway.cli.exception.CLIInternalException;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * This class represents the utility functions required for library packages extraction
 */
public class ToolkitLibExtractionUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ToolkitLibExtractionUtils.class);
    private static final PrintStream OUT = System.out;

    /**
     * Extracts the platform and runtime and copy related jars and balos to extracted runtime and platform.
     */
    public static void extractPlatformAndRuntime() {
        try {
            String libPath = CmdUtils.getCLILibPath();
            String birPath = CliConstants.CLI_GATEWAY + File.separator + CliConstants.CLI_BIR_CACHE;
            String breLibPath = CliConstants.CLI_BRE + File.separator + CliConstants.CLI_LIB;
            String platformExtractedPath =
                    CmdUtils.getCLILibPath() + File.separator + CliConstants.CLI_PLATFORM;

            extractBallerinaDist(platformExtractedPath, libPath, birPath, breLibPath, true);
        } catch (IOException e) {
            String message = "Error while unzipping platform while project setup";
            LOGGER.error(message, e);
            throw new CLIInternalException(message);
        }
    }

    private static void extractBallerinaDist(String destination, String libPath, String birPath, String breLibPath,
                                             Boolean isAddToClasspath) throws IOException {
        if (!Files.exists(Paths.get(destination))) {
            OUT.println("Initializing Toolkit...");
            ZipUtils.unzip(destination + CliConstants.EXTENSION_ZIP, destination,
                    isAddToClasspath);

            // Copy bir to the platform
            CmdUtils.copyFolder(libPath + File.separator + birPath,
                    destination + File.separator + CliConstants.CLI_BIR_CACHE);

            // Copy gateway jars to platform
            CmdUtils.copyFolder(libPath + File.separator + CliConstants.CLI_GATEWAY + File.separator
                    + CliConstants.CLI_PLATFORM, destination + File.separator + breLibPath);


        }
    }
}
