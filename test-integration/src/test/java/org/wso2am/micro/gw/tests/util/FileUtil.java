/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2am.micro.gw.tests.util;

import org.apache.commons.io.FileUtils;
import org.wso2am.micro.gw.tests.context.MicroGWTestException;

import java.io.File;
import java.io.IOException;

public class FileUtil {

    public static void copyFile(String sourceLocation, String destLocation) throws MicroGWTestException {
        File source = new File(sourceLocation);
        File destination = new File(destLocation);
        try {
            FileUtils.copyFile(source, destination);
        } catch (IOException e) {
            throw new MicroGWTestException("error while copying config file. ");
        }
    }

    public static void copyDirectory(String sourceLocation, String destLocation) throws MicroGWTestException {
        File source = new File(sourceLocation);
        File destination = new File(destLocation);
        try {
            FileUtils.copyDirectory(source, destination);
        } catch (IOException e) {
            throw new MicroGWTestException("error while copying config file. ");
        }
    }
}
