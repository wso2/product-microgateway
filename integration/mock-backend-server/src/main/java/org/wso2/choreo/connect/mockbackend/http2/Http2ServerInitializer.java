package org.wso2.choreo.connect.mockbackend.http2;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http.HttpServerUpgradeHandler.UpgradeCodecFactory;
import io.netty.handler.codec.http2.CleartextHttp2ServerUpgradeHandler;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2ServerUpgradeCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.util.AsciiString;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sets up the Netty pipeline for the example server. Depending on the endpoint config, sets up the
 * pipeline for NPN or cleartext HTTP upgrade to HTTP/2.
 */
public class Http2ServerInitializer extends ChannelInitializer<SocketChannel> {

    private static final Logger logger = LoggerFactory.getLogger(Http2ServerInitializer.class);

    private final SslContext sslCtx;
    private final int maxHttpContentLength;
    private final long sleepTime;
    private final boolean h2ContentAggregate;

    private static final UpgradeCodecFactory upgradeCodecFactory = protocol -> {
        if (AsciiString.contentEquals(Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME, protocol)) {
            return new Http2ServerUpgradeCodec(
                    Http2FrameCodecBuilder.forServer().build(), new EchoHttp2ServerHandler());
        } else {
            return null;
        }
    };

    Http2ServerInitializer(SslContext sslCtx, long sleepTime, boolean h2ContentAggregate) {
        this(sslCtx, sleepTime, h2ContentAggregate, 16 * 1024);
    }

    private Http2ServerInitializer(SslContext sslCtx, long sleepTime, boolean h2ContentAggregate,
                                   int maxHttpContentLength) {
        if (maxHttpContentLength < 0) {
            throw new IllegalArgumentException("maxHttpContentLength (expected >= 0): " + maxHttpContentLength);
        }
        this.sslCtx = sslCtx;
        this.maxHttpContentLength = maxHttpContentLength;
        this.sleepTime = sleepTime;
        this.h2ContentAggregate = h2ContentAggregate;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        if (sslCtx != null) {
            configureSsl(ch);
        } else {
            configureClearText(ch);
        }
    }

    /**
     * Configure the pipeline for TLS NPN negotiation to HTTP/2.
     */
    private void configureSsl(SocketChannel ch) {
        ch.pipeline().addLast(sslCtx.newHandler(ch.alloc()), new Http2OrHttpHandler(sleepTime, h2ContentAggregate));
    }

    /**
     * Configure the pipeline for a cleartext upgrade from HTTP to HTTP/2.0
     */
    private void configureClearText(SocketChannel ch) {
        final ChannelPipeline p = ch.pipeline();
        final HttpServerCodec sourceCodec = new HttpServerCodec();
        final HttpServerUpgradeHandler upgradeHandler = new HttpServerUpgradeHandler(sourceCodec,
                upgradeCodecFactory);
        final CleartextHttp2ServerUpgradeHandler cleartextHttp2ServerUpgradeHandler = new CleartextHttp2ServerUpgradeHandler(
                sourceCodec, upgradeHandler, new HelloWorldHttp2HandlerBuilder().build());
    
        p.addLast(cleartextHttp2ServerUpgradeHandler);
        p.addLast(new SimpleChannelInboundHandler<HttpMessage>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, HttpMessage msg) {
                // If this handler is hit then no upgrade has been attempted and the client is just talking HTTP.
                logger.debug("Directly talking: {} (no upgrade was attempted)", msg.protocolVersion());
                ChannelPipeline pipeline = ctx.pipeline();
                ChannelHandlerContext thisCtx = pipeline.context(this);
                pipeline.addAfter(thisCtx.name(), null, new EchoHttpServerHandler(sleepTime, false));
                pipeline.replace(this, null, new HttpObjectAggregator(maxHttpContentLength));
                ctx.fireChannelRead(ReferenceCountUtil.retain(msg));
            }
        });

        p.addLast(new UserEventLogger());
    }

    /**
     * Class that logs any User Events triggered on this channel.
     */
    private static class UserEventLogger extends ChannelInboundHandlerAdapter {
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            logger.debug("User Event Triggered: {}", evt);
            ctx.fireUserEventTriggered(evt);
        }
    }
    
}
