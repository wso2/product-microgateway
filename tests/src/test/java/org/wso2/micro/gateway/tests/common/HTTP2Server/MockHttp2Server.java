package org.wso2.micro.gateway.tests.common.HTTP2Server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.*;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;

public final class MockHttp2Server extends Thread {
    private static final Log log = LogFactory.getLog(MockHttp2Server.class);
    static boolean SSL;
    //static final boolean SSL = true;

    static int PORT = Integer.parseInt(System.getProperty("port", SSL ? "8443" : "8080"));

    public MockHttp2Server(int port, boolean ssl) {
        PORT = port;
        SSL = ssl;
    }

    public static void main(String[] args) throws Exception {

        MockHttp2Server mockHttp2Server = new MockHttp2Server(PORT, SSL = false);
        mockHttp2Server.start();
    }


    public void run() {

        // Configure SSL
        SslContext sslCtx = null;
        log.info("SSL: " + SSL);
        log.info("PORT: " + PORT);

        if (SSL) {

            log.info("configuring ssl");
            SslProvider provider = OpenSsl.isAlpnSupported() ? SslProvider.OPENSSL : SslProvider.JDK;
            SelfSignedCertificate ssc = null;

            try {
                ssc = new SelfSignedCertificate();
            } catch (CertificateException e) {
                log.error("A CertificateException occurred " + e);
            }
            try {
                sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
                        .sslProvider(provider)
                        /* NOTE: the cipher filter may not include all ciphers required by the HTTP/2 specification.
                         * Please refer to the HTTP/2 specification for cipher requirements. */
                        .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                        .applicationProtocolConfig(new ApplicationProtocolConfig(
                                ApplicationProtocolConfig.Protocol.ALPN,
                                // NO_ADVERTISE is currently the only mode supported by both OpenSsl and JDK providers.
                                ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                                // ACCEPT is currently the only mode supported by both OpenSsl and JDK providers.
                                ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                                ApplicationProtocolNames.HTTP_2,
                                ApplicationProtocolNames.HTTP_1_1))
                        .build();
            } catch (SSLException e) {
                log.error("An SSLException occurred " + e);
            }
        } else {
            sslCtx = null;
        }

        // Configure the server.
        EventLoopGroup group = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 1024);
            b.group(group)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new Http2ServerInitializer(sslCtx));

            Channel ch = b.bind(PORT).sync().channel();

            log.info("Open your HTTP/2-enabled web browser and navigate to " +
                    (SSL ? "https" : "http") + "://127.0.0.1:" + PORT + '/');

            ch.closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("An InterruptedException occurred" + e);
        } finally {
            group.shutdownGracefully();
        }
    }
}