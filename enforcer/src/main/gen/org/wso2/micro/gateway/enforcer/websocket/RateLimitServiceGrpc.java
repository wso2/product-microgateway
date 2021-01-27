package org.wso2.micro.gateway.enforcer.websocket;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.33.1)",
    comments = "Source: mgw_websocket_rls.proto")
public final class RateLimitServiceGrpc {

  private RateLimitServiceGrpc() {}

  public static final String SERVICE_NAME = "envoy.extensions.filters.http.mgw_websocket.v3.RateLimitService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<org.wso2.micro.gateway.enforcer.websocket.RateLimitRequest,
      org.wso2.micro.gateway.enforcer.websocket.RateLimitResponse> getShouldRateLimitStreamMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ShouldRateLimitStream",
      requestType = org.wso2.micro.gateway.enforcer.websocket.RateLimitRequest.class,
      responseType = org.wso2.micro.gateway.enforcer.websocket.RateLimitResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
  public static io.grpc.MethodDescriptor<org.wso2.micro.gateway.enforcer.websocket.RateLimitRequest,
      org.wso2.micro.gateway.enforcer.websocket.RateLimitResponse> getShouldRateLimitStreamMethod() {
    io.grpc.MethodDescriptor<org.wso2.micro.gateway.enforcer.websocket.RateLimitRequest, org.wso2.micro.gateway.enforcer.websocket.RateLimitResponse> getShouldRateLimitStreamMethod;
    if ((getShouldRateLimitStreamMethod = RateLimitServiceGrpc.getShouldRateLimitStreamMethod) == null) {
      synchronized (RateLimitServiceGrpc.class) {
        if ((getShouldRateLimitStreamMethod = RateLimitServiceGrpc.getShouldRateLimitStreamMethod) == null) {
          RateLimitServiceGrpc.getShouldRateLimitStreamMethod = getShouldRateLimitStreamMethod =
              io.grpc.MethodDescriptor.<org.wso2.micro.gateway.enforcer.websocket.RateLimitRequest, org.wso2.micro.gateway.enforcer.websocket.RateLimitResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ShouldRateLimitStream"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.wso2.micro.gateway.enforcer.websocket.RateLimitRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.wso2.micro.gateway.enforcer.websocket.RateLimitResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RateLimitServiceMethodDescriptorSupplier("ShouldRateLimitStream"))
              .build();
        }
      }
    }
    return getShouldRateLimitStreamMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static RateLimitServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RateLimitServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RateLimitServiceStub>() {
        @java.lang.Override
        public RateLimitServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RateLimitServiceStub(channel, callOptions);
        }
      };
    return RateLimitServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static RateLimitServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RateLimitServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RateLimitServiceBlockingStub>() {
        @java.lang.Override
        public RateLimitServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RateLimitServiceBlockingStub(channel, callOptions);
        }
      };
    return RateLimitServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static RateLimitServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RateLimitServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RateLimitServiceFutureStub>() {
        @java.lang.Override
        public RateLimitServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RateLimitServiceFutureStub(channel, callOptions);
        }
      };
    return RateLimitServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class RateLimitServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public io.grpc.stub.StreamObserver<org.wso2.micro.gateway.enforcer.websocket.RateLimitRequest> shouldRateLimitStream(
        io.grpc.stub.StreamObserver<org.wso2.micro.gateway.enforcer.websocket.RateLimitResponse> responseObserver) {
      return asyncUnimplementedStreamingCall(getShouldRateLimitStreamMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getShouldRateLimitStreamMethod(),
            asyncBidiStreamingCall(
              new MethodHandlers<
                org.wso2.micro.gateway.enforcer.websocket.RateLimitRequest,
                org.wso2.micro.gateway.enforcer.websocket.RateLimitResponse>(
                  this, METHODID_SHOULD_RATE_LIMIT_STREAM)))
          .build();
    }
  }

  /**
   */
  public static final class RateLimitServiceStub extends io.grpc.stub.AbstractAsyncStub<RateLimitServiceStub> {
    private RateLimitServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RateLimitServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RateLimitServiceStub(channel, callOptions);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<org.wso2.micro.gateway.enforcer.websocket.RateLimitRequest> shouldRateLimitStream(
        io.grpc.stub.StreamObserver<org.wso2.micro.gateway.enforcer.websocket.RateLimitResponse> responseObserver) {
      return asyncBidiStreamingCall(
          getChannel().newCall(getShouldRateLimitStreamMethod(), getCallOptions()), responseObserver);
    }
  }

  /**
   */
  public static final class RateLimitServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<RateLimitServiceBlockingStub> {
    private RateLimitServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RateLimitServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RateLimitServiceBlockingStub(channel, callOptions);
    }
  }

  /**
   */
  public static final class RateLimitServiceFutureStub extends io.grpc.stub.AbstractFutureStub<RateLimitServiceFutureStub> {
    private RateLimitServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RateLimitServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RateLimitServiceFutureStub(channel, callOptions);
    }
  }

  private static final int METHODID_SHOULD_RATE_LIMIT_STREAM = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final RateLimitServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(RateLimitServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_SHOULD_RATE_LIMIT_STREAM:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.shouldRateLimitStream(
              (io.grpc.stub.StreamObserver<org.wso2.micro.gateway.enforcer.websocket.RateLimitResponse>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class RateLimitServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    RateLimitServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return org.wso2.micro.gateway.enforcer.websocket.MgwWebSocketProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("RateLimitService");
    }
  }

  private static final class RateLimitServiceFileDescriptorSupplier
      extends RateLimitServiceBaseDescriptorSupplier {
    RateLimitServiceFileDescriptorSupplier() {}
  }

  private static final class RateLimitServiceMethodDescriptorSupplier
      extends RateLimitServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    RateLimitServiceMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (RateLimitServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new RateLimitServiceFileDescriptorSupplier())
              .addMethod(getShouldRateLimitStreamMethod())
              .build();
        }
      }
    }
    return result;
  }
}
