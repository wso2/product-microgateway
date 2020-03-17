/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.apimgt.gateway.cli.model.template;

import org.wso2.apimgt.gateway.cli.exception.CLIInternalException;
import org.wso2.apimgt.gateway.cli.model.template.service.BallerinaService;
import org.wso2.apimgt.gateway.cli.utils.CmdUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

/**
 * Mustache data holder for Ballerina.toml file.
 */
public class BallerinaToml {
    private List<String> dependencies;
    private List<BallerinaLibrary> libs;
    private String toolkitHome;
    private String mgwVersion;

    public BallerinaToml() {
        dependencies = new ArrayList<>();
        libs = new ArrayList<>();
        init();
    }

    private void init() {
        Properties pom = new Properties();
        String pomPath = "/META-INF/maven/org.wso2.am.microgw/org.wso2.micro.gateway.cli/pom.properties";
        try (InputStream is = getClass().getResourceAsStream(pomPath)) {
            if (is != null) {
                pom.load(is);
                this.mgwVersion = pom.getProperty("version");
            }
        } catch (IOException e) {
            throw new CLIInternalException("Failed to initialize target ballerina project");
        }
    }

    /**
     * Add dependencies defined in {@code api} to ballerina toml file.
     *
     * @param api service definition with required dependencies and their versions
     */
    public void addDependencies(BallerinaService api) {
        if (api != null && api.getLibVersions() != null) {
            HashMap<String, String> moduleVersionMap = api.getLibVersions();
            for (HashMap.Entry<String, String> entry : moduleVersionMap.entrySet()) {
                String dependency = "\"" + entry.getKey() + "\" = \"" + entry.getValue() + "\"";
                dependencies.add(dependency);
            }
        }
    }

    /**
     * Add platform libraries of ballerina toml file.
     * This method will check {@code projectName}'s lib directory and pick all the
     * jar files in it as platform libraries.
     *
     * @param projectName name of the micro gateway project.
     */
    public void addLibs(String projectName) {
        List<String> jars = CmdUtils.getExternalJarDependencies(projectName);
        for (String jar : jars) {
            // currently we only require path to jar file.
            // Windows paths contain '\' separator which causes issues when included in ballerina.toml
            String path = jar.replace('\\', '/');
            BallerinaLibrary lib = new BallerinaLibrary();
            lib.setPath(path);
            libs.add(lib);
        }
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }

    public List<BallerinaLibrary> getLibs() {
        return libs;
    }

    public void setLibs(List<BallerinaLibrary> libs) {
        this.libs = libs;
    }

    public String getToolkitHome() {
        return toolkitHome;
    }

    public void setToolkitHome(String toolkitHome) {
        if (toolkitHome != null) {
            // Windows paths contain '\' separator which causes issues when included in ballerina.toml
            this.toolkitHome = toolkitHome.replace('\\', '/');
        }
    }

    public String getMgwVersion() {
        return mgwVersion;
    }

    public void setMgwVersion(String mgwVersion) {
        this.mgwVersion = mgwVersion;
    }
}
