package org.wso2.choreo.connect.discovery.service.keymgt;

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
 * [#protodoc-title: RTDS]
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler",
    comments = "Source: wso2/discovery/service/keymgt/rtds.proto")
public final class RevokedTokenDiscoveryServiceGrpc {

  private RevokedTokenDiscoveryServiceGrpc() {}

  public static final String SERVICE_NAME = "wso2.discovery.service.keymgt.RevokedTokenDiscoveryService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest,
      io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse> getStreamTokensMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "StreamTokens",
      requestType = io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest.class,
      responseType = io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
  public static io.grpc.MethodDescriptor<io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest,
      io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse> getStreamTokensMethod() {
    io.grpc.MethodDescriptor<io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest, io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse> getStreamTokensMethod;
    if ((getStreamTokensMethod = RevokedTokenDiscoveryServiceGrpc.getStreamTokensMethod) == null) {
      synchronized (RevokedTokenDiscoveryServiceGrpc.class) {
        if ((getStreamTokensMethod = RevokedTokenDiscoveryServiceGrpc.getStreamTokensMethod) == null) {
          RevokedTokenDiscoveryServiceGrpc.getStreamTokensMethod = getStreamTokensMethod =
              io.grpc.MethodDescriptor.<io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest, io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "StreamTokens"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RevokedTokenDiscoveryServiceMethodDescriptorSupplier("StreamTokens"))
              .build();
        }
      }
    }
    return getStreamTokensMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest,
      io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse> getFetchTokensMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "FetchTokens",
      requestType = io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest.class,
      responseType = io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest,
      io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse> getFetchTokensMethod() {
    io.grpc.MethodDescriptor<io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest, io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse> getFetchTokensMethod;
    if ((getFetchTokensMethod = RevokedTokenDiscoveryServiceGrpc.getFetchTokensMethod) == null) {
      synchronized (RevokedTokenDiscoveryServiceGrpc.class) {
        if ((getFetchTokensMethod = RevokedTokenDiscoveryServiceGrpc.getFetchTokensMethod) == null) {
          RevokedTokenDiscoveryServiceGrpc.getFetchTokensMethod = getFetchTokensMethod =
              io.grpc.MethodDescriptor.<io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest, io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "FetchTokens"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RevokedTokenDiscoveryServiceMethodDescriptorSupplier("FetchTokens"))
              .build();
        }
      }
    }
    return getFetchTokensMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static RevokedTokenDiscoveryServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RevokedTokenDiscoveryServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RevokedTokenDiscoveryServiceStub>() {
        @java.lang.Override
        public RevokedTokenDiscoveryServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RevokedTokenDiscoveryServiceStub(channel, callOptions);
        }
      };
    return RevokedTokenDiscoveryServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static RevokedTokenDiscoveryServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RevokedTokenDiscoveryServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RevokedTokenDiscoveryServiceBlockingStub>() {
        @java.lang.Override
        public RevokedTokenDiscoveryServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RevokedTokenDiscoveryServiceBlockingStub(channel, callOptions);
        }
      };
    return RevokedTokenDiscoveryServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static RevokedTokenDiscoveryServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RevokedTokenDiscoveryServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RevokedTokenDiscoveryServiceFutureStub>() {
        @java.lang.Override
        public RevokedTokenDiscoveryServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RevokedTokenDiscoveryServiceFutureStub(channel, callOptions);
        }
      };
    return RevokedTokenDiscoveryServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * [#protodoc-title: RTDS]
   * </pre>
   */
  public static abstract class RevokedTokenDiscoveryServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public io.grpc.stub.StreamObserver<io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest> streamTokens(
        io.grpc.stub.StreamObserver<io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse> responseObserver) {
      return asyncUnimplementedStreamingCall(getStreamTokensMethod(), responseObserver);
    }

    /**
     */
    public void fetchTokens(io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest request,
        io.grpc.stub.StreamObserver<io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getFetchTokensMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getStreamTokensMethod(),
            asyncBidiStreamingCall(
              new MethodHandlers<
                io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest,
                io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse>(
                  this, METHODID_STREAM_TOKENS)))
          .addMethod(
            getFetchTokensMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest,
                io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse>(
                  this, METHODID_FETCH_TOKENS)))
          .build();
    }
  }

  /**
   * <pre>
   * [#protodoc-title: RTDS]
   * </pre>
   */
  public static final class RevokedTokenDiscoveryServiceStub extends io.grpc.stub.AbstractAsyncStub<RevokedTokenDiscoveryServiceStub> {
    private RevokedTokenDiscoveryServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RevokedTokenDiscoveryServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RevokedTokenDiscoveryServiceStub(channel, callOptions);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest> streamTokens(
        io.grpc.stub.StreamObserver<io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse> responseObserver) {
      return asyncBidiStreamingCall(
          getChannel().newCall(getStreamTokensMethod(), getCallOptions()), responseObserver);
    }

    /**
     */
    public void fetchTokens(io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest request,
        io.grpc.stub.StreamObserver<io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getFetchTokensMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * [#protodoc-title: RTDS]
   * </pre>
   */
  public static final class RevokedTokenDiscoveryServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<RevokedTokenDiscoveryServiceBlockingStub> {
    private RevokedTokenDiscoveryServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RevokedTokenDiscoveryServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RevokedTokenDiscoveryServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse fetchTokens(io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest request) {
      return blockingUnaryCall(
          getChannel(), getFetchTokensMethod(), getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * [#protodoc-title: RTDS]
   * </pre>
   */
  public static final class RevokedTokenDiscoveryServiceFutureStub extends io.grpc.stub.AbstractFutureStub<RevokedTokenDiscoveryServiceFutureStub> {
    private RevokedTokenDiscoveryServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RevokedTokenDiscoveryServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RevokedTokenDiscoveryServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse> fetchTokens(
        io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getFetchTokensMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_FETCH_TOKENS = 0;
  private static final int METHODID_STREAM_TOKENS = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final RevokedTokenDiscoveryServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(RevokedTokenDiscoveryServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_FETCH_TOKENS:
          serviceImpl.fetchTokens((io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest) request,
              (io.grpc.stub.StreamObserver<io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_STREAM_TOKENS:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.streamTokens(
              (io.grpc.stub.StreamObserver<io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class RevokedTokenDiscoveryServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    RevokedTokenDiscoveryServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return org.wso2.choreo.connect.discovery.service.keymgt.RtdsProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("RevokedTokenDiscoveryService");
    }
  }

  private static final class RevokedTokenDiscoveryServiceFileDescriptorSupplier
      extends RevokedTokenDiscoveryServiceBaseDescriptorSupplier {
    RevokedTokenDiscoveryServiceFileDescriptorSupplier() {}
  }

  private static final class RevokedTokenDiscoveryServiceMethodDescriptorSupplier
      extends RevokedTokenDiscoveryServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    RevokedTokenDiscoveryServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (RevokedTokenDiscoveryServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new RevokedTokenDiscoveryServiceFileDescriptorSupplier())
              .addMethod(getStreamTokensMethod())
              .addMethod(getFetchTokensMethod())
              .build();
        }
      }
    }
    return result;
  }
}
