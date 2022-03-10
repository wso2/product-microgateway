package org.wso2.choreo.connect.mockbackend.dto;

import com.sun.net.httpserver.Headers;

import java.util.Map;

public class EchoResponse {
    private String data;
    private Headers headers;
    private String method;
    private String path;
    private Map<String, String> query;

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Headers getHeaders() {
        return headers;
    }

    public void setHeaders(Headers headers) {
        this.headers = headers;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, String> getQuery() {
        return query;
    }

    public void setQuery(Map<String, String> query) {
        this.query = query;
    }
}
