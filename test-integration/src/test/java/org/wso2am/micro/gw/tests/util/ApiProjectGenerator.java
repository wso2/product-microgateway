package org.wso2am.micro.gw.tests.util;

import org.testcontainers.shaded.org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.regex.Pattern;

public class ApiProjectGenerator {

    public static void createApiZipFiles() throws IOException {

        String apisPath = "test-integration/src/test/resources/apis/openApis";
        final File folder = new File(apisPath);

        for (final File fileEntry : folder.listFiles()) {
            if (!fileEntry.isDirectory()) {
                // create zip files
                String apiProjPath = createApictlProjStructure(fileEntry.getName());
                ZipDir.createZipFile(apiProjPath);
            }
        }
    }


    public static void main(String[] args) throws IOException {

        String certificatesTrustStorePath = ApiProjectGenerator.class.getClassLoader()
                .getResource("keystore/cacerts").getPath();


        //ZipDir.createZipFile("test-integration/src/test/resources/apis/proj1");
        createApictlProjStructure("mockApi.yaml");
    }

    public static String createApictlProjStructure(String apiYamlName) throws IOException {

        String filename = apiYamlName.split(Pattern.quote("."))[0];
        String apisZipPath = ApiProjectGenerator.class.getClassLoader()
                .getResource("apis").getPath() + File.separator + "apiProjects" + File.separator + filename;
        createDirectory(apisZipPath);
        createDirectory(apisZipPath + File.separator + "Meta-information");
        createDirectory(apisZipPath + File.separator + "instruct");
        createDirectory(apisZipPath + File.separator + "Sequences");
        createDirectory(apisZipPath + File.separator + "libs");
        createDirectory(apisZipPath + File.separator + "Interceptors");
        createDirectory(apisZipPath + File.separator + "Image");
        createDirectory(apisZipPath + File.separator + "Docs");

        String apiPath = "test-integration/src/test/resources/apis/openApis" + File.separator+ apiYamlName;
        FileUtils.copyFile(new File(apiPath), new File(apisZipPath + File.separator +
                "Meta-information" + File.separator + apiYamlName));

        return apisZipPath;
    }

    public static void createDirectory(String filePath) {
        File theDir = new File(filePath);
        theDir.mkdirs();
    }

}
