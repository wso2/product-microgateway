package org.wso2am.micro.gw.tests.mockconsul;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Map;
import java.util.concurrent.Executors;

public class BackendServersHTTPS extends BackendSeversHTTP {
    private String tlsVersion;
    private String keyStorePath;
    private String keyStorePassword;

    public BackendServersHTTPS(String host, int startingPort, int numberOfServers, String tlsVersion, String trustStorePath, String trustStorePassword) {
        super(host, startingPort, numberOfServers);
        this.tlsVersion = tlsVersion;
        this.keyStorePath = trustStorePath;
        this.keyStorePassword = trustStorePassword;
    }

    @Override
    protected void createServer(String hostname, int port) throws IOException,
            KeyManagementException, CertificateException, NoSuchAlgorithmException,
            KeyStoreException, UnrecoverableKeyException {

        // initialise the HTTPS server
        HttpsServer httpsServer = HttpsServer.create(new InetSocketAddress(hostname, port), 0);
        SSLContext sslContext = SSLContext.getInstance(tlsVersion);

        // initialise the keystore

        KeyStore keyStore = KeyStore.getInstance("JKS");

        //wso2carbon.jks only works for localhost
        keyStore.load(new FileInputStream(keyStorePath), keyStorePassword.toCharArray());

        // setup the key manager factory
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(keyStore, keyStorePassword.toCharArray());

        // setup the trust manager factory
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
        trustManagerFactory.init(keyStore);

        // setup the HTTPS context and parameters
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
        httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
        httpsServer.createContext("/", exchange -> {
            String response = exchange.getLocalAddress() + "|" + exchange.getRequestURI() + "|" + exchange.getRemoteAddress();
            System.out.println(response);
            exchange.getResponseHeaders().set("Content-Type", "text/html");
            exchange.getResponseHeaders().set("Accept-Charset", "utf-8");
            exchange.getResponseHeaders().set("Connection", "close");
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.getBytes(StandardCharsets.UTF_8).length);//response.getBytes(StandardCharsets.UTF_8).length+1
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
        });
        httpsServer.setExecutor(Executors.newCachedThreadPool());//multi threaded
        httpsServer.start();
        System.out.println("https://" + host + ":" + port + "/");
    }

    public void setResponseHeaders(HttpExchange exchange, Map<String, String> headers) {
        for (String key : headers.keySet()) {
            exchange.getResponseHeaders().set(key, headers.get(key));
        }
    }

    @Override
    public void run() {
        super.run();
    }

    public static void main(String[] args) {
        File targetClassesDir = new File(ConsulTestCase.class.getProtectionDomain().getCodeSource().
                getLocation().getPath());
        String keyStorePath = targetClassesDir.getParentFile().toString() + File.separator + "test-classes" +
                File.separator + "keystore" + File.separator + "wso2carbon.jks";
        String keyStorePassword = "wso2carbon";
        BackendServersHTTPS backendServersHTTPS = new BackendServersHTTPS("localhost", 6001,
                5, "TLSv1.2", keyStorePath, keyStorePassword);
        backendServersHTTPS.run();
    }
}
