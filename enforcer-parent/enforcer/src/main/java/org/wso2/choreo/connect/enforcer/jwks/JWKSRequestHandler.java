package org.wso2.choreo.connect.enforcer.jwks;


import com.nimbusds.jose.jwk.JWKSet;
import io.grpc.netty.shaded.io.netty.buffer.Unpooled;
import io.grpc.netty.shaded.io.netty.channel.ChannelFuture;
import io.grpc.netty.shaded.io.netty.channel.ChannelFutureListener;
import io.grpc.netty.shaded.io.netty.channel.ChannelHandlerContext;
import io.grpc.netty.shaded.io.netty.channel.SimpleChannelInboundHandler;
import io.grpc.netty.shaded.io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.grpc.netty.shaded.io.netty.handler.codec.http.FullHttpRequest;
import io.grpc.netty.shaded.io.netty.handler.codec.http.FullHttpResponse;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpMethod;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpObject;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpRequest;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpResponseStatus;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpVersion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;


/**
 * JWKS Request Handler for Backend JWT's
 */
public class JWKSRequestHandler extends SimpleChannelInboundHandler<HttpObject> {
    private static final String CONTENT_LENGTH = "content-length";
    private static final String CONNECTION = "Connection";
    private static final String CLOSE = "close";
    private static final String APPLICATION_JSON = "application/json";
    private static final String CONTENT_TYPE = "Content-Type";

    private static final Logger logger = LogManager.getLogger(JWKSRequestHandler.class);


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        FullHttpResponse res = null;
        FullHttpRequest req = null;
        BackendJWKSDto backendJWKSDto = ConfigHolder.getInstance().getConfig().getBackendJWKSDto();
        JWKSet jwks = backendJWKSDto.getJwks();
        if (msg instanceof HttpRequest) {

            req = (FullHttpRequest) msg;
            if (!(req.method() == HttpMethod.GET)) {
                return;
            }
        } else {
            logger.error("Error occurred while processing the request. Request isn't an instance of HTTP request");
            return;
        }
        res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                Unpooled.wrappedBuffer(jwks.toJSONObject().toString().getBytes()));
        res.headers()
                .set(CONNECTION, CLOSE)
                .set(CONTENT_TYPE, APPLICATION_JSON)
                .setInt(CONTENT_LENGTH, res.content().readableBytes());
        ChannelFuture f = ctx.write(res);
        f.addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void channelReadComplete(io.grpc.netty.shaded.io.netty.channel.ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error(cause);
        ctx.close();
    }


}
