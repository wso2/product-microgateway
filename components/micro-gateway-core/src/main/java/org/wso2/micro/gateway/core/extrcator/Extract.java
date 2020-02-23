package org.wso2.micro.gateway.core.extrcator;

import org.ballerinalang.jvm.values.api.BMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 *
 */
public class Extract {

    /**
     *
     */
    public static BMap<String, String> extract(String projectName, String serviceName) throws IOException {
        String swaggerContent;
        BMap<String, String> resources = null;
        InputStream in = new Object().getClass().getResourceAsStream("/resources/wso2/" + projectName);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        while ((swaggerContent = reader.readLine()) != null) {
            resources.put(serviceName, swaggerContent);
        }
        return resources;
    }
}
