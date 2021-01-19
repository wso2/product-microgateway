/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package svcdiscovery

import (
	"errors"
	"fmt"
	"strings"
	"testing"
	"time"
)

func TestConsulWatcher_Watch(t *testing.T) {
	//conf = {
	//	//Address: "169.254.1.1:8501",
	//	CAPem: []byte("-----BEGIN CERTIFICATE-----\nMIIC7TCCAp" +
	//		"OgAwIBAgIQTJSoYy6c1twuT4+sqb3pGDAKBggqhkjOPQQDAjCBuTEL\nMAkGA1UE" +
	//		"BhMCVVMxCzAJBgNVBAgTAkNBMRYwFAYDVQQHEw1TYW4gRnJhbmNpc2Nv\nMRowGA" +
	//		"YDVQQJExExMDEgU2Vjb25kIFN0cmVldDEOMAwGA1UEERMFOTQxMDUxFzAV\nBgNVBAoT" +
	//		"Dkhhc2hpQ29ycCBJbmMuMUAwPgYDVQQDEzdDb25zdWwgQWdlbnQgQ0Eg\nMTAxNzkz" +
	//		"MjAyOTE3NTQ0MTgwNzUwMjAyMzI0MzkwNTE4ODQzNjcyMB4XDTIxMDEw\nNjAxNTUyNVo" +
	//		"XDTI2MDEwNTAxNTUyNVowgbkxCzAJBgNVBAYTAlVTMQswCQYDVQQI\nEwJDQTEWMBQGA1" +
	//		"UEBxMNU2FuIEZyYW5jaXNjbzEaMBgGA1UECRMRMTAxIFNlY29u\nZCBTdHJlZXQxDjAMBgNVB" +
	//		"BETBTk0MTA1MRcwFQYDVQQKEw5IYXNoaUNvcnAgSW5j\nLjFAMD4GA1UEAxM3Q29uc3VsIEFn" +
	//		"ZW50IENBIDEwMTc5MzIwMjkxNzU0NDE4MDc1\nMDIwMjMyNDM5MDUxODg0MzY3MjBZMBMGBy" +
	//		"qGSM49AgEGCCqGSM49AwEHA0IABJHy\nDhBHMQ14UB2flhUfMceC2fnmhNQ6yu+BCA14XGaA" +
	//		"4dvuL5qa5KVB1n4o9qYvT6oc\nyqX11oGtVEzdShSS30WjezB5MA4GA1UdDwEB/wQEAwIBh" +
	//		"jAPBgNVHRMBAf8EBTAD\nAQH/MCkGA1UdDgQiBCC/6kSjhEwHvONTVNLHlXSNgRTYpPaXC" +
	//		"ROeLjgQ00lBaTAr\nBgNVHSMEJDAigCC/6kSjhEwHvONTVNLHlXSNgRTYpPaXCROeLjgQ00" +
	//		"lBaTAKBggq\nhkjOPQQDAgNIADBFAiEAtYhLzf7K00z5buwCovPe3gwKbQzJzjvLg/xwfUAu" +
	//		"kdgC\nIHUvDrvMdAMpdEeXfJr0H1CsSUt2uwJ6LQr1xDmmPf7e\n-----END CERTIFICATE-----"),
	//	CertPEM: []byte("-----BEGIN CERTIFICATE-----\nMIICpjCCAkygAwIBAgIQIzl4Fy" +
	//		"bvY6NN945INgLYrTAKBggqhkjOPQQDAjCBuTEL\nMAkGA1UEBhMCVVMxCzAJBgNVBAgTAkNBMRYwF" +
	//		"AYDVQQHEw1TYW4gRnJhbmNpc2Nv\nMRowGAYDVQQJExExMDEgU2Vjb25kIFN0cmVldDEOMAwGA1UEE" +
	//		"RMFOTQxMDUxFzAV\nBgNVBAoTDkhhc2hpQ29ycCBJbmMuMUAwPgYDVQQDEzdDb25zdWwgQWdlbnQ" +
	//		"gQ0Eg\nMTAxNzkzMjAyOTE3NTQ0MTgwNzUwMjAyMzI0MzkwNTE4ODQzNjcyMB4XDTIxMDEw\nNjA" +
	//		"5NDEyN1oXDTIyMDEwNjA5NDEyN1owITEfMB0GA1UEAxMWY2xpZW50LmxvY2Fs\nLWRjLmNvbn" +
	//		"N1bDBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABHBiL1uK3uqCtue3\nU1o+f/V+JPgmu8ixz" +
	//		"ioIUFHM+V5xaVgy3k9Kn+BKKehy4Gg6b1jyJzq1rKFwlzRQ\nEMtkvWWjgcwwgckwDgYDVR0P" +
	//		"AQH/BAQDAgWgMB0GA1UdJQQWMBQGCCsGAQUFBwMC\nBggrBgEFBQcDATAMBgNVHRMBAf8EAjA" +
	//		"AMCkGA1UdDgQiBCCH5kmYI9qlaRHactVC\nXVSpMgKG0HPQwV6rixT2kOpgXzArBgNVHSMEJD" +
	//		"AigCC/6kSjhEwHvONTVNLHlXSN\ngRTYpPaXCROeLjgQ00lBaTAyBgNVHREEKzApghZjbG" +
	//		"llbnQubG9jYWwtZGMuY29u\nc3Vsgglsb2NhbGhvc3SHBH8AAAEwCgYIKoZIzj0EAwIDSAAwRQIhALgI1Ae6omC/\nYa8KvA" +
	//		"vUM8HVE/iBB3fXnNM4UJZYxFg1AiBCOYpgDa23VUT8x9VJVuCmCZ02uEzC\nluNAt4BWlvADHA==\n-----END CERTIFICATE-----"),
	//	KeyPEM: []byte("-----BEGIN EC PRIVATE KEY-----\nMHcCAQEEIPBO0X3kFQxjX" +
	//		"1QPO58OzLPVG4to4tBHNorS2SWiLyN1oAoGCCqGSM49\nAwEHoUQDQgAEcGIvW4re6oK257dT" +
	//		"Wj5/9X4k+Ca7yLHOKghQUcz5XnFpWDLeT0qf\n4Eop6HLgaDpvWPInOrWsoXCXNFAQy2S9ZQ" +
	//		"==\n-----END EC PRIVATE KEY-----"),
	//}
	//conf.Address = "169.254.1.1:8501"
	//conf.Scheme = "https"

	//consul:[dc1,dc2].namespace.serviceA.[tag1,tag2];http://abc.com:80
	str := "consul([local-dc].phony.[*],http://localhost:3000)"
	s, _, _ := ParseConsulSyntax(str)
	q, _ := ParseQueryString(s)

	doneChan := make(chan bool)
	nodeInfoChan := ConsulWatcherInstance.Poll(q, doneChan)
	go func() {
		select {
		case <-time.After(7 * time.Second):
			doneChan <- true
		}

	}()
	for {
		select {
		case n, ok := <-nodeInfoChan:
			if !ok {
				return
			}
			fmt.Println("nodeInfo chan:", n)
		}

	}

}

func consulSyntaxBreak(str string) (string, string, error) {
	list := strings.Split(str, ",")
	length := len(list)
	if length < 2 {
		return "", "", errors.New("default host not provided")
	}
	defaultHost := list[length-1]
	defaultHost = strings.Replace(defaultHost, ")", "", 1)
	defaultHost = strings.TrimSpace(defaultHost)

	for i := 0; i < length-1; i++ {
		str += list[i]
		str = strings.Join(list[0:length-1], ",")
	}
	str = strings.Replace(str, "(", "", 1)
	str = strings.Replace(str, ConsulBegin, "", 1)
	str = strings.TrimSpace(str)
	return str, defaultHost, nil

}
