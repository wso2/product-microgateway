/*
 *  Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package soaputils

import (
	"encoding/xml"
	"fmt"

	logger "github.com/wso2/product-microgateway/adapter/pkg/loggers"
	"github.com/wso2/product-microgateway/adapter/pkg/logging"
)

const (
	// xmlHeader Header A generic XML header suitable for use with the output of marshalled envelope.
	xmlHeader = `<?xml version="1.0" encoding="UTF-8"?>`

	// soap11Namespace is the xml namespace used for SOAP 1.1.
	soap11Namespace = "http://schemas.xmlsoap.org/soap/envelope/"

	// soap12Namespace is the xml namespace used for SOAP 1.2.
	soap12Namespace = "http://www.w3.org/2003/05/soap-envelope/"

	soap11ProtocolVersion = "SOAP 1.1 Protocol"
	soap12ProtocolVersion = "SOAP 1.2 Protocol"
)

// Envelope is the data structure used to keep the SOAP envelope.
type Envelope struct {
	// XMLName is the serialized name of the soap Envelope object.
	XMLName xml.Name `xml:"soapenv:Envelope"`
	// SoapEnv is the namespace identifier for the soap Envelope.
	SoapEnv string `xml:"xmlns:soapenv,attr"`

	// These are generic namespaces used by all messages.
	XMLNSXsd string `xml:"xmlns:xsd,attr,omitempty"`
	XMLNSXsi string `xml:"xmlns:xsi,attr,omitempty"`

	// Header is used to define headers in soap Envelope.
	Header *Header
	// Body is the soap Envelop body.
	Body *Body
}

// Header is used to hold SOAP header inside Envelope.
type Header struct {
	// XMLName is the serialized name of the Header object.
	XMLName xml.Name `xml:"soapenv:Header"`

	// Headers is an array of envelope headers to send.
	Headers []interface{} `xml:",omitempty"`
}

// Body is a SOAP envelope body.
type Body struct {
	XMLName xml.Name `xml:"soapenv:Body"`
	Fault   *Fault   `xml:",omitempty"`
}

// Fault represents the SOAP fault inside Body.
type Fault struct {
	XMLName xml.Name `xml:"soapenv:Fault"`

	Code        string `xml:"faultcode,omitempty"`
	FaultString string `xml:"faultstring,omitempty"`
	Detail      string `xml:"detail,omitempty"`
}

// GenerateSoapFaultMessage generates and returns a SOAP fault message with given parameters.
func GenerateSoapFaultMessage(protocolVersion string, errorMessage string, errorDescription string, code string) (string, error) {
	var envelope *Envelope
	switch protocolVersion {
	case soap11ProtocolVersion:
		envelope = &Envelope{
			SoapEnv: soap11Namespace,
			Body: &Body{
				Fault: &Fault{
					XMLName:     xml.Name{},
					Code:        "soapenv:Server",
					FaultString: errorMessage,
					Detail:      fmt.Sprintf("%s:%s", code, errorDescription),
				},
			},
		}
	case soap12ProtocolVersion:
		envelope = &Envelope{
			SoapEnv: soap12Namespace,
			Body: &Body{
				Fault: &Fault{
					XMLName:     xml.Name{},
					Code:        "soapenv:Receiver",
					FaultString: errorMessage,
					Detail:      fmt.Sprintf("%s:%s", code, errorDescription),
				},
			},
		}
	default:
		envelope = &Envelope{
			SoapEnv: soap12Namespace,
			Body: &Body{
				Fault: &Fault{
					XMLName:     xml.Name{},
					Code:        "soapenv:Receiver",
					FaultString: errorMessage,
					Detail:      fmt.Sprintf("%s:%s", code, errorDescription),
				},
			},
		}
	}

	msg, err := xml.Marshal(envelope)
	if err != nil {
		logger.LoggerSoapUtils.ErrorC(logging.ErrorDetails{
			Message:   fmt.Sprintf("Error while generating the soap fault message. %s", err.Error()),
			Severity:  logging.MINOR,
			ErrorCode: 4000,
		})
		return xmlHeader + string(msg), err
	}
	return xmlHeader + string(msg), nil
}
