/*
 * Copyright 2026 Telefonaktiebolaget LM Ericsson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ericsson.bss.cassandra.ecchronos.application;

import com.datastax.oss.driver.api.core.metadata.EndPoint;
import com.ericsson.bss.cassandra.ecchronos.application.config.security.CRLConfig;
import com.ericsson.bss.cassandra.ecchronos.application.config.security.CqlTLSConfig;
import com.ericsson.bss.cassandra.ecchronos.application.utils.CertUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies that hostname verification requested through {@code require_endpoint_verification} is enforced
 * for CQL connections, both with and without CRL checking enabled.
 */
public class TestReloadingCertificateHandlerEndpointVerification
{
    private static final String PASSWORD = "ecctest";
    private static final String STORE_TYPE_JKS = "JKS";
    private static final String HOST = "localhost";
    private static final int PORT = 9042;
    private static final int MAX_HANDSHAKE_ITERATIONS = 200;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final CertUtils certUtils = new CertUtils();
    private String caCert;
    private String caKey;
    private String clientKeyStore;
    private String clientTrustStore;
    private String crlFile;
    private ReloadingCertificateHandler handler;

    @Before
    public void setup() throws Exception
    {
        Date notBefore = Date.from(Instant.now());
        Date notAfter = Date.from(Instant.now().plus(Duration.ofHours(1)));
        caCert = path("ca.crt");
        caKey = path("ca.key");
        certUtils.createSelfSignedCACertificate("ca", notBefore, notAfter, "RSA", caCert, caKey);

        String clientCert = path("client.crt");
        String clientKey = path("client.key");
        certUtils.createCertificate("client", notBefore, notAfter, caCert, caKey, clientCert, clientKey);
        clientKeyStore = path("client.keystore");
        certUtils.createKeyStore(clientCert, clientKey, STORE_TYPE_JKS, PASSWORD, clientKeyStore);
        clientTrustStore = path("client.truststore");
        certUtils.createTrustStore(caCert, STORE_TYPE_JKS, PASSWORD, clientTrustStore);

        crlFile = path("ca.crl");
        certUtils.createCRL(caCert, caKey, crlFile, false, false);
    }

    @After
    public void cleanup() throws Exception
    {
        if (handler != null)
        {
            handler.close();
        }
    }

    @Test
    public void testCertificateForOtherHostIsRejectedWithCRLEnabled() throws Exception
    {
        handler = new ReloadingCertificateHandler(() -> getTLSConfig(true));

        assertThatThrownBy(() -> handshake(handler.newSslEngine(endPoint()), serverEngine("wrong.example")))
                .isInstanceOf(SSLException.class)
                .hasStackTraceContaining("No name matching " + HOST);
    }

    @Test
    public void testCertificateForOtherHostIsRejectedWithCRLDisabled() throws Exception
    {
        handler = new ReloadingCertificateHandler(() -> getTLSConfig(false));

        assertThatThrownBy(() -> handshake(handler.newSslEngine(endPoint()), serverEngine("wrong.example")))
                .isInstanceOf(SSLException.class)
                .hasStackTraceContaining("No name matching " + HOST);
    }

    @Test
    public void testCertificateForHostIsAcceptedWithCRLEnabled() throws Exception
    {
        handler = new ReloadingCertificateHandler(() -> getTLSConfig(true));

        SSLEngine client = handler.newSslEngine(endPoint());
        handshake(client, serverEngine(HOST));

        assertThat(client.getSession().getPeerHost()).isEqualTo(HOST);
    }

    private CqlTLSConfig getTLSConfig(final boolean crlEnabled)
    {
        CqlTLSConfig config = new CqlTLSConfig(true, clientKeyStore, PASSWORD, clientTrustStore, PASSWORD);
        config.setStoreType(STORE_TYPE_JKS);
        config.setProtocol("TLSv1.2");
        config.setRequireEndpointVerification(true);
        CRLConfig crlConfig = new CRLConfig();
        crlConfig.setEnabled(crlEnabled);
        crlConfig.setPath(crlFile);
        crlConfig.setStrict(false);
        config.setCRLConfig(crlConfig);
        return config;
    }

    private static EndPoint endPoint()
    {
        EndPoint endPoint = mock(EndPoint.class);
        when(endPoint.resolve()).thenReturn(new InetSocketAddress(HOST, PORT));
        return endPoint;
    }

    private SSLEngine serverEngine(final String commonName) throws Exception
    {
        Date notBefore = Date.from(Instant.now());
        Date notAfter = Date.from(Instant.now().plus(Duration.ofHours(1)));
        String serverCert = path(commonName + ".crt");
        String serverKey = path(commonName + ".key");
        String serverKeyStore = path(commonName + ".keystore");
        certUtils.createCertificate(commonName, notBefore, notAfter, caCert, caKey, serverCert, serverKey);
        certUtils.createKeyStore(serverCert, serverKey, STORE_TYPE_JKS, PASSWORD, serverKeyStore);

        KeyStore keyStore = KeyStore.getInstance(STORE_TYPE_JKS);
        try (InputStream inputStream = new FileInputStream(serverKeyStore))
        {
            keyStore.load(inputStream, PASSWORD.toCharArray());
        }
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, PASSWORD.toCharArray());
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
        SSLEngine engine = sslContext.createSSLEngine();
        engine.setUseClientMode(false);
        return engine;
    }

    private String path(final String fileName)
    {
        return Paths.get(temporaryFolder.getRoot().getPath(), fileName).toString();
    }

    private static void handshake(final SSLEngine client, final SSLEngine server) throws SSLException
    {
        int bufferSize = Math.max(client.getSession().getPacketBufferSize(), server.getSession().getPacketBufferSize());
        ByteBuffer empty = ByteBuffer.allocate(0);
        ByteBuffer clientToServer = ByteBuffer.allocate(bufferSize);
        ByteBuffer serverToClient = ByteBuffer.allocate(bufferSize);
        ByteBuffer application = ByteBuffer.allocate(bufferSize);

        client.beginHandshake();
        server.beginHandshake();
        for (int i = 0; i < MAX_HANDSHAKE_ITERATIONS; i++)
        {
            if (isDone(client) && isDone(server))
            {
                return;
            }
            client.wrap(empty, clientToServer);
            runTasks(client);
            server.wrap(empty, serverToClient);
            runTasks(server);

            clientToServer.flip();
            server.unwrap(clientToServer, application);
            clientToServer.compact();
            runTasks(server);

            serverToClient.flip();
            client.unwrap(serverToClient, application);
            serverToClient.compact();
            runTasks(client);

            application.clear();
        }
        throw new IllegalStateException("TLS handshake did not complete");
    }

    private static boolean isDone(final SSLEngine engine)
    {
        HandshakeStatus status = engine.getHandshakeStatus();
        return status == HandshakeStatus.NOT_HANDSHAKING || status == HandshakeStatus.FINISHED;
    }

    private static void runTasks(final SSLEngine engine)
    {
        Runnable task;
        while ((task = engine.getDelegatedTask()) != null)
        {
            task.run();
        }
    }
}
