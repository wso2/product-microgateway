package org.wso2.choreo.connect.enforcer.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;



/**
 * sample soap message
 */
public class SOAPUtils {
    private static final Logger log = LogManager.getLogger(SOAPUtils.class);

    public static String getSoapFaultMessage(String soapProtocolVersion,
                                             String message, String description, String code) {
        try {
            MessageFactory factory = MessageFactory.newInstance(soapProtocolVersion);
            SOAPMessage soapMsg = factory.createMessage();
            SOAPPart part = soapMsg.getSOAPPart();
            SOAPEnvelope envelope = part.getEnvelope();
            SOAPFault soapFault = envelope.getBody().addFault();
            soapFault.setFaultCode("soapenv:Sender");
            soapFault.setFaultString(message);
            soapFault.addDetail().addTextNode(code + ":" + description);
            return envelope.toString();
        } catch (Exception e) {
            log.error("Error while creating the SOAP fault message.", e);
            return "";
        }
    }
}
