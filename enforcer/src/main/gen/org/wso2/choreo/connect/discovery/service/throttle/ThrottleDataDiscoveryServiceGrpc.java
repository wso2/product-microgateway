package org.wso2.choreo.connect.discovery.service.throttle;

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
 * [#protodoc-title: TDDS]
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler",
    comments = "Source: wso2/discovery/service/throttle/tdds.proto")
public final class ThrottleDataDiscoveryServiceGrpc {

  private ThrottleDataDiscoveryServiceGrpc() {}

  public static final String SERVICE_NAME = "discovery.service.throttle.ThrottleDataDiscoveryService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest,
      io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse> getStreamThrottleDataMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "StreamThrottleData",
      requestType = io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest.class,
      responseType = io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
  public static io.grpc.MethodDescriptor<io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest,
      io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse> getStreamThrottleDataMethod() {
    io.grpc.MethodDescriptor<io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest, io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse> getStreamThrottleDataMethod;
    if ((getStreamThrottleDataMethod = ThrottleDataDiscoveryServiceGrpc.getStreamThrottleDataMethod) == null) {
      synchronized (ThrottleDataDiscoveryServiceGrpc.class) {
        if ((getStreamThrottleDataMethod = ThrottleDataDiscoveryServiceGrpc.getStreamThrottleDataMethod) == null) {
          ThrottleDataDiscoveryServiceGrpc.getStreamThrottleDataMethod = getStreamThrottleDataMethod =
              io.grpc.MethodDescriptor.<io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest, io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "StreamThrottleData"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ThrottleDataDiscoveryServiceMethodDescriptorSupplier("StreamThrottleData"))
              .build();
        }
      }
    }
    return getStreamThrottleDataMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest,
      io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse> getFetchThrottleDataMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "FetchThrottleData",
      requestType = io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest.class,
      responseType = io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest,
      io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse> getFetchThrottleDataMethod() {
    io.grpc.MethodDescriptor<io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest, io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse> getFetchThrottleDataMethod;
    if ((getFetchThrottleDataMethod = ThrottleDataDiscoveryServiceGrpc.getFetchThrottleDataMethod) == null) {
      synchronized (ThrottleDataDiscoveryServiceGrpc.class) {
        if ((getFetchThrottleDataMethod = ThrottleDataDiscoveryServiceGrpc.getFetchThrottleDataMethod) == null) {
          ThrottleDataDiscoveryServiceGrpc.getFetchThrottleDataMethod = getFetchThrottleDataMethod =
              io.grpc.MethodDescriptor.<io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest, io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "FetchThrottleData"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ThrottleDataDiscoveryServiceMethodDescriptorSupplier("FetchThrottleData"))
              .build();
        }
      }
    }
    return getFetchThrottleDataMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ThrottleDataDiscoveryServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ThrottleDataDiscoveryServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ThrottleDataDiscoveryServiceStub>() {
        @java.lang.Override
        public ThrottleDataDiscoveryServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ThrottleDataDiscoveryServiceStub(channel, callOptions);
        }
      };
    return ThrottleDataDiscoveryServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ThrottleDataDiscoveryServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ThrottleDataDiscoveryServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ThrottleDataDiscoveryServiceBlockingStub>() {
        @java.lang.Override
        public ThrottleDataDiscoveryServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ThrottleDataDiscoveryServiceBlockingStub(channel, callOptions);
        }
      };
    return ThrottleDataDiscoveryServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static ThrottleDataDiscoveryServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ThrottleDataDiscoveryServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ThrottleDataDiscoveryServiceFutureStub>() {
        @java.lang.Override
        public ThrottleDataDiscoveryServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ThrottleDataDiscoveryServiceFutureStub(channel, callOptions);
        }
      };
    return ThrottleDataDiscoveryServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * [#protodoc-title: TDDS]
   * </pre>
   */
  public static abstract class ThrottleDataDiscoveryServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public io.grpc.stub.StreamObserver<io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest> streamThrottleData(
        io.grpc.stub.StreamObserver<io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse> responseObserver) {
      return asyncUnimplementedStreamingCall(getStreamThrottleDataMethod(), responseObserver);
    }

    /**
     */
    public void fetchThrottleData(io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest request,
        io.grpc.stub.StreamObserver<io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getFetchThrottleDataMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getStreamThrottleDataMethod(),
            asyncBidiStreamingCall(
              new MethodHandlers<
                io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest,
                io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse>(
                  this, METHODID_STREAM_THROTTLE_DATA)))
          .addMethod(
            getFetchThrottleDataMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest,
                io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse>(
                  this, METHODID_FETCH_THROTTLE_DATA)))
          .build();
    }
  }

  /**
   * <pre>
   * [#protodoc-title: TDDS]
   * </pre>
   */
  public static final class ThrottleDataDiscoveryServiceStub extends io.grpc.stub.AbstractAsyncStub<ThrottleDataDiscoveryServiceStub> {
    private ThrottleDataDiscoveryServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ThrottleDataDiscoveryServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ThrottleDataDiscoveryServiceStub(channel, callOptions);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest> streamThrottleData(
        io.grpc.stub.StreamObserver<io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse> responseObserver) {
      return asyncBidiStreamingCall(
          getChannel().newCall(getStreamThrottleDataMethod(), getCallOptions()), responseObserver);
    }

    /**
     */
    public void fetchThrottleData(io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest request,
        io.grpc.stub.StreamObserver<io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getFetchThrottleDataMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * [#protodoc-title: TDDS]
   * </pre>
   */
  public static final class ThrottleDataDiscoveryServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<ThrottleDataDiscoveryServiceBlockingStub> {
    private ThrottleDataDiscoveryServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ThrottleDataDiscoveryServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ThrottleDataDiscoveryServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse fetchThrottleData(io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest request) {
      return blockingUnaryCall(
          getChannel(), getFetchThrottleDataMethod(), getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * [#protodoc-title: TDDS]
   * </pre>
   */
  public static final class ThrottleDataDiscoveryServiceFutureStub extends io.grpc.stub.AbstractFutureStub<ThrottleDataDiscoveryServiceFutureStub> {
    private ThrottleDataDiscoveryServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ThrottleDataDiscoveryServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ThrottleDataDiscoveryServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse> fetchThrottleData(
        io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getFetchThrottleDataMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_FETCH_THROTTLE_DATA = 0;
  private static final int METHODID_STREAM_THROTTLE_DATA = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final ThrottleDataDiscoveryServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(ThrottleDataDiscoveryServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_FETCH_THROTTLE_DATA:
          serviceImpl.fetchThrottleData((io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest) request,
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
        case METHODID_STREAM_THROTTLE_DATA:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.streamThrottleData(
              (io.grpc.stub.StreamObserver<io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class ThrottleDataDiscoveryServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    ThrottleDataDiscoveryServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return org.wso2.choreo.connect.discovery.service.throttle.TDdsProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("ThrottleDataDiscoveryService");
    }
  }

  private static final class ThrottleDataDiscoveryServiceFileDescriptorSupplier
      extends ThrottleDataDiscoveryServiceBaseDescriptorSupplier {
    ThrottleDataDiscoveryServiceFileDescriptorSupplier() {}
  }

  private static final class ThrottleDataDiscoveryServiceMethodDescriptorSupplier
      extends ThrottleDataDiscoveryServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    ThrottleDataDiscoveryServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (ThrottleDataDiscoveryServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new ThrottleDataDiscoveryServiceFileDescriptorSupplier())
              .addMethod(getStreamThrottleDataMethod())
              .addMethod(getFetchThrottleDataMethod())
              .build();
        }
      }
    }
    return result;
  }
}
