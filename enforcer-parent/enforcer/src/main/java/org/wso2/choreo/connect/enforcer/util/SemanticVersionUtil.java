/*
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.enforcer.util;

import org.wso2.choreo.connect.enforcer.dto.SemVersion;
import org.wso2.choreo.connect.enforcer.exception.EnforcerException;

/**
 * This class holds Semantic version handling utils
 */
public class SemanticVersionUtil {
    // Validates version string and extracts version components
    public static SemVersion validateAndGetVersionComponents(String version, String apiUUID) throws EnforcerException {
        String[] versionComponents = version.split("\\.");

        // If the versionComponents length is less than 2, return error
        if (versionComponents.length < 2) {
            String errMessage = String.format("Invalid version: %s for API: %s. " +
                    "API version should be in the format x.y.z or vx.y.z where x,y,z are non-negative integers" +
                    "and v is version prefix", version, apiUUID);
            throw new EnforcerException(errMessage);
        }

        int majorVersion, minorVersion;
        try {
            if (versionComponents[0].startsWith("v")) {
                majorVersion = Integer.parseInt(versionComponents[0].substring(1));
            } else {
                majorVersion = Integer.parseInt(versionComponents[0]);
            }
        } catch (NumberFormatException e) {
            String errMessage = String.format("Invalid version: %s for API: %s. Version is not starting with v or " +
                    "Major version is not a number", version, apiUUID);
            throw new EnforcerException(errMessage);
        }

        try {
            minorVersion = Integer.parseInt(versionComponents[1]);
        } catch (NumberFormatException e) {
            String errMessage = String.format("Invalid version: %s for API: %s. Minor version is not a number",
                    version, apiUUID);
            throw new EnforcerException(errMessage);
        }

        if (majorVersion < 0) {
            String errMessage = String.format("Invalid version: %s for API: %s. Major version should be " +
                    "a non-negative integer", version, apiUUID);
            throw new EnforcerException(errMessage);
        }

        if (minorVersion < 0) {
            String errMessage = String.format("Invalid version: %s for API: %s. Minor version should be " +
                    "a non-negative integer", version, apiUUID);
            throw new EnforcerException(errMessage);
        }

        if (versionComponents.length == 2) {
            return new SemVersion(version, majorVersion, minorVersion);
        }

        int patchVersion;
        try {
            patchVersion = Integer.parseInt(versionComponents[2]);
        } catch (NumberFormatException e) {
            String errMessage = String.format("Invalid version: %s for API: %s. Patch version is not a number",
                    version, apiUUID);
            throw new EnforcerException(errMessage);
        }

        if (patchVersion < 0) {
            String errMessage = String.format("Invalid version: %s for API: %s. Patch version should be " +
                    "a non-negative integer", version, apiUUID);
            throw new EnforcerException(errMessage);
        }

        return new SemVersion(version, majorVersion, minorVersion, patchVersion);
    }
}
