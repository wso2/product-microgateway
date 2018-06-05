package org.wso2.apimgt.gateway.codegen.service;

import org.wso2.apimgt.gateway.codegen.service.bean.ext.ExtendedAPI;
import org.wso2.apimgt.gateway.codegen.service.bean.policy.ApplicationThrottlePolicyDTO;
import org.wso2.apimgt.gateway.codegen.service.bean.policy.SubscriptionThrottlePolicyDTO;

import java.util.List;

public interface APIService {
    List<ExtendedAPI> getAPIs(String labelId, String accessToken);

    List<ApplicationThrottlePolicyDTO> getApplicationPolicies(String token);

    List<SubscriptionThrottlePolicyDTO> getSubscriptionPolicies(String token);
}
