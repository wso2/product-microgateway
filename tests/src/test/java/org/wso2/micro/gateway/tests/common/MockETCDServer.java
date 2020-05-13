package org.wso2.micro.gateway.tests.common;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.cli.constants.TokenManagementConstants;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

public class MockETCDServer extends Thread {

    private static final Logger log = LoggerFactory.getLogger(MockHttpServer.class);
    private HttpsServer httpServer;
    private String ETCDServerUrl;
    private int ETCDServerPort;

    public final static String ALL_KEYS_RESPONSE = "{    \"action\": \"get\",    \"node\": {       " +
            " \"key\": \"/jti\",        \"dir\": true,        \"nodes\": [            {                " +
            "\"key\": \"/jti/530ade9c-f831-4867-b465-54fbcb3d04bc\",               " +
            " \"value\": \"bar\",              " +
            "  \"modifiedIndex\": 27,                \"createdIndex\": 27            },            " +
            "{                \"key\": \"/jti/cd132d9f-dc47-4ba1-a278-9c408fad27f1\",               " +
            " \"value\": \"bar\",                \"modifiedIndex\": 34,              " +
            "  \"createdIndex\": 34            },            {              " +
            "  \"key\": \"/jti/1de9bb21-da04-4f68-967c-bbf422f3f8ec\",              " +
            "  \"value\": \"bar\",                \"modifiedIndex\": 30,               " +
            " \"createdIndex\": 30            }        ],        \"modifiedIndex\": 25,       " +
            " \"createdIndex\": 25    }}";

    public static void main(String[] args) {

        MockETCDServer mockETCDServer = new MockETCDServer(2379);
        mockETCDServer.start();
    }

    public MockETCDServer(int ETCDServerPort) {

        this.ETCDServerPort = ETCDServerPort;
    }
    public void run() {
        String etcdKeysBasePath = "/v2/keys/jti";

        if (ETCDServerPort < 0) {
            throw new RuntimeException("Server port is not defined");
        }
        try {
            httpServer = HttpsServer.create(new InetSocketAddress(ETCDServerPort), 0);
            httpServer.setHttpsConfigurator(new HttpsConfigurator(getSslContext()) {
                public void configure(HttpsParameters params) {

                    try {
                        // initialise the SSL context
                        SSLContext c = SSLContext.getDefault();
                        SSLEngine engine = c.createSSLEngine();
                        params.setNeedClientAuth(false);
                        params.setCipherSuites(engine.getEnabledCipherSuites());
                        params.setProtocols(engine.getEnabledProtocols());
                        // get the default parameters
                        SSLParameters defaultSSLParameters = c
                                .getDefaultSSLParameters();
                        params.setSSLParameters(defaultSSLParameters);
                    } catch (Exception ex) {
                        log.error("Failed to create HTTPS port");
                    }
                }
            });
            httpServer.createContext(etcdKeysBasePath, exchange -> {

                byte[] response = ALL_KEYS_RESPONSE.getBytes();
                exchange.getResponseHeaders().set(HttpHeaderNames.CONTENT_TYPE.toString(), TokenManagementConstants.CONTENT_TYPE_APPLICATION_JSON);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            httpServer.start();
            ETCDServerUrl = "http://localhost:" + ETCDServerPort;
        } catch (IOException e) {
            log.error("Error occurred while setting up mock server", e);
        } catch (Exception e) {
            log.error("Error occurred while setting up mock server", e);
        }
    }

    public void stopIt() {

        httpServer.stop(0);
    }

    public String getETCDServerUrl() {

        return ETCDServerUrl;
    }

    public void setETCDServerUrl(String ETCDServerUrl) {

        this.ETCDServerUrl = ETCDServerUrl;
    }

    private SSLContext getSslContext() throws Exception {

        SSLContext sslContext = SSLContext.getInstance("TLS");

        // initialise the keystore
        char[] password = "wso2carbon".toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");
        InputStream fis = Thread.currentThread().getContextClassLoader().getResourceAsStream("wso2carbon.jks");
        ks.load(fis, password);

        // setup the key manager factory
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, password);

        // setup the trust manager factory
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        // setup the HTTPS context and parameters
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        return sslContext;
    }

}
