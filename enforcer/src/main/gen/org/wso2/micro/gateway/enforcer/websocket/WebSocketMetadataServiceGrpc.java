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
public final class WebSocketMetadataServiceGrpc {

  private WebSocketMetadataServiceGrpc() {}

  public static final String SERVICE_NAME = "envoy.extensions.filters.http.mgw_websocket.v3.WebSocketMetadataService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<org.wso2.micro.gateway.enforcer.websocket.RateLimitRequest,
      org.wso2.micro.gateway.enforcer.websocket.RateLimitResponse> getPublishMetadataMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "publishMetadata",
      requestType = org.wso2.micro.gateway.enforcer.websocket.RateLimitRequest.class,
      responseType = org.wso2.micro.gateway.enforcer.websocket.RateLimitResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
  public static io.grpc.MethodDescriptor<org.wso2.micro.gateway.enforcer.websocket.RateLimitRequest,
      org.wso2.micro.gateway.enforcer.websocket.RateLimitResponse> getPublishMetadataMethod() {
    io.grpc.MethodDescriptor<org.wso2.micro.gateway.enforcer.websocket.RateLimitRequest, org.wso2.micro.gateway.enforcer.websocket.RateLimitResponse> getPublishMetadataMethod;
    if ((getPublishMetadataMethod = WebSocketMetadataServiceGrpc.getPublishMetadataMethod) == null) {
      synchronized (WebSocketMetadataServiceGrpc.class) {
        if ((getPublishMetadataMethod = WebSocketMetadataServiceGrpc.getPublishMetadataMethod) == null) {
          WebSocketMetadataServiceGrpc.getPublishMetadataMethod = getPublishMetadataMethod =
              io.grpc.MethodDescriptor.<org.wso2.micro.gateway.enforcer.websocket.RateLimitRequest, org.wso2.micro.gateway.enforcer.websocket.RateLimitResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "publishMetadata"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.wso2.micro.gateway.enforcer.websocket.RateLimitRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.wso2.micro.gateway.enforcer.websocket.RateLimitResponse.getDefaultInstance()))
              .setSchemaDescriptor(new WebSocketMetadataServiceMethodDescriptorSupplier("publishMetadata"))
              .build();
        }
      }
    }
    return getPublishMetadataMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static WebSocketMetadataServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<WebSocketMetadataServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<WebSocketMetadataServiceStub>() {
        @java.lang.Override
        public WebSocketMetadataServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new WebSocketMetadataServiceStub(channel, callOptions);
        }
      };
    return WebSocketMetadataServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static WebSocketMetadataServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<WebSocketMetadataServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<WebSocketMetadataServiceBlockingStub>() {
        @java.lang.Override
        public WebSocketMetadataServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new WebSocketMetadataServiceBlockingStub(channel, callOptions);
        }
      };
    return WebSocketMetadataServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static WebSocketMetadataServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<WebSocketMetadataServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<WebSocketMetadataServiceFutureStub>() {
        @java.lang.Override
        public WebSocketMetadataServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new WebSocketMetadataServiceFutureStub(channel, callOptions);
        }
      };
    return WebSocketMetadataServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class WebSocketMetadataServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public io.grpc.stub.StreamObserver<org.wso2.micro.gateway.enforcer.websocket.RateLimitRequest> publishMetadata(
        io.grpc.stub.StreamObserver<org.wso2.micro.gateway.enforcer.websocket.RateLimitResponse> responseObserver) {
      return asyncUnimplementedStreamingCall(getPublishMetadataMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getPublishMetadataMethod(),
            asyncBidiStreamingCall(
              new MethodHandlers<
                org.wso2.micro.gateway.enforcer.websocket.RateLimitRequest,
                org.wso2.micro.gateway.enforcer.websocket.RateLimitResponse>(
                  this, METHODID_PUBLISH_METADATA)))
          .build();
    }
  }

  /**
   */
  public static final class WebSocketMetadataServiceStub extends io.grpc.stub.AbstractAsyncStub<WebSocketMetadataServiceStub> {
    private WebSocketMetadataServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected WebSocketMetadataServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new WebSocketMetadataServiceStub(channel, callOptions);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<org.wso2.micro.gateway.enforcer.websocket.RateLimitRequest> publishMetadata(
        io.grpc.stub.StreamObserver<org.wso2.micro.gateway.enforcer.websocket.RateLimitResponse> responseObserver) {
      return asyncBidiStreamingCall(
          getChannel().newCall(getPublishMetadataMethod(), getCallOptions()), responseObserver);
    }
  }

  /**
   */
  public static final class WebSocketMetadataServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<WebSocketMetadataServiceBlockingStub> {
    private WebSocketMetadataServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected WebSocketMetadataServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new WebSocketMetadataServiceBlockingStub(channel, callOptions);
    }
  }

  /**
   */
  public static final class WebSocketMetadataServiceFutureStub extends io.grpc.stub.AbstractFutureStub<WebSocketMetadataServiceFutureStub> {
    private WebSocketMetadataServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected WebSocketMetadataServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new WebSocketMetadataServiceFutureStub(channel, callOptions);
    }
  }

  private static final int METHODID_PUBLISH_METADATA = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final WebSocketMetadataServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(WebSocketMetadataServiceImplBase serviceImpl, int methodId) {
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
        case METHODID_PUBLISH_METADATA:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.publishMetadata(
              (io.grpc.stub.StreamObserver<org.wso2.micro.gateway.enforcer.websocket.RateLimitResponse>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class WebSocketMetadataServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    WebSocketMetadataServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return org.wso2.micro.gateway.enforcer.websocket.MgwWebSocketProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("WebSocketMetadataService");
    }
  }

  private static final class WebSocketMetadataServiceFileDescriptorSupplier
      extends WebSocketMetadataServiceBaseDescriptorSupplier {
    WebSocketMetadataServiceFileDescriptorSupplier() {}
  }

  private static final class WebSocketMetadataServiceMethodDescriptorSupplier
      extends WebSocketMetadataServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    WebSocketMetadataServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (WebSocketMetadataServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new WebSocketMetadataServiceFileDescriptorSupplier())
              .addMethod(getPublishMetadataMethod())
              .build();
        }
      }
    }
    return result;
  }
}
