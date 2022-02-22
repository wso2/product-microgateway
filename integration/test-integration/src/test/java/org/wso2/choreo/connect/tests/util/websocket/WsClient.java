/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

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
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.util.Utils;

import javax.net.ssl.SSLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Map;

public final class WsClient {
    private static final Logger log = LoggerFactory.getLogger(WsClient.class);

    private final String url;
    private final Map<String, String> headers;

    public WsClient(String url, Map<String, String> headers) {
        this.url = url;
        this.headers = headers;
    }

    public ArrayList<String> connectAndSendMessages(String[] messages) throws CCTestException {
        log.info("Starting websocket client");
        EventLoopGroup group = new NioEventLoopGroup();
        ArrayList<String> receivedMessages = new ArrayList<>();
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme() == null? "ws" : uri.getScheme();

            if (!"ws".equalsIgnoreCase(scheme) && !"wss".equalsIgnoreCase(scheme)) {
                throw new CCTestException("Only ws and wss schemes are supported.");
            }
            if (uri.getPort() == -1) {
                log.error("Invalid port provided. URI: {}", url);
                throw new CCTestException("Invalid port provided");
            }
            HttpHeaders httpHeaders = new DefaultHttpHeaders();
            for (Map.Entry<String, String> header: headers.entrySet()) {
                httpHeaders.add(header.getKey(), header.getValue());
            }
            WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker(
                    uri, WebSocketVersion.V13, null, true, httpHeaders,
                    65536, true, false, 10000L);
            final WsClientHandler handler = new WsClientHandler(handshaker, receivedMessages);

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
                                    new HttpClientCodec(),
                                    new HttpObjectAggregator(8192),
                                    WebSocketClientCompressionHandler.INSTANCE,
                                    handler);
                        }
                    });

            log.info("Websocket client initiating connection");
            Channel ch = b.connect(uri.getHost(), uri.getPort()).sync().channel();
            handler.handshakeFuture().sync();

            log.info("Websocket client handshake complete");

            sendMessages(ch, messages);
        } catch (URISyntaxException e) {
            log.error("Error while parsing websocket URI", e);
        } catch (InterruptedException e) {
            log.error("Interrupted while syncing channel connect, close or handshake", e);
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

