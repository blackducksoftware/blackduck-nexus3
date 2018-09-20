/**
 * blackduck-nexus3
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.blackduck.nexus3.connection;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

@Named
@Singleton
public class HubConnection {

    public HttpURLConnection connectToHub(final HubModel hubModel) throws IOException, NoSuchAlgorithmException, KeyManagementException {
        final URL url = new URL(hubModel.getHubUrl());

        final HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        if ("https".equals(url.getProtocol())) {
            bypassCertificate((HttpsURLConnection) urlConnection);
        }
        urlConnection.setRequestMethod("POST");
        urlConnection.setRequestProperty("Content-Type", "application/json");
        urlConnection.setRequestProperty("Authorization", "token " + hubModel.getApiKey());

        urlConnection.setReadTimeout(hubModel.getTimeout());
        urlConnection.setConnectTimeout(hubModel.getTimeout());

        urlConnection.setDoInput(true);
        urlConnection.setDoOutput(true);

        return urlConnection;
    }

    public HubResponse createProject(final HttpURLConnection urlConnection, final String projectJson) throws IOException {
        try (final OutputStream outputStream = urlConnection.getOutputStream()) {
            final byte[] projectBytes = projectJson.getBytes();
            outputStream.write(projectBytes);
        }

        urlConnection.connect();
        final String responseMessage = urlConnection.getResponseMessage();
        final int responseCode = urlConnection.getResponseCode();
        urlConnection.disconnect();

        final HubResponse hubResponse = new HubResponse(responseCode, responseMessage);

        return hubResponse;
    }

    private void bypassCertificate(final HttpsURLConnection urlConnection) throws NoSuchAlgorithmException, KeyManagementException {
        final X509TrustManager trustManager = new X509TrustManager() {
            @Override
            public void checkClientTrusted(final X509Certificate[] x509Certificates, final String s) throws CertificateException {}

            @Override
            public void checkServerTrusted(final X509Certificate[] x509Certificates, final String s) throws CertificateException {}

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };

        final SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, new X509TrustManager[] { trustManager }, new SecureRandom());
        urlConnection.setSSLSocketFactory(sslContext.getSocketFactory());

        final HostnameVerifier allAllowed = (s, sslSession) -> true;
        urlConnection.setHostnameVerifier(allAllowed);
    }
}
