package org.wso2am.micro.gw.tests.mockconsul;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BackendSeversHTTP {
    protected final int startingPort;
    protected final int numberOfServers;
    protected String host;
    protected final List<HttpServer> serverList;
    protected static final Logger logger = Logger.getLogger(BackendSeversHTTP.class.getName());

    public BackendSeversHTTP(int startingPort, int numberOfServers) {
        this.startingPort = startingPort;
        this.numberOfServers = numberOfServers;
        serverList = new ArrayList<>();
        this.host = "localhost";
    }

    public BackendSeversHTTP(String host, int startingPort, int numberOfServers) {
        this(startingPort, numberOfServers);
        this.host = host;
    }

    protected void createServer(String hostname, int port) throws IOException, KeyManagementException, KeyStoreException, UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(hostname, port), 0);
        serverList.add(httpServer);
        String context = "/";
        httpServer.createContext(context, exchange -> {
            String resp = exchange.getLocalAddress() + "|" + exchange.getRequestURI() + "|" + exchange.getRemoteAddress();
            System.out.println(resp);

            byte[] response = resp.getBytes();
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        httpServer.start();
        System.out.println("http://" + host + ":" + port + "/");
    }

    public void run() {
        try {
            for (int i = 0; i < numberOfServers; i++) {
                createServer(this.host, startingPort + i);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error starting backend servers http: ", e);
        }
    }

    public static void main(String[] args) {
        BackendSeversHTTP b = new BackendSeversHTTP("169.254.1.3",6001, 2);
        b.run();
    }

}
