package org.wso2.choreo.connect.enforcer.jwks;



import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.X509CertUtils;
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
import org.wso2.carbon.apimgt.common.gateway.dto.JWTConfigurationDto;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;



import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;




/**
    JWKS Request Handler for Backend JWT's

 */
public class JwksRequestHandler extends SimpleChannelInboundHandler<HttpObject> {
    private static final String CONTENT_LENGTH = "content-length";
    private static final String CONNECTION = "Connection";
    private static final String CLOSE = "close";
    private static final String APPLICATION_JSON = "application/json";
    private static final String CONTENT_TYPE = "Content-Type";

    private static final Logger logger = LogManager.getLogger(JwksRequestHandler.class);

 
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {

        FullHttpResponse res = null;
        FullHttpRequest req = null;

        //Temp

        JWTConfigurationDto jwtConfigurationDto = ConfigHolder.getInstance()
                .getConfig()
                .getJwtConfigurationDto();

        Certificate publicCert = jwtConfigurationDto.getPublicCert();


        X509Certificate cert = X509CertUtils.parse(publicCert.getEncoded());
        RSAPublicKey publicKey = RSAKey.parse(cert).toRSAPublicKey();


        RSAKey jwk = new RSAKey.Builder(publicKey)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .keyIDFromThumbprint()
                .build().toPublicJWK();

         //TODO Make this a class of its own

        // ~
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
                Unpooled.wrappedBuffer(jwk.toJSONString().getBytes()));

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
