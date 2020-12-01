package org.wso2am.micro.gw.tests.util;

import org.apache.commons.io.FileUtils;
import org.wso2am.micro.gw.tests.context.MicroGWTestException;

import java.io.File;
import java.io.IOException;

public class FileUtil {

    public static void copyFile(String sourceLocation, String destLocation) throws MicroGWTestException {
        File source = new File(sourceLocation);
        File destination = new File(destLocation);
        try {
            FileUtils.copyFile(source, destination);
        } catch (IOException e) {
            throw new MicroGWTestException("error while copying config file. ");
        }
    }

    public static void copyDirectory(String sourceLocation, String destLocation) throws MicroGWTestException {
        File source = new File(sourceLocation);
        File destination = new File(destLocation);
        try {
            FileUtils.copyDirectory(source, destination);
        } catch (IOException e) {
            throw new MicroGWTestException("error while copying config file. ");
        }
    }
}
