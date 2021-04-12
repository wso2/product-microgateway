package org.wso2.choreo.connect.enforcer.grpc;

import io.grpc.stub.StreamObserver;
import org.wso2.choreo.connect.discovery.service.health.HealthCheckRequest;
import org.wso2.choreo.connect.discovery.service.health.HealthCheckResponse;
import org.wso2.choreo.connect.discovery.service.health.HealthGrpc;

/**
 * This is the gRPC server written to serve the health state of enforcer.
 */
public class HealthService extends HealthGrpc.HealthImplBase {
    @Override
    public void check(HealthCheckRequest request, StreamObserver<HealthCheckResponse> responseObserver) {
        HealthCheckResponse response = HealthCheckResponse.newBuilder()
                .setStatus(HealthCheckResponse.ServingStatus.SERVING).build();
        // respond for all without checking requested service name
        // service name format: package_names.ServiceName
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
