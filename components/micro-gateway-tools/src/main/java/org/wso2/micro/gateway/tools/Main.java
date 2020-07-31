package org.wso2.micro.gateway.tools;

/**
 * Extern function wso2.gateway:main.
 */
public class Main {
    public static void main(String[] args) {
        GetConfig getConfig = new GetConfig();

        try {
            getConfig.getConfigurations(args);
        } catch (Exception ex) {
            // if some error occurs we are using printStackTrace as we have logged that error from the shell
            ex.printStackTrace();
            System.exit(1);
        }
    }
}
