package org.wso2.choreo.connect.mockbackend.http2;

import io.netty.buffer.EmptyByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.DefaultHttp2WindowUpdateFrame;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2FrameStream;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersFrame;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

/**
 * Handler implementation for the http/2 echo server without content aggregation. This echo backs the header/data
 * frames as soon as they arrive without any content aggregation against stream id.
 */
public class EchoHttp2ServerHandler extends ChannelDuplexHandler {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Http2HeadersFrame) {
            onHeadersRead(ctx, (Http2HeadersFrame) msg);
        } else if (msg instanceof Http2DataFrame) {
            onDataRead(ctx, (Http2DataFrame) msg);
        } else {
            super.channelRead(ctx, msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    private static void onDataRead(ChannelHandlerContext ctx, Http2DataFrame data) {
        Http2FrameStream stream = data.stream();
        ctx.write(new DefaultHttp2DataFrame(data.content(), data.isEndStream()).stream(stream));
        // Update the flow-controller
        ctx.write(new DefaultHttp2WindowUpdateFrame(data.initialFlowControlledBytes()).stream(stream));
    }

    private static void onHeadersRead(ChannelHandlerContext ctx, Http2HeadersFrame headersFrame) {
        Http2FrameStream stream = headersFrame.stream();
        Http2Headers headers = new DefaultHttp2Headers().status(OK.codeAsText());
        ctx.write(new DefaultHttp2HeadersFrame(headers).stream(stream));
        if (headersFrame.isEndStream()) {
            ctx.write(new DefaultHttp2DataFrame(new EmptyByteBuf(ctx.alloc()), true).stream(stream));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        ctx.close();
    }
}
