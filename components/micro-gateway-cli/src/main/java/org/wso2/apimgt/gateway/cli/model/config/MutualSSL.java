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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.wso2.apimgt.gateway.cli.constants.RESTServiceConstants;
import org.wso2.apimgt.gateway.cli.model.rest.ClientCertMetadataDTO;

import java.util.List;

/**
 * Mutual SSL data holder for Client -> GW connection.
 */
//todo: check the intended usage of this
public class MutualSSL {

    private List<ClientCertMetadataDTO> clientCertificates;

    @SuppressFBWarnings(value = "URF_UNREAD_FIELD")
    private JsonArray certificateDetails;

    public List<ClientCertMetadataDTO> getClientCertificates() {
        return clientCertificates;
    }

    public MutualSSL() {
        this.clientCertificates = null;
    }

    public void setClientCertificates(List<ClientCertMetadataDTO> clientCertificates) {
        this.clientCertificates = clientCertificates;
    }

    public JsonArray getCertificateDetails() {

        if (clientCertificates == null) {
            return null;
        }

        int count = clientCertificates.size();
        JsonArray certificateData = new JsonArray();
        for (int i = 0; i < count; i++) {
            String alias = clientCertificates.get(i).getAlias();
            String tier = clientCertificates.get(i).getTier();
            JsonObject element = new JsonObject();
            element.addProperty(RESTServiceConstants.CERTIFICATE_ALIAS, alias);
            element.addProperty(RESTServiceConstants.CERTIFICATE_TIER, tier);
            certificateData.add(element);
        }
        JsonArray certificateDetails = certificateData;
        return certificateDetails;
    }

    public void setCertificateDetails(JsonArray certificateDetails) {
        this.certificateDetails = certificateDetails;
    }
}
