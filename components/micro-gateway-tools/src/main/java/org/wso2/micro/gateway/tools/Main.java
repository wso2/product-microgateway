package org.wso2.micro.gateway.tools;

/**
 * Extern function wso2.gateway:main.
 */
public class Main {
    public static void main(String[] args) {
        if (args[0].equals("unzip")) {
            String zipFilePath = System.getenv("GW_HOME") + "/runtime.zip";
            String destDirectory = System.getenv("GW_HOME") + "/runtime";
            UnzipUtility unZipper = new UnzipUtility();
            try {
                unZipper.unzip(zipFilePath, destDirectory);
            } catch (Exception ex) {
                // if some error occurs we are using printStackTrace as we have logged that error from the shell
                ex.printStackTrace();
                System.exit(1);
            }
        } else if (args[0].equals("getConfig")) {
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
}
