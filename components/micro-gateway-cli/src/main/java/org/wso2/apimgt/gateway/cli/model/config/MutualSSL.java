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
package org.wso2.apimgt.gateway.cli.model.config;

import org.wso2.apimgt.gateway.cli.utils.GatewayCmdUtils;

import java.io.File;
import java.util.List;

public class MutualSSL {

    private String keyStoreLocation;
    private String keyStorePassword;
    private String trustStoreLocation;
    private String trustStorePassword;
    private String protocolName;
    private String  protocolVersion;
    private String cypher;

    private boolean sslVerify = false;



    public String getKeyStoreLocation() { return keyStoreLocation; }

  /*  public String getKeyStoreAbsoluteLocation() {
        String absolutekeyoreLocation = trustStoreLocation;
        File file = new File(absolutekeyoreLocation);
        if (!file.isAbsolute()) {
            absolutekeyoreLocation = GatewayCmdUtils.getCLIHome() + File.separator + absolutekeyoreLocation;
            file = new File(absolutekeyoreLocation);
            if (!file.exists()) {
                System.err.println("Error while loading trust store location: " + absolutekeyoreLocation);
                Runtime.getRuntime().exit(1);
            }
        }
        return absolutekeyoreLocation;
    }*/


    public void setKeyStoreLocation(String keyStoreLocation) { this.keyStoreLocation = keyStoreLocation; }

    public String getKeyStorePassword() { return keyStorePassword; }

    public void setKeyStorePassword(String keyStorePassword) { this.keyStorePassword = keyStorePassword; }

    public String getTrustStoreLocation() { return trustStoreLocation; }

    public void setTrustStoreLocation(String trustStoreLocation) { this.trustStoreLocation = trustStoreLocation; }

    public String getTrustStorePassword() { return trustStorePassword; }

    public void setTrustStorePassword(String trustStorePassword) { this.trustStorePassword = trustStorePassword; }

    public String getProtocolName() {
        return protocolName;
    }

    public void setProtocolName(String protocolName) {
        this.protocolName = protocolName;
    }

     public String getProtocolVersion() { return protocolVersion; }

    public void setProtocolVersion(String protocolVersion) { this.protocolVersion = protocolVersion;}

    public String getCypher() { return cypher; }

    public void setCypher(String cypher) { this.cypher = cypher; }

    public boolean isSslVerify() { return sslVerify; }

    public void setSslVerify(boolean sslVerify) { this.sslVerify = sslVerify; }
}
