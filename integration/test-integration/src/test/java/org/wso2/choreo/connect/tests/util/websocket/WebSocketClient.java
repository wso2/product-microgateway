package org.wso2.choreo.connect.tests.util.websocket;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.tests.context.CCTestException;

import javax.net.ssl.SSLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Map;

public final class WebSocketClient {
    private static final Logger log = LogManager.getLogger(WebSocketClient.class);

    private final String url;
    private final Map<String, String> headers;

    public WebSocketClient(String url, Map<String, String> headers) {
        this.url = url;
        this.headers = headers;
    }

    public ArrayList<String> connectAndSendMessages(String[] messages) throws CCTestException {
        log.info("Starting websocket client");
        EventLoopGroup group = new NioEventLoopGroup();
        ArrayList<String> receivedMessages = null;
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme() == null? "ws" : uri.getScheme();

            if (!"ws".equalsIgnoreCase(scheme) && !"wss".equalsIgnoreCase(scheme)) {
                throw new CCTestException("Only WS(S) is supported.");
            }
            final WebSocketClientHandler handler =
                    new WebSocketClientHandler(
                            WebSocketClientHandshakerFactory.newHandshaker(
                                    uri, WebSocketVersion.V13, null, true, new DefaultHttpHeaders()),
                            receivedMessages);

            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws SSLException {
                            ChannelPipeline p = ch.pipeline();
                            if ("wss".equalsIgnoreCase(scheme)) {
                                SslContext sslCtx = SslContextBuilder.forClient()
                                        .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
                                p.addLast(sslCtx.newHandler(ch.alloc(), uri.getHost(), uri.getPort()));
                            }
                            p.addLast(
//                                    new HttpClientCodec(),
//                                    new HttpObjectAggregator(8192),
//                                    WebSocketClientCompressionHandler.INSTANCE,
                                    handler);
                        }
                    });

            log.info("Websocket client initiating connection");
            Channel ch = b.connect(uri.getHost(), uri.getPort()).sync().channel();
            handler.handshakeFuture().sync();
            log.info("Websocket client handshake complete");

            sendMessages(ch, messages);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }
        return receivedMessages;
    }

    private void sendMessages(Channel ch, String[] messagesToSend) throws InterruptedException {
        for (String messageToSend: messagesToSend) {
            if ("close".equalsIgnoreCase(messageToSend)) {
                log.info("Sending close frame.");
                ch.writeAndFlush(new CloseWebSocketFrame());
                ch.closeFuture().sync();
                break;
            } else if ("ping".equalsIgnoreCase(messageToSend)) {
                log.info("Sending ping frame.");
                WebSocketFrame frame = new PingWebSocketFrame(Unpooled.wrappedBuffer(new byte[] { 8, 1, 8, 1 }));
                ch.writeAndFlush(frame);
            } else {
                log.info("Sending test frame.");
                WebSocketFrame frame = new TextWebSocketFrame(messageToSend);
                ch.writeAndFlush(frame);
            }
        }
    }
}

