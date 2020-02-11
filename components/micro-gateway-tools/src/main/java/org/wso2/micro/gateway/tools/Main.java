package org.wso2.micro.gateway.tools;

/**
 * Extern function wso2.gateway:main.
 */
public class Main {
    public static void main(String[] args) {
            String confFilePath = args [1];
            String fileWritePath = args [2];
            GetConfig getConfig = new GetConfig();
            try {
                getConfig.getConfigurations(confFilePath, fileWritePath);
            } catch (Exception ex) {
                // if some error occurs we are using printStackTrace as we have logged that error from the shell
                ex.printStackTrace();
                System.exit(1);
            }
    }
}
