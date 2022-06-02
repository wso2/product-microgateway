package org.wso2.choreo.connect.mockbackend.http2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.channel.ChannelInitializer;

/**
 * Mock HTTP2 server for testing Open API tests.
 */
public class Http2MockBackend {
    private static final Logger logger = LoggerFactory.getLogger(Http2MockBackend.class);

    private final int backendServerPort;
    private boolean secured = false;
    private boolean mtlsEnabled = false;
    private int sleepTime = 5000;
    private boolean h2ContentAggregate = true;

    private String keyStoreName = "backendKeystore.pkcs12";
    private String keyStorePassword = "backend";

    public Http2MockBackend(int port) {
        this.backendServerPort = port;
    }

    public Http2MockBackend(int port, boolean isSecured, boolean mtlsEnabled) {
        this.secured = isSecured;
        this.backendServerPort = port;
        this.mtlsEnabled = mtlsEnabled;
    }

    public void startServer() {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 1024);
            b.group(group)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024);

            b = configureHttp2(b);

            // Start the server.
            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(backendServerPort).sync();

            logger.info("HTTP/2 Server is listening on https://127.0.0.1:" + backendServerPort + '/');

            // Wait until the server socket is closed.
            f.channel().closeFuture().sync();

        } catch (InterruptedException | SSLException | CertificateException e) {
            logger.error("Failed to start the HTTP2 server", e);

        } finally {
            group.shutdownGracefully();
        }
    }

    private ServerBootstrap configureHttp2(ServerBootstrap b) throws SSLException, CertificateException {
        // Configure SSL.
        final SslContext sslCtx;
        if (secured) {
            ApplicationProtocolConfig protocolConfig = new ApplicationProtocolConfig(
                    Protocol.ALPN,
                    SelectorFailureBehavior.NO_ADVERTISE,
                    SelectedListenerFailureBehavior.ACCEPT,
                    ApplicationProtocolNames.HTTP_2, ApplicationProtocolNames.HTTP_1_1);
            SslContextBuilder sslContextBuilder = createSslContextBuilder();
            sslCtx = sslContextBuilder.applicationProtocolConfig(protocolConfig)
                    .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                    .build();
        } else {
            sslCtx = null;
        }
        return b.childHandler(new Http2ServerInitializer(sslCtx, sleepTime, h2ContentAggregate));
    }

    private SslContextBuilder createSslContextBuilder() throws CertificateException {
        final SslContextBuilder sslContextBuilder;
        if (keyStoreName != null) {
            KeyManagerFactory keyManagerFactory = getKeyManagerFactory(keyStoreName);
            sslContextBuilder = SslContextBuilder.forServer(keyManagerFactory);
        } else {
            logger.info("Creating SSL context using self signed certificate");
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslContextBuilder = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey());
        }
        return sslContextBuilder.sslProvider(SslProvider.JDK);
    }

    private KeyManagerFactory getKeyManagerFactory(String keyStoreName) {
        KeyManagerFactory kmf;
        try {
            KeyStore ks = getKeyStore(keyStoreName, keyStorePassword);
            kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            if (ks != null) {
                kmf.init(ks, keyStorePassword.toCharArray());
            }
            return kmf;
        } catch (UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
            throw new IllegalArgumentException("Failed to initialize the Key Manager factory", e);
        }
    }

    private KeyStore getKeyStore(String keyStoreName, String keyStorePassword) throws IOException {
        KeyStore keyStore = null;
        String tlsStoreType = "PKCS12";

        if (keyStoreName != null && keyStorePassword != null) {
            try (InputStream is =  Thread.currentThread().getContextClassLoader()
            .getResourceAsStream(keyStoreName)) {
                keyStore = KeyStore.getInstance(tlsStoreType);
                keyStore.load(is, keyStorePassword.toCharArray());
            } catch (CertificateException | NoSuchAlgorithmException | KeyStoreException e) {
                throw new IOException(e);
            }
        }
        return keyStore;
    }

}