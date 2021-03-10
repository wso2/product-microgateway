package org.wso2.micro.gateway.enforcer.analytics;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.carbon.apimgt.common.gateway.analytics.collectors.AnalyticsDataProvider;
import org.wso2.carbon.apimgt.common.gateway.analytics.exceptions.DataNotFoundException;
import org.wso2.carbon.apimgt.common.gateway.analytics.publishers.dto.API;
import org.wso2.carbon.apimgt.common.gateway.analytics.publishers.dto.Application;
import org.wso2.carbon.apimgt.common.gateway.analytics.publishers.dto.Error;
import org.wso2.carbon.apimgt.common.gateway.analytics.publishers.dto.Latencies;
import org.wso2.carbon.apimgt.common.gateway.analytics.publishers.dto.MetaInfo;
import org.wso2.carbon.apimgt.common.gateway.analytics.publishers.dto.Operation;
import org.wso2.carbon.apimgt.common.gateway.analytics.publishers.dto.Target;
import org.wso2.carbon.apimgt.common.gateway.analytics.publishers.dto.enums.EventCategory;
import org.wso2.carbon.apimgt.common.gateway.analytics.publishers.dto.enums.FaultCategory;
import org.wso2.carbon.apimgt.common.gateway.analytics.publishers.dto.enums.FaultSubCategory;
import org.wso2.micro.gateway.enforcer.api.APIFactory;
import org.wso2.micro.gateway.enforcer.api.RequestContext;
import org.wso2.micro.gateway.enforcer.constants.APIConstants;
import org.wso2.micro.gateway.enforcer.security.AuthenticationContext;

/**
 * Generate FaultDTO for the errors generated from enforcer.
 */
public class MgwFaultAnalyticsProvider implements AnalyticsDataProvider {
    private static final Logger logger = LogManager.getLogger(APIFactory.class);
    private RequestContext requestContext;

    public MgwFaultAnalyticsProvider(RequestContext requestContext) {
        this.requestContext = requestContext;
    }

    @Override
    public EventCategory getEventCategory() {
        return EventCategory.FAULT;
    }

    @Override
    public boolean isAnonymous() {
        // TODO: (VirajSalaka) fix
        return false;
    }

    @Override
    public boolean isAuthenticated() {
        AuthenticationContext authenticationContext = requestContext.getAuthenticationContext();
        return authenticationContext != null && authenticationContext.isAuthenticated();
    }

    @Override
    public FaultCategory getFaultType() {
        if (requestContext.getProperties().containsKey(APIConstants.MessageFormat.STATUS_CODE)) {
            int statusCode = Integer.parseInt(requestContext.getProperties()
                    .get(APIConstants.MessageFormat.STATUS_CODE).toString());
            switch (statusCode) {
                case 401:
                case 403:
                    return FaultCategory.AUTH;
                case 429:
                    return FaultCategory.THROTTLED;
                default:
                    return FaultCategory.OTHER;
            }
        }
        return FaultCategory.OTHER;
    }

    @Override
    public API getApi() throws DataNotFoundException {
        API api = new API();
        api.setApiId(AnalyticsUtils.getAPIId(requestContext));
        api.setApiCreator(AnalyticsUtils.setDefaultIfNull(
                requestContext.getAuthenticationContext() == null
                        ? null : requestContext.getAuthenticationContext().getApiPublisher()));
        api.setApiType("HTTP");
        api.setApiName(requestContext.getMathedAPI().getAPIConfig().getName());
        api.setApiVersion(requestContext.getMathedAPI().getAPIConfig().getVersion());
        api.setApiCreatorTenantDomain("carbon.super");
        return api;
    }

    @Override
    public Application getApplication() throws DataNotFoundException {
        AuthenticationContext authContext = AnalyticsUtils.getAuthenticationContext(requestContext);
        Application application = new Application();
        // Default Value would be PRODUCTION
        application.setKeyType(
                authContext.getKeyType() == null ? APIConstants.API_KEY_TYPE_PRODUCTION : authContext.getKeyType());
        application.setApplicationId(AnalyticsUtils.setDefaultIfNull(authContext.getApplicationId()));
        application.setApplicationOwner(AnalyticsUtils.setDefaultIfNull(authContext.getSubscriber()));
        application.setApplicationName(AnalyticsUtils.setDefaultIfNull(authContext.getApplicationName()));
        return application;
    }

    @Override
    public Operation getOperation() throws DataNotFoundException {
        // This could be null if  OPTIONS request comes
        // TODO: (VirajSalaka) handle method not found operation
        if (requestContext.getMatchedResourcePath() != null) {
            Operation operation = new Operation();
            operation.setApiMethod(requestContext.getMatchedResourcePath().getMethod().name());
            operation.setApiResourceTemplate(requestContext.getMatchedResourcePath().getPath());
            return operation;
        }
        return null;
    }

    @Override
    public Target getTarget() {
        Target target = new Target();
        target.setResponseCacheHit(false);
        target.setTargetResponseCode(Integer.parseInt(
                requestContext.getProperties().get(APIConstants.MessageFormat.STATUS_CODE).toString()));
        // Destination is not included in the fault event scenario
        return target;
    }

    @Override
    public Latencies getLatencies() {
        // Latencies information are not required.
        return null;
    }

    @Override
    public MetaInfo getMetaInfo() {
        MetaInfo metaInfo = new MetaInfo();
        metaInfo.setRegionId("UnAssigned");
        metaInfo.setGatewayType("Envoy");
        metaInfo.setCorrelationId(requestContext.getCorrelationID());
        return metaInfo;
    }

    @Override
    public int getProxyResponseCode() {
        return Integer.parseInt(requestContext.getProperties()
                .get(APIConstants.MessageFormat.STATUS_CODE).toString());
    }

    @Override
    public int getTargetResponseCode() {
        return Integer.parseInt(requestContext.getProperties()
                .get(APIConstants.MessageFormat.STATUS_CODE).toString());
    }

    @Override
    public long getRequestTime() {
        // TODO: (VirajSalaka) Fetch the information from checkrequest
        return System.currentTimeMillis();
    }

    @Override
    public Error getError(FaultCategory faultCategory) {
        // All the messages should have the error_code
        if (requestContext.getProperties().containsKey(APIConstants.MessageFormat.ERROR_CODE)) {
            FaultCodeClassifier faultCodeClassifier =
                    new FaultCodeClassifier(Integer.parseInt(requestContext.getProperties()
                            .get(APIConstants.MessageFormat.ERROR_CODE).toString()));
            FaultSubCategory faultSubCategory = faultCodeClassifier.getFaultSubCategory(faultCategory);
            Error error = new Error();
            error.setErrorCode(faultCodeClassifier.getErrorCode());
            error.setErrorMessage(faultSubCategory);
            return error;
        }
        return null;
    }

    @Override
    public String getUserAgentHeader() {
        // User agent is not required for fault scenario
        return null;
    }

    @Override
    public String getEndUserIP() {
        // EndUserIP is not required for fault event type
        return null;
    }
}
