/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.apimgt.gateway.cli.model.rest;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ClientCertificatesDTO {

    private Integer count = null;
    private String next = null;
    private String previous = null;
    private List<ClientCertMetadataDTO> certificates = new ArrayList<ClientCertMetadataDTO>();
    private APIListPaginationDTO pagination = null;

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public String getNext() {
        return next;
    }

    public void setNext(String next) {
        this.next = next;
    }

    public String getPrevious() {
        return previous;
    }

    public void setPrevious(String previous) {
        this.previous = previous;
    }

    public List<ClientCertMetadataDTO> getCertificates() {
        return certificates;
    }

    public void setCertificates(List<ClientCertMetadataDTO> certificates) {
        this.certificates = certificates;
    }

    public APIListPaginationDTO getPagination() {
        return pagination;
    }

    public void setPagination(APIListPaginationDTO pagination) {
        this.pagination = pagination;
    }
}
