package org.wso2.micro.gateway.tests.common;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.wso2.apimgt.gateway.cli.constants.CliConstants;
import org.wso2.micro.gateway.tests.common.model.ApplicationPolicy;
import org.wso2.micro.gateway.tests.common.model.SubscriptionPolicy;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.jms.JMSException;
import javax.naming.NamingException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Mock http server for key-manager and APIM rest api endpoints
 */
public class MockHttpServer extends Thread {

    private static final Logger log = LoggerFactory.getLogger(MockHttpServer.class);
    private HttpsServer httpServer;
    private String KMServerUrl;
    private int KMServerPort = -1;
    private String DCRRestAPIBasePath = "/client-registration/v0.17";
    private String PubRestAPIBasePath = "/api/am/publisher/v1.2";
    private String AdminRestAPIBasePath = "/api/am/admin/v1.2";
    private String TMRestAPIBasePath = "/endpoints";
    private String InternalDataContext = "/internal/data/v1";
    public final static String PROD_ENDPOINT_RESPONSE = "{\"type\": \"production\"}";
    public final static String SAND_ENDPOINT_RESPONSE = "{\"type\": \"sandbox\"}";
    public final static String PROD_ENDPOINT_NEW_RESPONSE = "{\"type\": \"new-production\"}";
    public final static String SAND_ENDPOINT_NEW_RESPONSE = "{\"type\": \"new-sandbox\"}";
    public final static String PET_RESOURCE_RESPONSE = "{\"id\":111111148, \"category\":{ \"id\":0," +
            " \"name\":\"ABCD\" }, \"name\":\"doggieUpdated\", \"photoUrls\":[ \"SampleImage1.png\" ]," +
            " \"tags\":[ { \"id\":0, \"name\":\"TestTag1\" } ], \"status\":\"pending\"}";
    public final static String ECHOINVALIDRESPONSE_ENDPOINT_RESPONSE = "[{\"description\":\"Grilled white chicken, " +
            "hickory-smoked bacon and fresh sliced onions in barbeque sauce\", \"price\":\"25.99\"," +
            " \"icon\":\"/images/6.png\"}, {\"name\":\"Chicken Parmesan\", \"description\":\"Grilled chicken, fresh " +
            "tomatoes, feta and mozzarella cheese\", \"price\":\"17.99\", \"icon\":\"/images/1.png\"}, " +
            "{\"name\":\"Chilly Chicken Cordon Bleu\", \"description\":\"Spinash Alfredo sauce topped with grilled " +
            "chicken, ham, onions and mozzarella\", \"price\":\"15.99\", \"icon\":\"/images/10.png\"}, " +
            "{\"name\":\"Double Bacon 6Cheese\", \"description\":\"Hickory-smoked bacon, Julienne cut Canadian bacon," +
            " Parmesan, mozzarella, Romano, Asiago and and Fontina cheese\", \"price\":\"19.99\", " +
            "\"icon\":\"/images/9.png\"}, {\"name\":\"Garden Fresh\", \"description\":\"Slices onions and green " +
            "peppers, gourmet mushrooms, black olives and ripe Roma tomatoes\", \"price\":\"19.99\", " +
            "\"icon\":\"/images/3.png\"}, {\"name\":\"Grilled Chicken Club\", \"description\":\"Grilled white " +
            "chicken, hickory-smoked bacon and fresh sliced onions topped with Roma tomatoes\", \"price\":\"27.99\"," +
            " \"icon\":\"/images/8.png\"}, {\"name\":\"Hawaiian BBQ Chicken\", \"description\":\"Grilled" +
            " white chicken, hickory-smoked bacon, barbeque sauce topped with sweet pine-apple\", \"price\":\"26.99\"," +
            " \"icon\":\"/images/7.png\"}, {\"name\":\"Spicy Italian\", \"description\":\"Pepperoni and a double" +
            " portion of spicy Italian sausage\", \"price\":\"17.99\", \"icon\":\"/images/2.png\"}, " +
            "{\"name\":\"Spinach Alfredo\", \"description\":\"Rich and creamy blend of spinach and garlic Parmesan " +
            "with Alfredo sauce\", \"price\":\"28.99\", \"icon\":\"/images/5.png\"}, {\"name\":\"Tuscan Six Cheese\"," +
            " \"description\":\"Six cheese blend of mozzarella, Parmesan, Romano, Asiago and Fontina\"," +
            " \"price\":\"14.99\", \"icon\":\"/images/4.png\"}]";
    public final static String ECHO_ENDPOINT_RESPONSE = "{\"customerName\":\"string\", \"delivered\":true, " +
            "\"address\":\"string\", \"pizzaType\":\"string\", \"creditCardNumber\":\"string\", \"quantity\":0," +
            " \"orderId\":\"string\"}";
    public final static String INVALID_POSTBODY = "{\"customerName\":\"string\", \"delivered\":true, " +
            "\"address\":\"string\", \"pizzaType\":\"string\", \"creditCardNumber\":\"string\", \"quantity\":0," +
            " \"orderId\":44}";
    public final static String ERROR_MESSAGE_FOR_INVALID_RESPONSE = "{\"fault\":{\"code\":900916, \"message\":" +
            "\"Unprocessable entity\", \"description\":\"name is a required field\"}}";
    public final static String ECHO_ENDPOINT_RESPONSE_FOR_INVALID_REQUEST = "{\"fault\":{\"code\":900915, " +
            "\"message\":\"Unprocessable entity\", \"description\":\"44 is not the type, string\"}}";
    int count = 0;

    public static void main(String[] args) {

        MockHttpServer mockHttpServer = new MockHttpServer(9443);
        mockHttpServer.start();
    }

    public MockHttpServer(int KMServerPort) {

        this.KMServerPort = KMServerPort;
    }

    public void run() {

        if (KMServerPort < 0) {
            throw new RuntimeException("Server port is not defined");
        }
        try {
            httpServer = HttpsServer.create(new InetSocketAddress(KMServerPort), 0);
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
            httpServer.createContext(DCRRestAPIBasePath + "/register", new HttpHandler() {
                public void handle(HttpExchange exchange) throws IOException {

                    JSONObject payload = new JSONObject();
                    payload.put("clientId", UUID.randomUUID());
                    payload.put("clientSecret", UUID.randomUUID());
                    payload.put("clientName", "admin_Micro Gateway Cli");
                    payload.put("callBackURL", "https://wso2.org");
                    payload.put("isSaasApplication", false);
                    payload.put("jsonString", "{\"grant_types\":\"password\"}");

                    byte[] response = payload.toString().getBytes();
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                    exchange.getResponseBody().write(response);
                    exchange.close();
                }
            });
            httpServer.createContext("/echo", new HttpHandler() {
                public void handle(HttpExchange exchange) throws IOException {

                    String payload = IOUtils.toString(exchange.getRequestBody());
                    byte[] response = payload.toString().getBytes();
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                    exchange.getResponseBody().write(response);
                    exchange.close();
                }
            });
            httpServer.createContext("/echo/invalidResponse", new HttpHandler() {
                public void handle(HttpExchange exchange) throws IOException {

                    byte[] response = ECHOINVALIDRESPONSE_ENDPOINT_RESPONSE.toString().getBytes();
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                    exchange.getResponseBody().write(response);
                    exchange.close();
                }
            });
            httpServer.createContext(TMRestAPIBasePath + "/throttleEventReceiver", new HttpHandler() {
                public void handle(HttpExchange exchange) throws IOException {
                    String jsonRequest = IOUtils.toString(exchange.getRequestBody());
                    if (count == 9 || count == 19 || count == 29 || count == 39 || count == 49) {
                        JsonParser jsonParser = new JsonParser();
                        JsonObject jsonObject = (JsonObject) jsonParser.parse(jsonRequest);
                        JMSPublisher jmsPublisher = new JMSPublisher();

                        try {
                            jmsPublisher.getJson(jsonObject);
                        } catch (JMSException e) {
                            log.error("Error occurred while sending throttle event to TM", e);
                        } catch (NamingException e) {
                            log.error("Error occurred while sending throttle event to TM", e);
                        }
                    }
                    count++;
                    byte[] response = jsonRequest.toString().getBytes();
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                    exchange.getResponseBody().write(response);
                    exchange.close();
                }
            });
            httpServer.createContext("/echo/prod", new HttpHandler() {
                public void handle(HttpExchange exchange) throws IOException {

                    byte[] response = PROD_ENDPOINT_RESPONSE.toString().getBytes();
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                    exchange.getResponseBody().write(response);
                    exchange.close();
                }
            });
            httpServer.createContext("/echo/newprod", new HttpHandler() {
                public void handle(HttpExchange exchange) throws IOException {

                    byte[] response = PROD_ENDPOINT_NEW_RESPONSE.toString().getBytes();
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                    exchange.getResponseBody().write(response);
                    exchange.close();
                }
            });
            httpServer.createContext("/echo/sand", new HttpHandler() {
                public void handle(HttpExchange exchange) throws IOException {

                    byte[] response = SAND_ENDPOINT_RESPONSE.toString().getBytes();
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                    exchange.getResponseBody().write(response);
                    exchange.close();
                }
            });
            httpServer.createContext("/echo/newsand", new HttpHandler() {
                public void handle(HttpExchange exchange) throws IOException {

                    byte[] response = SAND_ENDPOINT_NEW_RESPONSE.toString().getBytes();
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                    exchange.getResponseBody().write(response);
                    exchange.close();
                }
            });
            httpServer.createContext("/oauth2/token", new HttpHandler() {
                public void handle(HttpExchange exchange) throws IOException {

                    JSONObject payload = new JSONObject();
                    payload.put("access_token", UUID.randomUUID());
                    payload.put("refresh_token", UUID.randomUUID());
                    payload.put("scope", "apim:api_view apim:tier_view");
                    payload.put("token_type", "Bearer");
                    payload.put("expires_in", 3600);

                    byte[] response = payload.toString().getBytes();
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                    exchange.getResponseBody().write(response);
                    exchange.close();
                }
            });
            httpServer.createContext(PubRestAPIBasePath + "/apis", new HttpHandler() {
                public void handle(HttpExchange exchange) throws IOException {

                    if(exchange.getRequestURI().getRawPath().contains("/swagger")) {
                        String apiId = exchange.getRequestURI().getRawPath().split("/")[6];
                        byte[] response = MockAPIPublisher.getInstance().getResponseForOpenAPI(apiId).getBytes();
                        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                        exchange.getResponseBody().write(response);
                        exchange.close();
                        return;
                    }
                    String query = parseParas(exchange.getRequestURI()).get("query");
                    String[] paras = URLDecoder.decode(query, CliConstants.CHARSET_UTF8).split(" ");
                    String label = null;
                    for (String para : paras) {
                        String[] searchQuery = para.split(":");
                        if ("label".equalsIgnoreCase(searchQuery[0])) {
                            label = searchQuery[1];
                        }
                    }

                    if (!StringUtils.isEmpty(label)) {
                        byte[] response = MockAPIPublisher.getInstance().getAPIResponseForLabel(label).getBytes();
                        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                        exchange.getResponseBody().write(response);
                        exchange.close();
                    } else {
                        exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, 0);
                        exchange.close();
                    }

                }
            });
            httpServer.createContext("/services/APIKeyValidationService", new HttpHandler() {
                public void handle(HttpExchange exchange) throws IOException {

                    String xmlRequest = IOUtils.toString(exchange.getRequestBody());
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    String token = null;

                    try {
                        DocumentBuilder builder = factory.newDocumentBuilder();
                        Document doc = builder.parse(new ByteArrayInputStream(xmlRequest.toString().getBytes("UTF-8")));
                        token = doc.getElementsByTagName("xsd:accessToken").item(0).getTextContent();

                        byte[] xmlResponse = MockAPIPublisher.getInstance().getKeyValidationResponseForToken(token)
                                .getBytes();
                        exchange.getResponseHeaders().set("Content-Type", "application/soap+xml");
                        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, xmlResponse.length);
                        exchange.getResponseBody().write(xmlResponse);
                    } catch (ParserConfigurationException e) {
                        log.error("Error occurred while parsing request", e);
                    } catch (SAXException e) {
                        log.error("Error occurred while parsing request", e);
                    }

                    byte[] xmlResponse = MockAPIPublisher.getInstance().getKeyValidationResponseForToken(token)
                            .getBytes();
                    exchange.getResponseHeaders().set("Content-Type", "application/soap+xml");
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, xmlResponse.length);
                    exchange.getResponseBody().write(xmlResponse);
                    exchange.close();

                }
            });
            httpServer.createContext(AdminRestAPIBasePath + "/throttling/policies/application", new HttpHandler() {
                public void handle(HttpExchange exchange) throws IOException {

                    String defaultPolicies = IOUtils.toString(new FileInputStream(
                            getClass().getClassLoader().getResource("application-policies.json").getPath()));
                    JSONObject policies = new JSONObject(defaultPolicies);
                    for (ApplicationPolicy policy : MockAPIPublisher.getInstance().getApplicationPolicies()) {
                        policies.getJSONArray("list").put(new JSONObject(new Gson().toJson(policy)));
                    }
                    byte[] response = policies.toString().getBytes();
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                    exchange.getResponseBody().write(response);
                    exchange.close();
                }
            });
            httpServer.createContext(AdminRestAPIBasePath + "/throttling/policies/subscription", new HttpHandler() {
                public void handle(HttpExchange exchange) throws IOException {

                    String defaultPolicies = IOUtils.toString(new FileInputStream(
                            getClass().getClassLoader().getResource("subscription-policies.json").getPath()));
                    JSONObject policies = new JSONObject(defaultPolicies);
                    for (SubscriptionPolicy policy : MockAPIPublisher.getInstance().getSubscriptionPolicies()) {
                        policies.getJSONArray("list").put(new JSONObject(new Gson().toJson(policy)));
                    }
                    byte[] response = policies.toString().getBytes();
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                    exchange.getResponseBody().write(response);
                    exchange.close();
                }
            });
            // Mock the introspection server.
            httpServer.createContext("/oauth2/introspect", new HttpHandler() {
                @Override
                public void handle(HttpExchange httpExchange) throws IOException {
                    String requestBody = IOUtils.toString(httpExchange.getRequestBody());
                    String token = requestBody.split("token=")[1];
                    byte[] response = MockAPIPublisher.getInstance().getIntrospectionResponse(token)
                            .toString().getBytes();

                    httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                    httpExchange.getResponseBody().write(response);
                    httpExchange.close();
                }
            });
            // Mock the internal data endpoint (get Api)
            httpServer.createContext(InternalDataContext + "/apis", new HttpHandler() {
                @Override
                public void handle(HttpExchange httpExchange) throws IOException {
                    String apisResponse = IOUtils.toString(new FileInputStream(
                            getClass().getClassLoader().getResource("api-get-response.json").getPath()));
                    JSONObject apisResponseJSON = new JSONObject(apisResponse);
                    byte[] response = apisResponseJSON.toString().getBytes();
                    httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                    httpExchange.getResponseBody().write(response);
                    httpExchange.close();
                }
            });
            // Mock the internal data endpoint (get application)
            httpServer.createContext(InternalDataContext + "/applications", new HttpHandler() {
                @Override
                public void handle(HttpExchange httpExchange) throws IOException {
                    String applicationsResponse = IOUtils.toString(new FileInputStream(
                            getClass().getClassLoader().getResource("application-get-response.json").getPath()));
                    JSONObject applicationsResponseJSON = new JSONObject(applicationsResponse);
                    byte[] response = applicationsResponse.toString().getBytes();
                    httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                    httpExchange.getResponseBody().write(response);
                    httpExchange.close();
                }
            });
            // Mock the internal data endpoint (get subscription)
            httpServer.createContext(InternalDataContext + "/subscriptions", new HttpHandler() {
                @Override
                public void handle(HttpExchange httpExchange) throws IOException {
                    String subscriptionResponse = IOUtils.toString(new FileInputStream(
                            getClass().getClassLoader().getResource("subscription-get-response.json").getPath()));
                    JSONObject subscriptionResponseJSON = new JSONObject(subscriptionResponse);
                    byte[] response = subscriptionResponseJSON.toString().getBytes();
                    httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                    httpExchange.getResponseBody().write(response);
                    httpExchange.close();
                }
            });// Mock the internal data endpoint (get application key mappings)
            httpServer.createContext(InternalDataContext + "/application-key-mappings", new HttpHandler() {
                @Override
                public void handle(HttpExchange httpExchange) throws IOException {
                    String consumerKey = "all";
                    String keyMappingResponse = IOUtils.toString(new FileInputStream(
                            getClass().getClassLoader().getResource("key-mapping-response.json").getPath()));
                    JSONObject keyMappingResponseJSON = new JSONObject(keyMappingResponse);
                    String query = httpExchange.getRequestURI().getQuery();
                    byte[] response;
                    if (query != null && query.contains("consumerKey")) {
                        consumerKey = query.split("=")[1];
                    }
                    response = keyMappingResponseJSON.get(consumerKey).toString().getBytes();
                    httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                    httpExchange.getResponseBody().write(response);
                    httpExchange.close();
                }
            });
            httpServer.start();
            KMServerUrl = "http://localhost:" + KMServerPort;
        } catch (IOException e) {
            log.error("Error occurred while setting up mock server", e);
        } catch (Exception e) {
            log.error("Error occurred while setting up mock server", e);

        }

    }

    public void stopIt() {

        httpServer.stop(0);
    }

    public String getKMServerUrl() {

        return KMServerUrl;
    }

    public void setKMServerUrl(String KMServerUrl) {

        this.KMServerUrl = KMServerUrl;
    }

    public int getKMServerPort() {

        return KMServerPort;
    }

    public void setKMServerPort(int KMServerPort) {

        this.KMServerPort = KMServerPort;
    }

    private Map<String, String> parseParas(URI uri) {

        String[] params = uri.getRawQuery().split("&");
        Map<String, String> map = new HashMap<>();
        for (String param : params) {
            String name = param.split("=")[0];
            String value = param.split("=")[1];
            map.put(name, value);
        }
        return map;
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