package org.wso2.choreo.connect.enforcer.mcp.request;

import io.grpc.netty.shaded.io.netty.buffer.Unpooled;
import io.grpc.netty.shaded.io.netty.channel.ChannelFuture;
import io.grpc.netty.shaded.io.netty.channel.ChannelFutureListener;
import io.grpc.netty.shaded.io.netty.channel.ChannelHandlerContext;
import io.grpc.netty.shaded.io.netty.channel.SimpleChannelInboundHandler;
import io.grpc.netty.shaded.io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.grpc.netty.shaded.io.netty.handler.codec.http.FullHttpRequest;
import io.grpc.netty.shaded.io.netty.handler.codec.http.FullHttpResponse;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpContent;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpHeaderNames;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpHeaderValues;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpHeaders;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpMethod;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpObject;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpRequest;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.enforcer.api.APIFactory;
import org.wso2.choreo.connect.enforcer.mcp.McpConstants;
import org.wso2.choreo.connect.enforcer.mcp.response.PayloadGenerator;

import java.nio.charset.StandardCharsets;

/**
 * MCP Request Handler for MCP Proxies
 */
public class McpRequestHandler extends SimpleChannelInboundHandler<HttpObject> {
    private static final Logger logger = LogManager.getLogger(McpRequestHandler.class);
    private static final String mcp = "/mcp";
    private static final String wellKnown = "/.well-known/authorization-server";

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        if (msg instanceof HttpRequest) {
            FullHttpRequest req = (FullHttpRequest) msg;
            String path = req.uri().split("\\?")[0];
            switch (path) {
                case mcp:
                    if (req.method() == HttpMethod.POST) {
                        logger.info("Received request for /mcp");
                        handleMcpRequest(ctx, msg);
                        ctx.fireChannelRead(msg);
                        return;
                    }
                    break;
                case wellKnown:
                    if (req.method() == HttpMethod.GET) {
                        logger.info("Received request for /well-known");
                        handleWellKnownRequest(ctx, msg);
                        ctx.fireChannelRead(msg);
                        return;
                    }
                    break;
                default:
                    ctx.fireChannelRead(msg);
                    return;
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    private void handleMcpRequest(ChannelHandlerContext ctx, HttpObject msg) {
        FullHttpRequest req = (FullHttpRequest) msg;
        HttpHeaders headers = req.headers();
        String vhostHeader = headers.get(McpConstants.VHOST_HEADER);
        if (vhostHeader == null) {
            logger.error("Missing required header: " + McpConstants.VHOST_HEADER);
            ctx.fireChannelRead(msg);
            return;
        }
        String basepathHeader = headers.get(McpConstants.BASEPATH_HEADER);
        if (basepathHeader == null) {
            logger.error("Missing required header: " + McpConstants.BASEPATH_HEADER);
            ctx.fireChannelRead(msg);
            return;
        }
        String versionHeader = headers.get(McpConstants.VERSION_HEADER);
        if (versionHeader == null) {
            logger.error("Missing required header: " + McpConstants.VERSION_HEADER);
            ctx.fireChannelRead(msg);
            return;
        }
        StringBuilder tokenHeader = new StringBuilder();
        if (headers.get(HttpHeaderNames.AUTHORIZATION) != null) {
            String auth = headers.get(HttpHeaderNames.AUTHORIZATION);
            tokenHeader.append(HttpHeaderNames.AUTHORIZATION).append(":").append(auth);
        } else if (headers.get("test-key") != null) {
            tokenHeader.append("test-key").append(":").append(headers.get("test-key"));
        } else {
            logger.info("Authorization header is not available. Assuming no authorization needed for this request");
        }
        String apikey = APIFactory.getInstance().getApiKey(vhostHeader, basepathHeader, versionHeader);
        HttpContent requestContent = (HttpContent) msg;
        FullHttpResponse res;
        ChannelFuture cf;
        if (requestContent.content().isReadable()) {
            String body = requestContent.content().toString(StandardCharsets.UTF_8);
            String jsonResponse = McpRequestProcessor.processRequest(apikey, body, tokenHeader.toString());
            res = new DefaultFullHttpResponse(req.protocolVersion(), HttpResponseStatus.OK,
                    Unpooled.wrappedBuffer(jsonResponse.getBytes(StandardCharsets.UTF_8)));
            res.headers()
                    .set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                    .setInt(HttpHeaderNames.CONTENT_LENGTH, res.content().readableBytes());
            cf = ctx.writeAndFlush(res);
        } else {
            logger.info("Received empty request body");
            String jsonResponse = PayloadGenerator
                    .getErrorResponse(McpConstants.RpcConstants.INVALID_REQUEST_CODE,
                            McpConstants.RpcConstants.INVALID_REQUEST_MESSAGE, "Request body not found");
            res = new DefaultFullHttpResponse(req.protocolVersion(), HttpResponseStatus.OK,
                    Unpooled.wrappedBuffer(jsonResponse.getBytes(StandardCharsets.UTF_8)));
            res.headers()
                    .set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                    .setInt(HttpHeaderNames.CONTENT_LENGTH, res.content().readableBytes());
            cf = ctx.writeAndFlush(res);

        }
        cf.addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
    }

    private void handleWellKnownRequest(ChannelHandlerContext ctx, HttpObject msg) {
        //todo
    }
}
