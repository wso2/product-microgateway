/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.mockbackend;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToXml;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

public class MockBackendSOAP extends Thread{
    private static final Logger log = LoggerFactory.getLogger(MockBackendSOAP.class);
    private final int serverPort;
    private WireMockServer wireMockServer;
    private String wsdlDefinition;
    private String responseBodySoap11;
    private String responseBodySoap12;

    private static final String REQ_PAYLOAD_11 = "<soap:Envelope\n" +
            "\txmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "\txmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"\n" +
            "\txmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
            "\t<soap:Body>\n" +
            "\t\t<CheckPhoneNumber\n" +
            "\t\t\txmlns=\"http://ws.cdyne.com/PhoneVerify/query\">\n" +
            "\t\t\t<PhoneNumber>18006785432</PhoneNumber>\n" +
            "\t\t\t<LicenseKey>18006785432</LicenseKey>\n" +
            "\t\t</CheckPhoneNumber>\n" +
            "\t</soap:Body>\n" +
            "</soap:Envelope>";

    private static final String REQ_PAYLOAD_12 = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<soap12:Envelope\n" +
            "\txmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "\txmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"\n" +
            "\txmlns:soap12=\"http://www.w3.org/2003/05/soap-envelope\">\n" +
            "\t<soap12:Body>\n" +
            "\t\t<CheckPhoneNumber\n" +
            "\t\t\txmlns=\"http://ws.cdyne.com/PhoneVerify/query\">\n" +
            "\t\t\t<PhoneNumber>18006785432</PhoneNumber>\n" +
            "\t\t\t<LicenseKey>18006785432</LicenseKey>\n" +
            "\t\t</CheckPhoneNumber>\n" +
            "\t</soap12:Body>\n" +
            "</soap12:Envelope>";

    public MockBackendSOAP(int serverPort) {
        this.serverPort = serverPort;
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        try {
            wsdlDefinition = Utils.readFileFromInputStream(
                    classloader.getResourceAsStream("wsdl/PhoneVerification.wsdl"));
            responseBodySoap11 = Utils.readFileFromInputStream(
                    classloader.getResourceAsStream("soap/checkPhoneNumberResponseBody_11.xml"));
            responseBodySoap12 = Utils.readFileFromInputStream(
                    classloader.getResourceAsStream("soap/checkPhoneNumberResponseBody_12.xml"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void run(){
        startWiremockServer();
    }

    private void startWiremockServer() {
        log.info("Starting mock backend for SOAP service on port:{}", serverPort);
        wireMockServer = new WireMockServer(options().port(serverPort));
        wireMockServer.stubFor(WireMock
                .get(urlEqualTo("/phoneverify/wsdl"))
                .willReturn(
                        aResponse()
                                .withStatus(200)
                                .withHeader(Constants.CONTENT_TYPE, Constants.CONTENT_TYPE_TEXT_XML)
                                .withBody(wsdlDefinition)));
        wireMockServer.stubFor(WireMock
                .post(urlEqualTo("/phoneverify11"))
                .withRequestBody(equalToXml(REQ_PAYLOAD_11))
                .withHeader(Constants.SOAP_ACTION, containing("http://mockbackend:2340/phoneverify/query/CheckPhoneNumber"))
                .willReturn(
                        aResponse()
                                .withStatus(200)
                                .withHeader(Constants.CONTENT_TYPE, Constants.CONTENT_TYPE_TEXT_XML)
                                .withBody(responseBodySoap11)));
        wireMockServer.stubFor(WireMock
                .post(urlEqualTo("/phoneverify12"))
                .withRequestBody(equalToXml(REQ_PAYLOAD_12))
                .willReturn(
                        aResponse()
                                .withStatus(200)
                                .withHeader(Constants.CONTENT_TYPE, Constants.CONTENT_TYPE_SOAP_XML)
                                .withBody(responseBodySoap12)));
        wireMockServer.start();
        log.info("Mock backend for SOAP service successfully started.");
    }

    private void stopWiremockServer(){
        if (wireMockServer.isRunning()){
            wireMockServer.stop();
        }
    }
}
