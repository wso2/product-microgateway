/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.choreo.connect.tests.util;

import java.io.File;

/**
 * Constants used in test cases.
 */
public class TestConstant {

    public static final String CHARSET_NAME = "UTF-8";
    public static final String HTTP_METHOD_GET = "GET";
    public static final String HTTP_METHOD_POST = "POST";
    public static final String HTTP_METHOD_PUT = "PUT";
    public static final String HTTP_METHOD_PATCH = "PATCH";
    public static final String HTTP_METHOD_DELETE = "DELETE";
    public static final String HTTP_METHOD_OPTIONS = "OPTIONS";
    public static final String HTTP_METHOD_HEAD = "HEAD";
    public static final String ADAPTER_APIS_RESOURCE = "/api/mgw/adapter/0.1/apis";

    public static final String KEY_TYPE_PRODUCTION = "PRODUCTION";
    public static final String KEY_TYPE_SANDBOX = "SANDBOX";
    public static final int DEPLOYMENT_WAIT_TIME = 15000;

    public static final String INVALID_JWT_TOKEN = "eyJ4NXQiOiJNell4TW1Ga09HWXdNV0kwWldObU5EY3hOR1l3WW1NNFpUQTNN" +
            "V0kyTkRBelpHUXpOR00wWkdSbE5qSmtPREZrWkRSaU9URmtNV0ZoTXpVMlpHVmxOZyIsImtpZCI6Ik16WXhNbUZrT0dZd01XST" +
            "BaV05tTkRjeE5HWXdZbU00WlRBM01XSTJOREF6WkdRek5HTTBaR1JsTmpKa09ERmtaRFJpT1RGa01XRmhNelUyWkdWbE5nX1JTM" +
            "jU2IiwiYWxnIjoiUlMyNTYifQ==.eyJhdWQiOiJBT2syNFF6WndRXzYyb2QyNDdXQnVtd0VFZndhIiwic3ViIjoiYWRtaW5AY2F" +
            "yYm9uLnN1cGVyIiwibmJmIjoxNTk2MDA5NTU2LCJhenAiOiJBT2syNFF6WndRXzYyb2QyNDdXQnVtd0VFZndhIiwic2NvcGUiOiJ" +
            "hbV9hcHBsaWNhdGlvbl9zY29wZSBkZWZhdWx0IiwiaXNzIjoiaHR0cHM6Ly9sb2NhbGhvc3Q6OTQ0My9vYXV0aDIvdG9rZW4iLCJr" +
            "ZXl0eXBlIjoiUFJPRFVDVElPTiIsImV4cCI6MTYyNzU0NTU1NiwiaWF0IjoxNTk2MDA5NTU2LCJqdGkiOiIyN2ZkMWY4Ny01ZTI1" +
            "LTQ1NjktYTJkYi04MDA3MTFlZTJjZWMifQ==.otDREOsUUmXuSbIVII7FR59HAWqtXh6WWCSX6NDylVIFfED3GbLkopo6rwCh2EX6" +
            "yiP-vGTqX8sB9Zfn784cIfD3jz2hCZqOqNzSUrzamZrWui4hlYC6qt4YviMbR9LNtxxu7uQD7QMbpZQiJ5owslaASWQvFTJgBmss5" +
            "t7cnurrfkatj5AkzVdKOTGxcZZPX8WrV_Mo2-rLbYMslgb2jCptgvi29VMPo9GlAFecoMsSwywL8sMyf7AJ3y4XW5Uzq7vDGxojD" +
            "am7jI5W8uLVVolZPDstqqZYzxpPJ2hBFC_OZgWG3LqhUgsYNReDKKeWUIEieK7QPgjetOZ5Geb1mA==sdsds";
    public static final String EXPIRED_JWT_TOKEN = "eyJ4NXQiOiJNell4TW1Ga09HWXdNV0kwWldObU5EY3hOR1l3WW1NNFpUQTNNV0" +
            "kyTkRBelpHUXpOR00wWkdSbE5qSmtPREZrWkRSaU9URmtNV0ZoTXpVMlpHVmxOZyIsImtpZCI6Ik16WXhNbUZrT0dZd01XSTBaV05" +
            "tTkRjeE5HWXdZbU00WlRBM01XSTJOREF6WkdRek5HTTBaR1JsTmpKa09ERmtaRFJpT1RGa01XRmhNelUyWkdWbE5nX1JTMjU2Iiwi" +
            "YWxnIjoiUlMyNTYifQ.eyJzdWIiOiJhZG1pbkBjYXJib24uc3VwZXIiLCJhdXQiOiJBUFBMSUNBVElPTiIsImF1ZCI6IkJwM01oZUY" +
            "0NWxpS096Q2RuVEZVREI0THVjZ2EiLCJuYmYiOjE2MDYwNDM3NDEsImF6cCI6IkJwM01oZUY0NWxpS096Q2RuVEZVREI0THVjZ2EiL" +
            "CJzY29wZSI6ImFtX2FwcGxpY2F0aW9uX3Njb3BlIGRlZmF1bHQiLCJpc3MiOiJodHRwczpcL1wvbG9jYWxob3N0Ojk0NDNcL29hdX" +
            "RoMlwvdG9rZW4iLCJleHAiOjE2MDYwNDczNDEsImlhdCI6MTYwNjA0Mzc0MSwianRpIjoiY2NhZWExNjAtMWNlOS00Y2VmLWI4YWQtM" +
            "zJmY2M3NWE4ODk5In0.vy4REXHFnqVSeWxka4f8EPRIocFtq6WI_ayMLCI7P0vAhB1rBenqvgSmE_H_FxRxE1h_tLFm5m1dtyXPvskf" +
            "mb2p0n88p_aqm3jQizxTo-Hd5CfHDMqr1ylovSjp81UI5aoniF9-aFND2TD4Povz8wUeZIQopMPyPKCSXPeW1b7leD1ROqhhvqWBm9-" +
            "-CGdjRlPII2dMR3SYkuoGMQyoCOP2j2pAiP01Q8VseGV9CXZBciDHjCPv-pnP_oTAWCLjqCzFw-fG3Z3C_euEfN2KhMu520UfGuBKz2" +
            "KcdFXFwDUzpLfyAsg8qHDGiMcM88sUc10cvMYQqRYw66SF3EqYWQ";

    public static final String EXPIRED_INTERNAL_KEY_TOKEN = "eyJraWQiOiJnYXRld2F5X2NlcnRpZmljYXRlX2FsaWFzIiwi" +
            "YWxnIjoiUlMyNTYifQ.eyJzdWIiOiJhZG1pbkBjYXJib24uc3VwZXIiLCJpc3MiOiJodHRwczpcL1wvbG9jYWxob3N0Ojk0NDNc" +
            "L29hdXRoMlwvdG9rZW4iLCJrZXl0eXBlIjoiUFJPRFVDVElPTiIsInN1YnNjcmliZWRBUElzIjpbeyJzdWJzY3JpYmVyVGVuYW5" +
            "0RG9tYWluIjpudWxsLCJuYW1lIjoiUGl6emFTaGFja0FQSSIsImNvbnRleHQiOiJcL3Bpenphc2hhY2tcLzEuMC4wIiwicHVib" +
            "GlzaGVyIjoiYWRtaW4iLCJ2ZXJzaW9uIjoiMS4wLjAiLCJzdWJzY3JpcHRpb25UaWVyIjpudWxsfV0sImV4cCI6MTYxNTk1NTI1" +
            "OSwidG9rZW5fdHlwZSI6IkludGVybmFsS2V5IiwiaWF0IjoxNjE1ODk1MjU5LCJqdGkiOiI3YTQ2MmZhNy1hNGE2LTQ2Y2ItOTQ3" +
            "My0yZGQ5MTM1YzM1NmQifQ.iYNnaFrkZqw6JexLYDz8O68iueUoeDnCVpsCpKXabgOj1eLUaSrwDeW_Blg2fDkSO-NA_V9ESmtBXYg" +
            "UpE_gDNy8jRRyc-PMt30zphkiqBcyxU2vhEqRz9ne37yVGlq4hLWrlaRDZNIzj1PniLGA7Y_fLiuq_SvLoh6Zz4tm-p4eIeDunaDkK" +
            "lFn0V_NFOTKiTwue6WAJutP2gEy3Fd5__cxaxjZ6GUbntVmddMhCQk0-0HN5843CzpSVhW8OpAi9QOCKC6HIq8XnJHzyw06juQOKD" +
            "PCi2qmQ-LlCQlBQrA4ECIFBzA_IqihOwymTPJHuqxPhGkJx1WfQ1oSb03Lxw";

    public static final String EXPIRED_API_KEY_TOKEN = "eyJ4NXQiOiJOVGRtWmpNNFpEazNOalkwWXpjNU1tWm1PRGd3TVRFM01XWXd" +
            "OREU1TVdSbFpEZzROemM0WkE9PSIsImtpZCI6ImdhdGV3YXlfY2VydGlmaWNhdGVfYWxpYXMiLCJ0eXAiOiJKV1QiLCJhbGciOiJSU" +
            "zI1NiJ9.eyJzdWIiOiJhZG1pbkBjYXJib24uc3VwZXIiLCJhcHBsaWNhdGlvbiI6eyJvd25lciI6ImFkbWluIiwidGllclF1b3RhVH" +
            "lwZSI6bnVsbCwidGllciI6IlVubGltaXRlZCIsIm5hbWUiOiJBUElLZXlUZXN0QXBwIiwiaWQiOjE0LCJ1dWlkIjoiNGZlZjM5NDIt" +
            "NjcyNy00NjA5LWFhMzctYzY2MWEwOTQ0NmFhIn0sImlzcyI6Imh0dHBzOlwvXC9hcGltOjk0NDNcL29hdXRoMlwvdG9rZW4iLCJ0aW" +
            "VySW5mbyI6eyJVbmxpbWl0ZWQiOnsidGllclF1b3RhVHlwZSI6InJlcXVlc3RDb3VudCIsImdyYXBoUUxNYXhDb21wbGV4aXR5Ijow" +
            "LCJncmFwaFFMTWF4RGVwdGgiOjAsInN0b3BPblF1b3RhUmVhY2giOnRydWUsInNwaWtlQXJyZXN0TGltaXQiOjAsInNwaWtlQXJyZX" +
            "N0VW5pdCI6bnVsbH19LCJrZXl0eXBlIjoiUFJPRFVDVElPTiIsInN1YnNjcmliZWRBUElzIjpbeyJzdWJzY3JpYmVyVGVuYW50RG9t" +
            "YWluIjoiY2FyYm9uLnN1cGVyIiwibmFtZSI6IkFQSUtleVRlc3RBUEkiLCJjb250ZXh0IjoiXC9hcGlLZXlcLzEuMC4wIiwicHVibG" +
            "lzaGVyIjoiYWRtaW4iLCJ2ZXJzaW9uIjoiMS4wLjAiLCJzdWJzY3JpcHRpb25UaWVyIjoiVW5saW1pdGVkIn1dLCJleHAiOjE2MzI3" +
            "NDA5MjgsInRva2VuX3R5cGUiOiJhcGlLZXkiLCJpYXQiOjE2MzI3NDA5MjcsImp0aSI6ImRlYTJjY2RhLTNjZTctNGYzOC04MDQ4LT" +
            "I1NzMwYjk0NWI3YiJ9.M_EpRWG8Bd1ekIUNz8mAtoeIvD-AMIoCZjAubNidFX-NXxFAkCmspLXvDwle9Do_tVMYZoEWjUAfHDqJWo8" +
            "TaLYjSc3goYvHoHAnv_0L0KQaveJYgv7nSQJ343bRVjDocjRZ0-iRY7CryoupoHPd1i0MGTqlWU04JUOadYhtwSUmw7PXnC-wXHGwb" +
            "UmONQ6_gbAAzEOEXD2K6yTfy16KeZvQTdv-zZJzb6ULi0kAuG4txJh38WMagHtu3fuhKm2KIDrflOZEKWmThU6HqB8aoxROABDlId1" +
            "VS_RbajyF8WfeZDv7W_tqNYbKbJVXxSJFFKfPeqL1A1N5po1dfLByPA==";

    public static final String CLIENT_CERT = "-----BEGIN CERTIFICATE-----MIIEnjCCAoYCFEBlls4RcEF1LLuOu0WK4Ng3rbxmMA" +
            "0GCSqGSIb3DQEBCwUAMIGKMQswCQYDVQQGEwJVUzETMBEGA1UECAwKQ2FsaWZvcm5pYTEWMBQGA1UEBwwNU2FuIEZyYW5jaXNjbzEM" +
            "MAoGA1UECgwDQUJDMRAwDgYDVQQLDAdGaW5hbmNlMRAwDgYDVQQDDAdhYmMuY29tMRwwGgYJKoZIhvcNAQkBFg1hZG1pbkBhYmMuY2" +
            "9tMB4XDTIyMDIwOTA1MTgxNFoXDTIzMDYyNDA1MTgxNFowgYsxCzAJBgNVBAYTAlVTMREwDwYDVQQIDAhOZXcgWW9yazEWMBQGA1UE" +
            "BwwNTmV3IFlvcmsgQ2l0eTEMMAoGA1UECgwDWFlaMRMwEQYDVQQLDApFbmdpbmVlcm5nMRAwDgYDVQQDDAd4eXouY29tMRwwGgYJKo" +
            "ZIhvcNAQkBFg1hZG1pbkB4eXouY29tMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArdpOkXeYHD22uXKI6BOIRMYV8y3h" +
            "3/zgr+Ah861cLdtoQnFA/TqueQ3ua0loTyspcct3sgXTp9F49x86ebpv2rYTTXe5r/iSr4c8IhkyoDcMM4+X+uVijLZGfnq0hA+qdG" +
            "5FcCfucs7uP3QGQw21fZdGE5MoGt6joMDhG9T6WSsEuBlnVcTN+0tsr7fTa6qpEoo2uLZ+tCxaF31NIb+8a6gyJz1z4AC5CJIXpXaY" +
            "lWyFMW0878u0cbVpXxgTeuxpA7LeH8RYAuJN+nli1Lix5x/JqZ+JvxlvB8BpCTyBitEDcMfU1EUtQz3UxtqILMnBtc8FALLDnS4a35" +
            "V0Vpx+pQIDAQABMA0GCSqGSIb3DQEBCwUAA4ICAQAaOzFQB+OibydS+2gwXhWJGt4bMEhmYh6v1VNf27VYv84RtfY+rECixAkxPYOJ" +
            "0L8685u/XMs6g48wVknaj6VYDYZJyCDYCnjw0q1+WwFi1b5jryFVR5mvHxjCbi3rlNP65xra4VOPeGCXfJzyocqvZ3UGPyY0rx9Ax0" +
            "Uwt2ONmsBY6JIDjRZQLGLlyi0ZRbA4AIayBdBMrA8LvcsewfEXPYXp3vP2QEw1izaO1Jqh4C5yG2eKYooLfMFR+fVNsPbxEKV7YR4J" +
            "iKFrbybJM4mjeYjQjcZuaLonCh0UnmsaV+2b6jEE5SjjE+Gt05jkk2kYN8CtwPxIyeYepCCEp2KAq9xleblxW7bo9Vk67eZ8Z1+FCY" +
            "dkHZdr04JAp2gxuv9lpCyBRee1291yavAk5yp1GYlzC7pyzgwc7N34LnjCI1it1wTNq/wiCEus8w2Toq0fxm8dmx5q91zymxn8nTrd" +
            "0/YmwdmVHpoe6F03s3LUUYh0pYqufInYXsjMh0CVpKHBGl/xRJxlzmRFwO+GEams7PI+ltvyQw7mtBldGXYy1BVTqs/vItD4vo7ooA" +
            "tAjuVqLPItN6csPb6R94w2edmrwQmaLxklkA/Lez+Rc1oOJp8u+ChzmhIi63AHwlJjYgamw195U4wCZKjobxqzOcwe8/AU0NM9KYCd" +
            "5QoQYNZgxw==-----END CERTIFICATE-----";

    public static final String INVALID_CLIENT_CERT = "-----BEGIN CERTIFICATE-----MIIESDCCAjACFBGKEeR93EUJwx69IzikB29U" +
            "GIlcMA0GCSqGSIb3DQEBCwUAMH4xCzAJBgNVBAYTAlNMMQswCQYDVQQIDAJOQzEMMAoGA1UEBwwDQW51MRAwDgYDVQQKDAdhbnUuY2" +
            "9tMRAwDgYDVQQLDAdmaW5hbmNlMRAwDgYDVQQDDAd0aGFydWthMR4wHAYJKoZIhvcNAQkBFg90aGFydWthQGFudS5jb20wHhcNMjIw" +
            "NDIxMDgwODUwWhcNMjMwOTAzMDgwODUwWjBDMQswCQYDVQQGEwJJTjELMAkGA1UECAwCTVIxFTATBgNVBAoMDEluZE9yZywgSW5jLj" +
            "EQMA4GA1UEAwwHaW5kLmNvbTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAM9DrhWez1JFBs9B4f3LazluwP2kp6JYio3l" +
            "JECWBoSGK64qMKiSzip2DDM6qf4UUcsJC6ofPreRqCQ487N9yT/LDNHqxOHMi5/m/g3480/2sK87RMiTYkWKH9DscEZyFbD3Ol0yXf" +
            "Nk1Zbe8tm30Pci7sRo0QTquVRewM5wDgDKvwGVrXBKDVUHlTbmM/XgVT/xgk9CZgHzXqMSH9tzKxqiUdFycj7kHTe1F7NL9QJsNuTh" +
            "cR1EvL07cxBBHe+XUc6GQv4SzW/QQgljWT663osfqTMNwbazh0puQx6QGWK00taAJ3a3lTEw5um6dthWG811UDgS7VlYMB3syfc4gB" +
            "ECAwEAATANBgkqhkiG9w0BAQsFAAOCAgEAOoBVhNdbZhrkMnkEGUFUWVjmWrcdBZAABZnzoLhf2z0TxkBDyoLecCuL1VYojVqU+hx0" +
            "y7Fhs19zW08+kTpfnfocgawENGi1DiRit9n4NahpNMEei1avLP7bjK9TWNKgxub0fj7Zh4yO3+7mpYhc5jhAPh6WJbQutiCZlwlsoL" +
            "OaD2xkJhwx6QFtZxKTRemzFG10h7nhNbvPEEKzPtKgOiQh8XlqOsBn3eKSzIL+gPX93lOTNdhKdAs5+44tkw6GLY+RoSYlnFMEW4qE" +
            "pQZ0S8PiQzM1ieT5M7AR46hQ/saSNZYYS0mrlWrMGfvsU4vMBuwQEBYi/uVgkQlGWE5WMberGf/Se67fqPtOk96MvIt8vbCQtQHuWG" +
            "ezZKZmr+VCoWu0ro73skYyrLAd+ONNXF/o2Nf1j61t5bvyDGrroQ3kPXpppYY6mAemIWU6JE4hAwSrJ7aQnFCa4zsAeM5q0blZs90O" +
            "tGIFDyNqzsf8nFtUqFk+dPmg5WUDJkT9/Di6oUWHCnYSvuYVs8kCOYUoI2UxwxgAt41/n7VfvZjwecwmyiJ12ryaXlNEV4AtUlzdoK" +
            "OepULnPOtwhNKhjVP9U5p6JotaffsXT58TrIIH/StUJTxk/CmzeD/MtySLlVQgGeyjLOtQQL+I9nbbcHpaA01NjwGJ+tmHrbt2HnEj" +
            "boA=-----END CERTIFICATE-----";

    public static final String MUTUAL_SSL_API_KEY = "eyJ4NXQiOiJOMkpqTWpOaU0yRXhZalJrTnpaalptWTFZVEF4Tm1GbE5qZ" +
            "zRPV1UxWVdRMll6YzFObVk1TlE9PSIsImtpZCI6ImdhdGV3YXlfY2VydGlmaWNhdGVfYWxpYXMiLCJ0eXAiOiJKV1QiLCJhbG" +
            "ciOiJSUzI1NiJ9.eyJzdWIiOiJhZG1pbkBjYXJib24uc3VwZXIiLCJhcHBsaWNhdGlvbiI6eyJvd25lciI6ImFkbWluIiwidG" +
            "llclF1b3RhVHlwZSI6bnVsbCwidGllciI6IlVubGltaXRlZCIsIm5hbWUiOiJEZWZhdWx0QXBwbGljYXRpb24iLCJpZCI6MSw" +
            "idXVpZCI6ImQ3NGNmMTkzLTE5MDQtNDRmMS1iOTYzLTc5OGZmNzhmZDE2ZSJ9LCJpc3MiOiJodHRwczpcL1wvYXBpbTo5NDQ0" +
            "XC9vYXV0aDJcL3Rva2VuIiwidGllckluZm8iOnsiVW5saW1pdGVkIjp7InRpZXJRdW90YVR5cGUiOiJyZXF1ZXN0Q291bnQiL" +
            "CJncmFwaFFMTWF4Q29tcGxleGl0eSI6MCwiZ3JhcGhRTE1heERlcHRoIjowLCJzdG9wT25RdW90YVJlYWNoIjp0cnVlLCJzcG" +
            "lrZUFycmVzdExpbWl0IjowLCJzcGlrZUFycmVzdFVuaXQiOm51bGx9fSwia2V5dHlwZSI6IlBST0RVQ1RJT04iLCJwZXJtaXR" +
            "0ZWRSZWZlcmVyIjoiIiwic3Vic2NyaWJlZEFQSXMiOlt7InN1YnNjcmliZXJUZW5hbnREb21haW4iOiJjYXJib24uc3VwZXIi" +
            "LCJuYW1lIjoiTXV0dWFsU1NMIiwiY29udGV4dCI6IlwvdjJcLzEuMC41IiwicHVibGlzaGVyIjoiYWRtaW4iLCJ2ZXJzaW9uI" +
            "joiMS4wLjUiLCJzdWJzY3JpcHRpb25UaWVyIjoiVW5saW1pdGVkIn1dLCJ0b2tlbl90eXBlIjoiYXBpS2V5IiwicGVybWl0dG" +
            "VkSVAiOiIiLCJpYXQiOjE2NTYzMDk2MjMsImp0aSI6IjAxZmQyMDYxLTVlMjctNDg1ZS05MjY0LTdmNjUzNTBmNTE2MiJ9.Rn" +
            "cQXeiBt2olmWben0ZPm1tKq8o8SDqg7sSbNUTK7T0zT6mWHO7z6Oyp3rzJZKjL5xTnDgsTzMiTCfF2maDFgUHBtGZ3m_fi7iB" +
            "3lUi8Y-eu8bVNo9eJAQ6XV4-kx43HKx57joyShjvZvmfrk43VI5IGWIfwg4TSZGE3yVnsuAO6WukW2fMzRUi1p43maVTZxMCQ" +
            "8WNXnMS9FNQMR7Kxt5wIIPEv2iwQWTQMY3n7rbpmFdQG-3zRNenthuhTvG618lb7V8gfyGSXMJ0Dxn_RrbxqMS2UMC3npbiX6" +
            "PG7HJIsR_3PM1HQeKWYxRfTCoTNBBeLhSYoFJWtgfFh0Mqbfw==";

    public static final String MUTUAL_SSL_INVALID_API_KEY = "eyJ4NXQiOiJOMkpqTWpOaU0yRXhZalJrTnpaalptWTFZVEF4T" +
            "m1GbE5qZzRPV1UxWVdRMll6YzFObVk1TlE9PSIsImtpZCI6ImdhdGV3YXlfY2VydGlmaWNhdGVfYWxpYXMiLCJ0eXAiOiJKV1" +
            "QiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJhZG1pbkBjYXJib24uc3VwZXIiLCJhcHBsaWNhdGlvbiI6eyJvd25lciI6ImFkbW" +
            "luIiwidGllclF1b3RhVHlwZSI6bnVsbCwidGllciI6IlVubGltaXRlZCIsIm5hbWUiOiJEZWZhdWx0QXBwbGljYXRpb24iLCJ" +
            "pZCI6MSwidXVpZCI6ImE0MDNiNTM1LTIwODUtNDRkYy1iOWFkLTJjZDY4ODZiODM3NyJ9LCJpc3MiOiJodHRwczpcL1wvYXBp" +
            "bTo5NDQ0XC9vYXV0aDJcL3Rva2VuIiwidGllckluZm8iOnsiVW5saW1pdGVkIjp7InRpZXJRdW90YVR5cGUiOiJyZXF1ZXN0Q" +
            "291bnQiLCJncmFwaFFMTWF4Q29tcGxleGl0eSI6MCwiZ3JhcGhRTE1heERlcHRoIjowLCJzdG9wT25RdW90YVJlYWNoIjp0cn" +
            "VlLCJzcGlrZUFycmVzdExpbWl0IjowLCJzcGlrZUFycmVzdFVuaXQiOm51bGx9fSwia2V5dHlwZSI6IlNBTkRCT1giLCJwZXJ" +
            "taXR0ZWRSZWZlcmVyIjoiIiwic3Vic2NyaWJlZEFQSXMiOlt7InN1YnNjcmliZXJUZW5hbnREb21haW4iOiJjYXJib24uc3Vw" +
            "ZXIiLCJuYW1lIjoiTXV0dWFsU1NMIiwiY29udGV4dCI6IlwvdjJcLzEuMC41IiwicHVibGlzaGVyIjoiYWRtaW4iLCJ2ZXJza" +
            "W9uIjoiMS4wLjUiLCJzdWJzY3JpcHRpb25UaWVyIjoiVW5saW1pdGVkIn1dLCJ0b2tlbl90eXBlIjoiYXBpS2V5IiwicGVybW" +
            "l0dGVkSVAiOiIiLCJpYXQiOjE2NTU3OTE4NzIsImp0aSI6ImYwZjE3ZjI1LTUzZWQtNDQ4NS04NDMwLTE4YjhiMTg2N2UxZSJ" +
            "9.HU8Ng_8Z06dN70PbhTaKHJSs1xZY5Pmcu6RAVfUWsVgOe-YPjuN3ZupC3oEb57pazGbqpgR9_qOE7RLM5lyVwgrei1cwoYh" +
            "0MTEwbT3lI17QqtUfxw1R_rzXAuUZ9LapsPxAIVGiwSL_-_f8T0wyBnWBhjPSq3IrS_wLGwcE_MfBRrZYkFLLfWle2XBrUDRn" +
            "-9GQKfO_QXJq24JImsyNwy37HDmRu2xWeV6Km9z6xSiVLqXo3-74UKgqbSg8edqjpjD8VI4MJftX11zXAzwntKgB3gyloBtd3" +
            "LY5uAVGCziHW0zfEa44tXBYGZZc21QvyuaRKZBywjA1TNZ-TXJ-fQ==";

    public static final String MUTUAL_SSL_OPTIONAL_API_KEY = "eyJ4NXQiOiJOMkpqTWpOaU0yRXhZalJrTnpaalptWTFZVEF4" +
            "Tm1GbE5qZzRPV1UxWVdRMll6YzFObVk1TlE9PSIsImtpZCI6ImdhdGV3YXlfY2VydGlmaWNhdGVfYWxpYXMiLCJ0eXAiOiJKV" +
            "1QiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJhZG1pbkBjYXJib24uc3VwZXIiLCJhcHBsaWNhdGlvbiI6eyJvd25lciI6ImFkb" +
            "WluIiwidGllclF1b3RhVHlwZSI6bnVsbCwidGllciI6IlVubGltaXRlZCIsIm5hbWUiOiJEZWZhdWx0QXBwbGljYXRpb24iLC" +
            "JpZCI6MSwidXVpZCI6IjBiNzhlNjMxLTdjZWEtNDk4My1hNjhmLWEyNzc5NjMyMTUwMyJ9LCJpc3MiOiJodHRwczpcL1wvYXB" +
            "pbTo5NDQ0XC9vYXV0aDJcL3Rva2VuIiwidGllckluZm8iOnsiVW5saW1pdGVkIjp7InRpZXJRdW90YVR5cGUiOiJyZXF1ZXN0" +
            "Q291bnQiLCJncmFwaFFMTWF4Q29tcGxleGl0eSI6MCwiZ3JhcGhRTE1heERlcHRoIjowLCJzdG9wT25RdW90YVJlYWNoIjp0c" +
            "nVlLCJzcGlrZUFycmVzdExpbWl0IjowLCJzcGlrZUFycmVzdFVuaXQiOm51bGx9fSwia2V5dHlwZSI6IlBST0RVQ1RJT04iLC" +
            "JwZXJtaXR0ZWRSZWZlcmVyIjoiIiwic3Vic2NyaWJlZEFQSXMiOlt7InN1YnNjcmliZXJUZW5hbnREb21haW4iOiJjYXJib24" +
            "uc3VwZXIiLCJuYW1lIjoiTXV0dWFsU1NMT3B0aW9uYWwiLCJjb250ZXh0IjoiXC92MlwvMS4wLjUiLCJwdWJsaXNoZXIiOiJh" +
            "ZG1pbiIsInZlcnNpb24iOiIxLjAuNSIsInN1YnNjcmlwdGlvblRpZXIiOiJVbmxpbWl0ZWQifV0sInRva2VuX3R5cGUiOiJhc" +
            "GlLZXkiLCJwZXJtaXR0ZWRJUCI6IiIsImlhdCI6MTY1NjMyNTMwOCwianRpIjoiNTVlZTgyY2EtY2FkZS00NGIxLThlOTAtOT" +
            "M0OWRmYTI5MjQ3In0=.UZx3L-ph7FvpCdCP46-Pc2ydO1Yd5Zi12llQhtityeCOGiZFJdfBzr5moSojiEz53AoYMc5voikocT" +
            "TNGcTMEXWV41onO2kzmctUr1A0421652Hm7gHh2SDypcRD8uW6Jp7OSRNnRkYExKchn8lk8l8VkuijYD7UVgLghTHbBiSKER1" +
            "49mQJiZWI5fntM8YIZ9TtznD9hy26ZWFHpcq8jGQPAsOFV_cAUiLKZa-AY_lOc3Y5fcqqpRkqrTmkF4mmIvHp-H8UEjnVgu8W" +
            "KlZRrSdylPCUEI7F963U1YNQVzcvov37XIoiBhKiaRXarO5zzXALv2il9TEivlC2VwbKBw==";

    public static final String MUTUAL_SSL_OPTIONAL_INVALID_API_KEY = "eyJ4NXQiOiJOMkpqTWpOaU0yRXhZalJrTnpaalpt" +
            "WTFZVEF4Tm1GbE5qZzRPV1UxWVdRMll6YzFObVk1TlE9PSIsImtpZCI6ImdhdGV3YXlfY2VydGlmaWNhdGVfYWxpYXMiLCJ0e" +
            "XAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9eyJzdWIiOiJhZG1pbkBjYXJib24uc3VwZXIiLCJhcHBsaWNhdGlvbiI6eyJvd25lci" +
            "I6ImFkbWluIiwidGllclF1b3RhVHlwZSI6bnVsbCwidGllciI6IlVubGltaXRlZCIsIm5hbWUiOiJEZWZhdWx0QXBwbGljYXR" +
            "pb24iLCJpZCI6MSwidXVpZCI6ImE0MDNiNTM1LTIwODUtNDRkYy1iOWFkLTJjZDY4ODZiODM3NyJ9LCJpc3MiOiJodHRwczpc" +
            "L1wvYXBpbTo5NDQ0XC9vYXV0aDJcL3Rva2VuIiwidGllckluZm8iOnsiVW5saW1pdGVkIjp7InRpZXJRdW90YVR5cGUiOiJyZ" +
            "XF1ZXN0Q291bnQiLCJncmFwaFFMTWF4Q29tcGxleGl0eSI6MCwiZ3JhcGhRTE1heERlcHRoIjowLCJzdG9wT25RdW90YVJlYW" +
            "NoIjp0cnVlLCJzcGlrZUFycmVzdExpbWl0IjowLCJzcGlrZUFycmVzdFVuaXQiOm51bGx9fSwia2V5dHlwZSI6IlNBTkRCT1g" +
            "iLCJwZXJtaXR0ZWRSZWZlcmVyIjoiIiwic3Vic2NyaWJlZEFQSXMiOlt7InN1YnNjcmliZXJUZW5hbnREb21haW4iOiJjYXJi" +
            "b24uc3VwZXIiLCJuYW1lIjoiTXV0dWFsU1NMT3B0aW9uYWwiLCJjb250ZXh0IjoiXC92MlwvMS4wLjUiLCJwdWJsaXNoZXIiO" +
            "iJhZG1pbiIsInZlcnNpb24iOiIxLjAuNSIsInN1YnNjcmlwdGlvblRpZXIiOiJVbmxpbWl0ZWQifV0sInRva2VuX3R5cGUiOi" +
            "JhcGlLZXkiLCJwZXJtaXR0ZWRJUCI6IiIsImlhdCI6MTY1NTc5MTg3MiwianRpIjoiZjBmMTdmMjUtNTNlZC00NDg1LTg0MzA" +
            "tMThiOGIxODY3ZTFlIn0dTwoZ06dNQ9uFNhwWWEBVFlgOew93ZkLegRtaZgR9ElwK3otXMHQxMTBtPSNe0KofClECGUAhUSJP" +
            "TDIGdTPSq3IrSwsbBwRGWFJ9aV5wa1A0Z9GQKUFyatuCSMyNLRw5bFZ5XiguUCptKDx52qMwVAwlV1wDPCcBDBtd3LY5BUYLO" +
            "FtMEThwWBlcVC8pcjA1TH5Ncn59";

    public static final String CLIENT_CERT_HEADER = "X-WSO2-CLIENT-CERTIFICATE";
    public static final int GATEWAY_LISTENER_HTTPS_PORT = 9095;
    public static final int GATEWAY_LISTENER_HTTP_PORT = 9090;
    public static final int ADAPTER_PORT = 9843;
    public final static int MOCK_SERVER_PORT = 2383;
    public final static int MOCK_SERVER2_PORT = 2390;
    public final static int APIM_SERVLET_TRP_HTTPS_PORT = 9444;
    public final static int APIM_SERVLET_TRP_HTTP_PORT = 9764;
    public static final int MOCK_BACKEND_HTTP2_CLEAR_TEXT_SERVER_PORT = 2350;
    public static final int MOCK_BACKEND_HTTP2_SECURED_SERVER_PORT = 2351;
    public final static int MOCK_GRAPHQL_SERVER_PORT = 2320;

    public static final String MOCK_BACKEND_BASEPATH = "/v2";
    public static final String MOCK_GRAPHQL_BASEPATH = "/gql";

    public static final int INVALID_CREDENTIALS_CODE = 900901;
    public static final String RESOURCE_FORBIDDEN_CODE = "900908";

    public static final String LINE = "\r\n";
    public static final String URL_SEPARATOR = "/";

    public static final String TEST_RESOURCES_PATH = File.separator + "test-classes";
    public static final String CONFIGS_DIR = File.separator + "configs";
    public static final String CERTS_DIR = File.separator + "certs";
    public static final String CONF_DIR = File.separator + "conf";
    public static final String DATABASE_DIR = File.separator + "database";
    public static final String TEST_DOCKER_COMPOSE_DIR = File.separator + "dockerCompose";
    public static final String DOCKER_COMPOSE_DIR = File.separator + "docker-compose";
    public static final String RESOURCES_DIR = File.separator + "resources";
    public static final String SECURITY_DIR = File.separator + "security";
    public static final String CC_TEMP_PATH = File.separator + "choreo-connect-temp";

    public static final String CONFIG_TOML_PATH = CONF_DIR + File.separator + "config.toml";
    public static final String DEPLYMNT_TOML_PATH = CONF_DIR + File.separator + "deployment.toml";
    public static final String DOCKER_COMPOSE_YAML_PATH = File.separator + "docker-compose.yaml";
    public static final String CA_CERTS_FILE = File.separator + "ca-certificates.crt";

    public static final String ENFORCER_DIR_NAME = "enforcer";
    public static final String ROUTER_TRUSTSTORE_DIR = RESOURCES_DIR + File.separator + "router"
            + SECURITY_DIR + File.separator + "truststore";
    public static final String ENFORCER_TRUSTSTORE_DIR = RESOURCES_DIR + File.separator + ENFORCER_DIR_NAME
            + SECURITY_DIR + File.separator + "truststore";
    public static final String DOCKER_COMPOSE_CC_DIR = DOCKER_COMPOSE_DIR + File.separator + "choreo-connect";
    public static final String DROPINS_FOLDER_PATH = DOCKER_COMPOSE_DIR + RESOURCES_DIR
            + File.separator + ENFORCER_DIR_NAME + File.separator + "dropins";
    public static final String STARTUP_APIS_DIR = RESOURCES_DIR + File.separator
            + "adapter" + File.separator + "artifacts" + File.separator + "apis";
    public static final String JACOCO_EXEC_NAME = "aggregate.exec";
    public static final String ENFORCER_PARENT_DIR_NAME = "enforcer-parent";
    public static final String TARGET_DIR_NAME = "target";
    public static final String CODECOV_AGGREGATE_REPORT_DIR_NAME = "coverage-aggregate-reports";



    public static final String HEALTH_ENDPOINT_RESPONSE = "{\"status\": \"healthy\"}";

    // apim related constants
    public static final String APIM_SERVICE_NAME_IN_DOCKER_COMPOSE = "apim";
    public static final String SUPER_TENANT_DOMAIN = "carbon.super";
    public static final String DEFAULT_TOKEN_VALIDITY_TIME = "36000";
    public static final String ADMIN_USERNAME = "admin";
    public static final String ADMIN_PASSWORD = "admin";
    public static final String LOCAL_HOST_NAME = "localhost";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String INTERNAL_KEY_HEADER = "Internal-Key";
    public static final String CONTENT_TYPE_HEADER = "content-type";
    public static final String SOAP_ACTION_HEADER = "SOAPAction";

    //apim instances names
    public static final String AM_PRODUCT_GROUP_NAME = "APIM";
    public static final String AM_ALL_IN_ONE_INSTANCE = "all-in-one";

    public static final String ENABLE_ENFORCER_CUSTOM_FILTER = "enable-custom-filter";

    public static final class THROTTLING {
        public static final String ADVANCED = "advanced";
        public static final String APPLICATION = "application";
        public static final String SUBSCRIPTION = "subscription";
    }

    public static final class APIM_INTERNAL_ROLE {
        public static final String SUBSCRIBER = "Internal/subscriber";
        public static final String PUBLISHER = "Internal/publisher";
        public static final String CREATOR = "Internal/creator";
        public static final String EVERYONE = "Internal/everyone";

        public APIM_INTERNAL_ROLE() {
        }
    }

    public static class GRANT_TYPE {
        public static final String PASSWORD = "password";
        public static final String CLIENT_CREDENTIAL = "client_credentials";
        public static final String AUTHORIZATION_CODE = "authorization_code";
        public static final String REFRESH_CODE = "refresh_token";
        public static final String SAML2 = "urn:ietf:params:oauth:grant-type:saml2-bearer";
        public static final String NTLM = "iwa:ntlm";
        public static final String IMPLICIT = "implicit";
        public static final String JWT = "urn:ietf:params:oauth:grant-type:jwt-bearer";

        public GRANT_TYPE() {
        }
    }

    public static class RESOURCE_TIER {
        public static final String UNLIMITED = "Unlimited";

        public RESOURCE_TIER() {
        }
    }

    public static class API_TIER {
        public static final String UNLIMITED = "Unlimited";

        public API_TIER() {
        }
    }

    public static final class APPLICATION_TIER {
        public static final String UNLIMITED = "Unlimited";

        public APPLICATION_TIER() {
        }
    }

    public static final class SUBSCRIPTION_TIER {
        public static final String UNLIMITED = "Unlimited";
        public static final String GOLD = "Gold";
        public static final String SILVER = "Silver";
        public static final String BRONZE = "Bronze";
        public static final String UNAUTHENTICATED = "Unauthenticated";

        public SUBSCRIPTION_TIER() {
        }
    }

    public static final class CONTENT_TYPES {
        public static final String TEXT_XML = "text/xml";
        public static final String SOAP_XML = "application/soap+xml";
    }

    public static final class API_TYPES {
        public static final String SOAP = "SOAP";
        public static final String GRAPHQL = "GRAPHQL";
    }

    public static final class SOAP_ENVELOPES {
        public static final String SOAP11_SAMPLE_REQ_PAYLOAD = "<soap:Envelope\n" +
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
        public static final String SOAP12_SAMPLE_REQ_PAYLOAD = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
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
    }
}
