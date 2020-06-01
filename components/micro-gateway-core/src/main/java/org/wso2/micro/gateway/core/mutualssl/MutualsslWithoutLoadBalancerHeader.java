// Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.wso2.micro.gateway.core.mutualssl;

import java.io.ByteArrayInputStream;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

/**
 * This class is for getting the certificate alias for a certificate to validate against per API.
 */
public class MutualsslWithoutLoadBalancerHeader {
    public static String getAlias(String certB64) throws CertificateException, KeyStoreException {
        byte[] decoded = Base64.getDecoder().decode(certB64);
        X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(decoded));
        String certificateAlias = LoadKeyStore.trustStore.getCertificateAlias(cert);
        if (certificateAlias != null) {
            return certificateAlias;
        } else {
            return "";
        }
    }

}
