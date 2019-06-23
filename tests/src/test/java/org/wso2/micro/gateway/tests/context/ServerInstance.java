/*
 * Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.micro.gateway.tests.context;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.mina.util.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.micro.gateway.tests.util.TestConstant;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;


/**
 * This class hold the server information and manage the a server instance.
 */
public class ServerInstance implements Server {
    private static final Logger log = LoggerFactory.getLogger(ServerInstance.class);
    //todo: remove toolkitDir to cliexecutor
    private String serverHome;
    private String configPath;
    private String[] args;
    private Process process;
    private ServerLogReader serverInfoLogReader;
    private ServerLogReader serverErrorLogReader;
    private boolean isServerRunning;
    private int httpServerPort = TestConstant.GATEWAY_LISTENER_HTTP_PORT;
    private int httpsServerPort = TestConstant.GATEWAY_LISTENER_HTTPS_PORT;
    private int httpServerPortToken = TestConstant.GATEWAY_LISTENER_HTTPS_TOKEN_PORT;
    private ConcurrentHashSet<LogLeecher> tmpLeechers = new ConcurrentHashSet<>();

    public ServerInstance(String serverDistributionPath) {
        this.serverHome = serverDistributionPath;
        initialize();
    }

    public ServerInstance(String serverDistributionPath, String configPath, int serverHttpPort, int serverHttpsPort,
                          int serverHttpsTokenPort) {
        this.configPath = configPath;
        this.serverHome = serverDistributionPath;
        this.httpServerPort = serverHttpPort;
        this.httpsServerPort = serverHttpsPort;
        this.httpServerPortToken = serverHttpsTokenPort;
        initialize();
    }

    /**
     * Method to start Micro-GW server given the port and bal file.
     *
     * @param httpPort       http server port
     * @param httpsPort      https server port
     * @param tokenHttpsPort https server token endpoint port
     * @return microGWServer      Started server instance.
     */
    public static ServerInstance initMicroGwServer(int httpPort, int httpsPort, int tokenHttpsPort, String configPath) {
        String serverRuntimePath;
        String osName = Utils.getOSName().toLowerCase();
        if (osName.contains("windows")) {
            serverRuntimePath = System.getProperty(Constants.SYSTEM_PROP_WINDOWS_RUNTIME);
        } else if (osName.contains("mac")) {
            serverRuntimePath = System.getProperty(Constants.SYSTEM_PROP_MACOS_RUNTIME);
        } else {
            serverRuntimePath = System.getProperty(Constants.SYSTEM_PROP_LINUX_RUNTIME);
        }
        return new ServerInstance(serverRuntimePath, configPath, httpPort, httpsPort, tokenHttpsPort);
    }

    /**
     * Method to start Micro-GW server in default port 9092 with given bal file and the given config file.
     *
     * @param configPath the absolute path of the config file
     * @return microGWServer      Started server instance.
     */
    public static ServerInstance initMicroGwServer(String configPath) {
        return initMicroGwServer(TestConstant.GATEWAY_LISTENER_HTTP_PORT, TestConstant.GATEWAY_LISTENER_HTTPS_PORT,
                TestConstant.GATEWAY_LISTENER_HTTPS_TOKEN_PORT, configPath);
    }

    /**
     * Method to start Micro-GW server in default port 9092 with given bal file and the default config file.
     *
     * @return microGWServer      Started server instance.
     */
    public static ServerInstance initMicroGwServer() {
        return initMicroGwServer(TestConstant.GATEWAY_LISTENER_HTTP_PORT, TestConstant.GATEWAY_LISTENER_HTTPS_PORT,
                TestConstant.GATEWAY_LISTENER_HTTPS_TOKEN_PORT, null);
    }

    public void startMicroGwServerWithDebugLog(String balFile) throws MicroGWTestException {
        String[] args = {balFile, "-e", "b7a.log.level=DEBUG"};
        setArguments(args);

        startServer();
    }

    /**
     * Start microgateway server with the given set of additional arguments and the given bal file.
     *
     * @param balFile the path of the bal File
     * @param args    additional commandline arguments
     * @throws MicroGWTestException
     */
    public void startMicroGwServer(String balFile, String[] args) throws MicroGWTestException {
        String[] newArgs = {balFile};
        newArgs = ArrayUtils.addAll(args, newArgs);
        setArguments(newArgs);
        startServer();
    }

    /**
     * @param balFile
     * @throws MicroGWTestException
     */
    public void startMicroGwServer(String balFile) throws MicroGWTestException {
        String[] newArgs = {balFile};
        setArguments(newArgs);
        startServer();
    }

    /**
     * Start the server pointing to the ballerina.conf path.
     *
     * @param balFile    ballerina file path
     * @param gwConfPath ballerina.conf file path
     * @throws MicroGWTestException if an error occurs while starting the server
     */
    public void startMicroGwServerWithConfigPath(String balFile, String gwConfPath) throws
            MicroGWTestException {
        String gwConfigPathArg = "--config ";
        String[] args = {gwConfigPathArg, gwConfPath, balFile};
        setArguments(args);

        startServer();
    }

    /**
     * Start a server instance y extracting a server zip distribution.
     *
     * @throws MicroGWTestException if server start fails
     */
    @Override
    public void startServer() throws MicroGWTestException {

        if (args == null | args.length == 0) {
            throw new IllegalArgumentException("No Argument provided for server startup.");
        }

        Utils.checkPortAvailability(httpServerPort);
        Utils.checkPortAvailability(httpsServerPort);
        Utils.checkPortAvailability(httpServerPortToken);

        log.info("Starting server..");
        startServerRuntime(args);

        serverInfoLogReader = new ServerLogReader("inputStream", process.getInputStream());
        tmpLeechers.forEach(leacher -> serverInfoLogReader.addLeecher(leacher));
        serverInfoLogReader.start();
        serverErrorLogReader = new ServerLogReader("errorStream", process.getErrorStream());
        serverErrorLogReader.start();
        log.info("Waiting for port " + httpServerPort + " to open");
        Utils.waitForPort(httpServerPort, 1000 * 60 * 2, false, "localhost");
        log.info("Server Started Successfully.");
        isServerRunning = true;
    }

    /**
     * Initialize the server instance with properties.
     */
    private void initialize() {
        if (serverHome == null) {
            log.info("Server Home " + serverHome);
            configServer();
        }
    }

    /**
     * Stop the server instance which is started by start method.
     *
     * @throws MicroGWTestException if service stop fails
     */
    @Override
    public void stopServer(boolean deleteExtractedDir) throws MicroGWTestException {
        log.info("Stopping server..");
        if (process != null) {
            String pid;
            try {
                pid = getServerPID();
                if (Utils.getOSName().toLowerCase().contains("windows")) {
                    Process killServer = Runtime.getRuntime().exec("TASKKILL -F /PID " + pid);
                    log.info(readProcessInputStream(killServer.getInputStream()));
                    killServer.waitFor(15, TimeUnit.SECONDS);
                    killServer.destroy();
                } else {
                    Process killServer = Runtime.getRuntime().exec("kill -9 " + pid);
                    killServer.waitFor(15, TimeUnit.SECONDS);
                    killServer.destroy();
                }
            } catch (IOException e) {
                log.error("Error getting process id for the server in port - " + httpServerPort
                        + " error - " + e.getMessage(), e);
                throw new MicroGWTestException("Error while getting the server process id", e);
            } catch (InterruptedException e) {
                log.error("Error stopping the server in port - " + httpServerPort + " error - " + e.getMessage(), e);
                throw new MicroGWTestException("Error waiting for services to stop", e);
            }
            process.destroy();
            serverInfoLogReader.stop();
            serverErrorLogReader.stop();
            process = null;
            //wait until port to close
            Utils.waitForPortToClosed(httpServerPort, 30000);
            log.info("Server Stopped Successfully");
        }
    }

    /**
     * Restart the server instance.
     *
     * @throws MicroGWTestException if the services could not be started
     */
    @Override
    public void restartServer() throws MicroGWTestException {
        log.info("Restarting Server...");
        stopServer(true);
        startServer();
        log.info("Server Restarted Successfully");
    }


    /**
     * Checking whether server instance is up and running.
     *
     * @return true if the server is up and running
     */
    @Override
    public boolean isRunning() {
        return isServerRunning;
    }

    /**
     * setting the list of command line argument while server startup.
     *
     * @param args list of service files
     */
    private void setArguments(String[] args) {
        this.args = args;
    }

    /**
     * to change the server configuration if required. This method can be overriding when initialising
     * the object of this class.
     */
    private void configServer() {
    }

//    /**
//     * Return toolkit directory path.
//     *
//     * @return absolute path of the server location
//     */
//    public String getToolkitDir() {
//        return toolkitDir;
//    }

    /**
     * Return the service URL.
     *
     * @param servicePath - http url of the given service
     * @return The service URL
     */
    public String getServiceURLHttp(String servicePath) {
        return "http://localhost:" + httpServerPort + "/" + servicePath;
    }

    /**
     * Add a Leecher which is going to listen to an expected text.
     *
     * @param leecher The Leecher instance
     */
    @SuppressWarnings("unused")
    public void addLogLeecher(LogLeecher leecher) {
        if (serverInfoLogReader == null) {
            tmpLeechers.add(leecher);
            return;
        }
        serverInfoLogReader.addLeecher(leecher);
    }

    /**
     * Executing the sh or bat file to start the server.
     *
     * @param args - command line arguments to pass when executing the sh or bat file
     * @throws MicroGWTestException if starting services failed
     */
    private void startServerRuntime(String[] args) throws MicroGWTestException {
        String scriptName = Constants.GATEWAY_SCRIPT_NAME;
        String[] cmdArray;
        File commandDir = new File(serverHome + File.separator + "bin");
        //Overwrite the config file
        if (configPath != null) {
            copyFile(configPath, serverHome + File.separator + "conf" + File.separator + "micro-gw.conf");
        }
        //run the server
        try {
            if (Utils.getOSName().toLowerCase().contains("windows")) {
                commandDir = new File(serverHome + File.separator + "bin");
                cmdArray = new String[]{"cmd.exe", "/c", scriptName + ".bat"};
                String[] cmdArgs = Stream.concat(Arrays.stream(cmdArray), Arrays.stream(args))
                        .toArray(String[]::new);
                process = Runtime.getRuntime().exec(cmdArgs, null, commandDir);
            } else {
                cmdArray = new String[]{"bash", scriptName};
                String[] cmdArgs = Stream.concat(Arrays.stream(cmdArray), Arrays.stream(args))
                        .toArray(String[]::new);
                process = Runtime.getRuntime().exec(cmdArgs, null, commandDir);
            }
        } catch (IOException e) {
            throw new MicroGWTestException("Error starting services", e);
        }
    }

    /**
     * reading the server process id.
     *
     * @return process id
     * @throws MicroGWTestException if pid could not be retrieved
     */
    private String getServerPID() throws MicroGWTestException {
        String pid = null;
        if (Utils.getOSName().toLowerCase().contains("windows")) {
            //reading the process id from netstat
            Process tmp;
            try {
                tmp = Runtime.getRuntime().exec("netstat -a -n -o");
            } catch (IOException e) {
                throw new MicroGWTestException("Error retrieving netstat data", e);
            }

            String outPut = readProcessInputStream(tmp.getInputStream());
            String[] lines = outPut.split("\r\n");
            for (String line : lines) {
                String[] column = line.trim().split("\\s+");
                if (column.length < 5) {
                    continue;
                }
                if (column[1].contains(":" + httpServerPort) && column[3].contains("LISTENING")) {
                    log.info(line);
                    pid = column[4];
                    break;
                }
            }
            tmp.destroy();
        } else {

            //reading the process id from ss
            Process tmp = null;
            try {
                String[] cmd = {"bash", "-c",
                        "ss -ltnp \'sport = :" + httpServerPort + "\' | grep LISTEN | awk \'{print $6}\'"};
                tmp = Runtime.getRuntime().exec(cmd);
                String outPut = readProcessInputStream(tmp.getInputStream());
                log.info("Output of the PID extraction command : " + outPut);
                /* The output of ss command is "users:(("java",pid=24522,fd=161))" in latest ss versions
                 But in older versions the output is users:(("java",23165,116))
                 TODO : Improve this OS dependent logic */
                if (outPut.contains("pid=")) {
                    pid = outPut.split("pid=")[1].split(",")[0];
                } else {
                    pid = outPut.split(",")[1];
                }

            } catch (Exception e) {
                log.warn("Error occurred while extracting the PID with ss " + e.getMessage());
                // If ss command fails trying with lsof. MacOS doesn't have ss by default
                pid = getPidWithLsof(httpServerPort);
            } finally {
                if (tmp != null) {
                    tmp.destroy();
                }
            }
        }
        log.info("Server process id in " + Utils.getOSName() + " : " + pid);
        return pid;
    }

    /**
     * Reading output from input stream.
     *
     * @param inputStream input steam of a process
     * @return the output string generated by java process
     */
    private String readProcessInputStream(InputStream inputStream) {
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        StringBuilder stringBuilder = new StringBuilder();
        try {
            inputStreamReader = new InputStreamReader(inputStream, Charset.defaultCharset());
            bufferedReader = new BufferedReader(inputStreamReader);
            int x;
            while ((x = bufferedReader.read()) != -1) {
                stringBuilder.append((char) x);
            }
        } catch (Exception ex) {
            log.error("Error reading process id", ex);
        } finally {
            if (inputStreamReader != null) {
                try {
                    inputStream.close();
                    inputStreamReader.close();
                } catch (IOException e) {
                    log.error("Error occurred while closing stream: " + e.getMessage(), e);
                }
            }
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    log.error("Error occurred while closing stream: " + e.getMessage(), e);
                }
            }
        }
        return stringBuilder.toString();
    }

    /**
     * This method returns the pid of the service which is using the provided port.
     *
     * @param httpServerPort port of the service running
     * @return the pid of the service
     * @throws MicroGWTestException if pid could not be retrieved
     */
    private String getPidWithLsof(int httpServerPort) throws MicroGWTestException {
        String pid;
        Process tmp = null;
        try {
            String[] cmd = {"bash", "-c", "lsof -Pi tcp:" + httpServerPort + " | grep LISTEN | awk \'{print $2}\'"};
            tmp = Runtime.getRuntime().exec(cmd);
            pid = readProcessInputStream(tmp.getInputStream());

        } catch (Exception err) {
            throw new MicroGWTestException("Error retrieving the PID : ", err);
        } finally {
            if (tmp != null) {
                tmp.destroy();
            }
        }
        return pid;
    }

    private static void copyFile(String sourceLocation, String destLocation) throws MicroGWTestException {
        File source = new File(sourceLocation);
        File destination = new File(destLocation);
        try {
            FileUtils.copyFile(source, destination);
        } catch (IOException e) {
            throw new MicroGWTestException("error while copying config file. ");
        }
    }
}
