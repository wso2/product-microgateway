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
import org.slf4j.LoggerFactory;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToXml;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

public class MockBackendSOAP extends Thread{
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(MockBackendSOAP.class);
    private WireMockServer wireMockServer;
    private String wsdlDefinition;
    private String responseBody;

    private static final String REQ_PAYLOAD = "<soap:Envelope\n" +
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

    private String REQ_PAYLOAD_2 = "";

    public MockBackendSOAP(){
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        try {
            wsdlDefinition = Utils.readFileFromInputStream(
                    classloader.getResourceAsStream("wsdl/PhoneVerification.wsdl"));
            responseBody = Utils.readFileFromInputStream(
                    classloader.getResourceAsStream("soap/checkPhoneNumberResponseBody.xml"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void run(){
        startWiremockServer(Constants.MOCK_BACKEND_SOAP_SERVER_PORT);
    }

    private void startWiremockServer(int port) {
        log.info("Starting mock backend for SOAP service...");
        wireMockServer = new WireMockServer(options().port(port));
        wireMockServer.stubFor(WireMock
                .get(urlEqualTo("/phoneverify/wsdl"))
                .willReturn(
                        aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "text/xml")
                                .withBody(wsdlDefinition)));
        wireMockServer.stubFor(WireMock
                .post(urlEqualTo("/phoneverify"))
                .withRequestBody(equalToXml(REQ_PAYLOAD))
                .willReturn(
                        aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "text/xml")
                                .withBody(responseBody)));
        wireMockServer.start();
        log.info("Mock backend for SOAP service successfully started.");
    }

    private void stopWiremockServer(){
        if (wireMockServer.isRunning()){
            wireMockServer.stop();
        }
    }
}
