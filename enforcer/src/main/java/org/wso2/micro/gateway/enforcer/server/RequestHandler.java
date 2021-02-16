package org.wso2.micro.gateway.enforcer.server;


/**
 * RequestHandler generic interface
 * @param <T> Request type
 * @param <S> Response type
 * e.g - HttpRequestHandler implements RequestHandler<CheckRequest,ResponseObject>
 */
public interface RequestHandler <T, S>{
    S process(T request);
}
