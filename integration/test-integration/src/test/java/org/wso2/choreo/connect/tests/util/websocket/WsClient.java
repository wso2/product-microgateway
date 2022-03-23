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
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakeException;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
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
import java.util.List;
import java.util.Map;

public final class WsClient {
    private static final Logger log = LoggerFactory.getLogger(WsClient.class);

    private static final int MAX_RETRY_COUNT = 10;
    private static final int RETRY_INTERVAL_MILLIS = 3000;

    private final String url;
    private final Map<String, String> headers;

    public WsClient(String url, Map<String, String> headers) {
        this.url = url;
        this.headers = headers;
    }

    public ArrayList<String> connectAndSendMessages(List<String> messages, int delayAfterSending)
            throws CCTestException {
        log.info("Starting websocket client");
        EventLoopGroup group = new NioEventLoopGroup(1);
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
                    65536, true, false, 15000L);
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
                                    // TODO: (suksw) Uncomment the following once this is fixed https://github.com/wso2/product-microgateway/issues/2693
                                    // WebSocketClientCompressionHandler.INSTANCE,
                                    handler);
                        }
                    });

            log.info("Websocket client initiating connection");
            Channel ch = b.connect(uri.getHost(), uri.getPort()).sync().channel();
            handler.handshakeFuture().sync();

            log.info("Websocket client handshake complete");

            sendMessages(ch, messages);
            Utils.delay(delayAfterSending, "interrupted while waiting for response frames");
        } catch (URISyntaxException e) {
            log.error("Error while parsing websocket URI", e);
        } catch (InterruptedException e) {
            log.error("Interrupted while syncing channel connect, close or handshake", e);
        } finally {
            group.shutdownGracefully().syncUninterruptibly();
        }
        return receivedMessages;
    }

    private void sendMessages(Channel ch, List<String> messagesToSend) throws InterruptedException {
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
                if (messageToSend.length() < 100) {
                    log.info("Sending text frame. Msg: {}", messageToSend);
                } else {
                    log.info("Sending text frame.");
                }
                WebSocketFrame frame = new TextWebSocketFrame(messageToSend);
                ch.writeAndFlush(frame);
            }
            // Remove the following once https://github.com/wso2/product-microgateway/issues/2706 is fixed
            Utils.delay(1000, "Interrupted while waiting after sending one frame");
        }
    }

    public ArrayList<String> retryConnectUntilDeployed(List<String> messages, int delayAfterSending) throws InterruptedException {
        int retryCount = 0;
        boolean respondedNotFound = false;
        ArrayList<String> responses = null;
        do {
            try {
                log.info("Trying websocket connect with url : " + url);
                responses = connectAndSendMessages(messages, delayAfterSending);
                respondedNotFound = false;
            } catch (WebSocketClientHandshakeException | CCTestException e) {
                if("Invalid handshake response getStatus: 404 Not Found".equals(e.getMessage()) ||
                "Invalid handshake response getStatus: 503 Service Unavailable".equals(e.getMessage())) {
                    retryCount++;
                    respondedNotFound = true;
                } else {
                    if (e.getMessage() != null) {
                        log.error("Error during websocket handshake." + e.getMessage());
                    } else {
                        log.error("Error during websocket handshake.");
                    }
                }
            }
        } while (respondedNotFound && shouldRetry(retryCount));
        return responses;
    }

    private static boolean shouldRetry(int retryCount) throws InterruptedException {
        if(retryCount >= MAX_RETRY_COUNT) {
            log.info("Retrying of the request is finished");
            return false;
        }
        Thread.sleep(RETRY_INTERVAL_MILLIS);
        return true;
    }

    public boolean isThrottledWebSocket(int expectedCount) throws InterruptedException {
        // Similar to HTTP throttling, this buffer is to avoid failures due to delays in evaluating throttle
        // conditions at TM here it sets the final throttle request count twice as the limit set in the policy.
        // it will make sure throttle will happen even if the throttle window passed.
        int throttleBuffer = expectedCount + 10;
        boolean isThrottled = false;
        List<String> messagesToSend = new ArrayList<>();
        messagesToSend.add("send me. small frames. " + throttleBuffer);
        List<String> responses = retryConnectUntilDeployed(messagesToSend, throttleBuffer * 1100);
        if (responses.size() >= expectedCount && responses.size() < throttleBuffer) {
            isThrottled = true;
        }
        for (String response: responses) {
            log.info("============== Response {}", response);
        }

        return isThrottled;
    }
}

