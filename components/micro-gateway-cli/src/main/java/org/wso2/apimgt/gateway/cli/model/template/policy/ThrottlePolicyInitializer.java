/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
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
package org.wso2.apimgt.gateway.cli.model.template.policy;

import org.wso2.apimgt.gateway.cli.constants.GeneratorConstants;
import org.wso2.carbon.apimgt.rest.api.admin.dto.SubscriptionThrottlePolicyDTO;
import org.wso2.carbon.apimgt.rest.api.admin.dto.ApplicationThrottlePolicyDTO;

import java.util.ArrayList;
import java.util.List;

public class ThrottlePolicyInitializer {
    private List<String> policyInitNames;
    private List<String> policyNames;
    private String srcPackage;
    private String modelPackage;

    public ThrottlePolicyInitializer() {
        policyInitNames = new ArrayList<>();
        policyNames = new ArrayList<>();
    }

    public List<String> getPolicyInitNames() {
        return policyInitNames;
    }

    public void setPolicyInitNames(List<String> policyInitNames) {
        this.policyInitNames = policyInitNames;
    }

    public ThrottlePolicyInitializer buildAppContext(List<ApplicationThrottlePolicyDTO> applicationPolicies) {
        for (ApplicationThrottlePolicyDTO policyDTO : applicationPolicies) {
            policyInitNames.add(GeneratorConstants.APPLICATION_INIT_FUNC_PREFIX + policyDTO.getPolicyName()
                    + GeneratorConstants.INIT_FUNC_SUFFIX);
            policyNames.add(policyDTO.getPolicyName());
        }
        return this;
    }

    public ThrottlePolicyInitializer buildSubsContext(List<SubscriptionThrottlePolicyDTO> subscriptionPolicies) {
        for (SubscriptionThrottlePolicyDTO policyDTO : subscriptionPolicies) {
            policyInitNames.add(GeneratorConstants.SUBSCRIPTION_INIT_FUNC_PREFIX + policyDTO.getPolicyName()
                    + GeneratorConstants.INIT_FUNC_SUFFIX);
            policyNames.add(policyDTO.getPolicyName());
        }
        return this;
    }

    public ThrottlePolicyInitializer srcPackage(String srcPackage) {
        if (srcPackage != null) {
            this.srcPackage = srcPackage.replaceFirst("\\.", "/");
        }
        return this;
    }

    public ThrottlePolicyInitializer modelPackage(String modelPackage) {
        if (modelPackage != null) {
            this.modelPackage = modelPackage.replaceFirst("\\.", "/");
        }
        return this;
    }
}