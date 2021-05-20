package org.wso2.choreo.connect.tests.apim.dto;

import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.test.utils.bean.APIRequest;

import java.util.List;

public class Api {
    //Not directly using APIRequest as a param since resolving endpointUrl is complicated
    String name;
    String version;
    String context;
    String endpointUrl;
    String tiersCollection;
    String tier;
    List<APIOperationsDTO> operationsDTOS;
    String[] vhosts;

    public Api(String name, String version, String context, String endpointUrl, String tiersCollection, String tier,
               List<APIOperationsDTO> operationsDTOS, String[] vhosts) {
        this.name = name;
        this.version = version;
        this.context = context;
        this.endpointUrl = context;
        this.tiersCollection = tiersCollection;
        this.tier = tier;
        this.operationsDTOS = operationsDTOS;
        this.vhosts = vhosts;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getContext() {
        return context;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public String getTiersCollection() {
        return tiersCollection;
    }

    public String getTier() {
        return tier;
    }

    public List<APIOperationsDTO> getOperationsDTOS() {
        return operationsDTOS;
    }

    public String[] getVhosts() {
        return vhosts;
    }
}
