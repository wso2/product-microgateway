package org.wso2.choreo.connect.discovery.service.websocket;

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
 * <pre>
 * [#protodoc-title: WebSocketFrameService]
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler",
    comments = "Source: wso2/discovery/service/websocket/frame_service.proto")
public final class WebSocketFrameServiceGrpc {

  private WebSocketFrameServiceGrpc() {}

  public static final String SERVICE_NAME = "envoy.extensions.filters.http.mgw_wasm_websocket.v3.WebSocketFrameService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<org.wso2.choreo.connect.discovery.service.websocket.WebSocketFrameRequest,
      org.wso2.choreo.connect.discovery.service.websocket.WebSocketFrameResponse> getPublishFrameDataMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "PublishFrameData",
      requestType = org.wso2.choreo.connect.discovery.service.websocket.WebSocketFrameRequest.class,
      responseType = org.wso2.choreo.connect.discovery.service.websocket.WebSocketFrameResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
  public static io.grpc.MethodDescriptor<org.wso2.choreo.connect.discovery.service.websocket.WebSocketFrameRequest,
      org.wso2.choreo.connect.discovery.service.websocket.WebSocketFrameResponse> getPublishFrameDataMethod() {
    io.grpc.MethodDescriptor<org.wso2.choreo.connect.discovery.service.websocket.WebSocketFrameRequest, org.wso2.choreo.connect.discovery.service.websocket.WebSocketFrameResponse> getPublishFrameDataMethod;
    if ((getPublishFrameDataMethod = WebSocketFrameServiceGrpc.getPublishFrameDataMethod) == null) {
      synchronized (WebSocketFrameServiceGrpc.class) {
        if ((getPublishFrameDataMethod = WebSocketFrameServiceGrpc.getPublishFrameDataMethod) == null) {
          WebSocketFrameServiceGrpc.getPublishFrameDataMethod = getPublishFrameDataMethod =
              io.grpc.MethodDescriptor.<org.wso2.choreo.connect.discovery.service.websocket.WebSocketFrameRequest, org.wso2.choreo.connect.discovery.service.websocket.WebSocketFrameResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "PublishFrameData"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.wso2.choreo.connect.discovery.service.websocket.WebSocketFrameRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.wso2.choreo.connect.discovery.service.websocket.WebSocketFrameResponse.getDefaultInstance()))
              .setSchemaDescriptor(new WebSocketFrameServiceMethodDescriptorSupplier("PublishFrameData"))
              .build();
        }
      }
    }
    return getPublishFrameDataMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static WebSocketFrameServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<WebSocketFrameServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<WebSocketFrameServiceStub>() {
        @java.lang.Override
        public WebSocketFrameServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new WebSocketFrameServiceStub(channel, callOptions);
        }
      };
    return WebSocketFrameServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static WebSocketFrameServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<WebSocketFrameServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<WebSocketFrameServiceBlockingStub>() {
        @java.lang.Override
        public WebSocketFrameServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new WebSocketFrameServiceBlockingStub(channel, callOptions);
        }
      };
    return WebSocketFrameServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static WebSocketFrameServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<WebSocketFrameServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<WebSocketFrameServiceFutureStub>() {
        @java.lang.Override
        public WebSocketFrameServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new WebSocketFrameServiceFutureStub(channel, callOptions);
        }
      };
    return WebSocketFrameServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * [#protodoc-title: WebSocketFrameService]
   * </pre>
   */
  public static abstract class WebSocketFrameServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public io.grpc.stub.StreamObserver<org.wso2.choreo.connect.discovery.service.websocket.WebSocketFrameRequest> publishFrameData(
        io.grpc.stub.StreamObserver<org.wso2.choreo.connect.discovery.service.websocket.WebSocketFrameResponse> responseObserver) {
      return asyncUnimplementedStreamingCall(getPublishFrameDataMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getPublishFrameDataMethod(),
            asyncBidiStreamingCall(
              new MethodHandlers<
                org.wso2.choreo.connect.discovery.service.websocket.WebSocketFrameRequest,
                org.wso2.choreo.connect.discovery.service.websocket.WebSocketFrameResponse>(
                  this, METHODID_PUBLISH_FRAME_DATA)))
          .build();
    }
  }

  /**
   * <pre>
   * [#protodoc-title: WebSocketFrameService]
   * </pre>
   */
  public static final class WebSocketFrameServiceStub extends io.grpc.stub.AbstractAsyncStub<WebSocketFrameServiceStub> {
    private WebSocketFrameServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected WebSocketFrameServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new WebSocketFrameServiceStub(channel, callOptions);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<org.wso2.choreo.connect.discovery.service.websocket.WebSocketFrameRequest> publishFrameData(
        io.grpc.stub.StreamObserver<org.wso2.choreo.connect.discovery.service.websocket.WebSocketFrameResponse> responseObserver) {
      return asyncBidiStreamingCall(
          getChannel().newCall(getPublishFrameDataMethod(), getCallOptions()), responseObserver);
    }
  }

  /**
   * <pre>
   * [#protodoc-title: WebSocketFrameService]
   * </pre>
   */
  public static final class WebSocketFrameServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<WebSocketFrameServiceBlockingStub> {
    private WebSocketFrameServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected WebSocketFrameServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new WebSocketFrameServiceBlockingStub(channel, callOptions);
    }
  }

  /**
   * <pre>
   * [#protodoc-title: WebSocketFrameService]
   * </pre>
   */
  public static final class WebSocketFrameServiceFutureStub extends io.grpc.stub.AbstractFutureStub<WebSocketFrameServiceFutureStub> {
    private WebSocketFrameServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected WebSocketFrameServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new WebSocketFrameServiceFutureStub(channel, callOptions);
    }
  }

  private static final int METHODID_PUBLISH_FRAME_DATA = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final WebSocketFrameServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(WebSocketFrameServiceImplBase serviceImpl, int methodId) {
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
        case METHODID_PUBLISH_FRAME_DATA:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.publishFrameData(
              (io.grpc.stub.StreamObserver<org.wso2.choreo.connect.discovery.service.websocket.WebSocketFrameResponse>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class WebSocketFrameServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    WebSocketFrameServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return org.wso2.choreo.connect.discovery.service.websocket.MgwWebSocketProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("WebSocketFrameService");
    }
  }

  private static final class WebSocketFrameServiceFileDescriptorSupplier
      extends WebSocketFrameServiceBaseDescriptorSupplier {
    WebSocketFrameServiceFileDescriptorSupplier() {}
  }

  private static final class WebSocketFrameServiceMethodDescriptorSupplier
      extends WebSocketFrameServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    WebSocketFrameServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (WebSocketFrameServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new WebSocketFrameServiceFileDescriptorSupplier())
              .addMethod(getPublishFrameDataMethod())
              .build();
        }
      }
    }
    return result;
  }
}
