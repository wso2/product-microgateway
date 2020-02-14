package org.wso2.micro.gateway.tests.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class GrpcServer {
    private static final Logger log = LoggerFactory.getLogger(GrpcServer.class);
    private Server server;

    public void start() throws IOException {
        /* The port on which the server should run */
        int port = 50051;
        if (server == null || server.isShutdown() || server.isTerminated()) {
            server = ServerBuilder.forPort(port).addService(new TestServiceImpl()).build().start();
        }
        log.info("Server started, listening on " + port);
    }

    public void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }
}
