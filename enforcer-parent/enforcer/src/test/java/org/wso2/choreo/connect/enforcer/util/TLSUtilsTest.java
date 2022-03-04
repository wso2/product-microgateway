/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.enforcer.util;

import org.junit.Assert;
import org.junit.Test;
import org.wso2.choreo.connect.enforcer.commons.exception.EnforcerException;

import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;

public class TLSUtilsTest {
    private static final String certsDir = "certs";
    private static final String trustStoreWithSingleFile = "truststoreSingle";
    private static final String trustStoreWithSeparateCerts = "truststoreSeparate";
    private static final String certWithAdditionalNewLine = "mg.pem";
    private static final String certWithTwoCerts = "certWithTwoCerts.pem";

    @Test
    public void testGetCertificateWithAdditionalNewLine() {
        String pemFilePath = TLSUtilsTest.class.getProtectionDomain().getCodeSource().
                getLocation().getPath() + certsDir + File.separator + certWithAdditionalNewLine;
        try {
            X509Certificate cert = (X509Certificate) TLSUtils.getCertificateFromFile(pemFilePath);
            Assert.assertNotNull(cert);
            Assert.assertEquals("13677610512757128594", cert.getSerialNumber().toString());
        } catch (CertificateException | IOException | EnforcerException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetCertificateWithPemHavingTwo() {
        String pemFilePath = TLSUtilsTest.class.getProtectionDomain().getCodeSource().
                getLocation().getPath() + certsDir + File.separator + certWithTwoCerts;
        try {
            X509Certificate cert = (X509Certificate) TLSUtils.getCertificateFromFile(pemFilePath);
            Assert.assertNotNull(cert);
            Assert.assertEquals("13677610512757128594", cert.getSerialNumber().toString());
        } catch (CertificateException | IOException | EnforcerException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testAddCertificatesToTruststoreMultipleCertsInSingleFile() throws KeyStoreException,
            CertificateException, IOException, NoSuchAlgorithmException {
        String trustStoreLocation = TLSUtilsTest.class.getProtectionDomain().getCodeSource().
                getLocation().getPath() + certsDir + File.separator + trustStoreWithSingleFile;
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null);
        Assert.assertEquals(0, trustStore.size());
        TLSUtils.addCertsToTruststore(trustStore, trustStoreLocation);
        Assert.assertEquals(2, trustStore.size());

        ArrayList<String> serialNumList = new ArrayList<>();
        serialNumList.add("1571815843");
        serialNumList.add("13677610512757128594");
        Iterator<String> aliases =  trustStore.aliases().asIterator();
        while (aliases.hasNext()) {
            String alias = aliases.next();
            X509Certificate cert = (X509Certificate)trustStore.getCertificate(alias);
            String serialNum = cert.getSerialNumber().toString();
            Assert.assertTrue(serialNumList.contains(serialNum));
            serialNumList.remove(serialNum);
        }
    }

    @Test
    public void testAddCertificatesToTruststoreCertsInSeparateFiles() throws KeyStoreException,
            CertificateException, IOException, NoSuchAlgorithmException {
        String trustStoreLocation = TLSUtilsTest.class.getProtectionDomain().getCodeSource().
                getLocation().getPath() + certsDir + File.separator + trustStoreWithSeparateCerts;
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null);
        Assert.assertEquals(0, trustStore.size());
        TLSUtils.addCertsToTruststore(trustStore, trustStoreLocation);
        Assert.assertEquals(2, trustStore.size());

        ArrayList<String> serialNumList = new ArrayList<>();
        serialNumList.add("1571815843");
        serialNumList.add("13677610512757128594");
        Iterator<String> aliases =  trustStore.aliases().asIterator();
        while (aliases.hasNext()) {
            String alias = aliases.next();
            X509Certificate cert = (X509Certificate)trustStore.getCertificate(alias);
            String serialNum = cert.getSerialNumber().toString();
            Assert.assertTrue(serialNumList.contains(serialNum));
            serialNumList.remove(serialNum);
        }
    }
}
