package org.wso2.choreo.connect.discovery.service.subscription;

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
 * [#protodoc-title: SDS]
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler",
    comments = "Source: wso2/discovery/service/subscription/sds.proto")
public final class SubscriptionDiscoveryServiceGrpc {

  private SubscriptionDiscoveryServiceGrpc() {}

  public static final String SERVICE_NAME = "discovery.service.subscription.SubscriptionDiscoveryService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest,
      io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse> getStreamSubscriptionsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "StreamSubscriptions",
      requestType = io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest.class,
      responseType = io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
  public static io.grpc.MethodDescriptor<io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest,
      io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse> getStreamSubscriptionsMethod() {
    io.grpc.MethodDescriptor<io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest, io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse> getStreamSubscriptionsMethod;
    if ((getStreamSubscriptionsMethod = SubscriptionDiscoveryServiceGrpc.getStreamSubscriptionsMethod) == null) {
      synchronized (SubscriptionDiscoveryServiceGrpc.class) {
        if ((getStreamSubscriptionsMethod = SubscriptionDiscoveryServiceGrpc.getStreamSubscriptionsMethod) == null) {
          SubscriptionDiscoveryServiceGrpc.getStreamSubscriptionsMethod = getStreamSubscriptionsMethod =
              io.grpc.MethodDescriptor.<io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest, io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "StreamSubscriptions"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SubscriptionDiscoveryServiceMethodDescriptorSupplier("StreamSubscriptions"))
              .build();
        }
      }
    }
    return getStreamSubscriptionsMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static SubscriptionDiscoveryServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SubscriptionDiscoveryServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SubscriptionDiscoveryServiceStub>() {
        @java.lang.Override
        public SubscriptionDiscoveryServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SubscriptionDiscoveryServiceStub(channel, callOptions);
        }
      };
    return SubscriptionDiscoveryServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static SubscriptionDiscoveryServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SubscriptionDiscoveryServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SubscriptionDiscoveryServiceBlockingStub>() {
        @java.lang.Override
        public SubscriptionDiscoveryServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SubscriptionDiscoveryServiceBlockingStub(channel, callOptions);
        }
      };
    return SubscriptionDiscoveryServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static SubscriptionDiscoveryServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SubscriptionDiscoveryServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SubscriptionDiscoveryServiceFutureStub>() {
        @java.lang.Override
        public SubscriptionDiscoveryServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SubscriptionDiscoveryServiceFutureStub(channel, callOptions);
        }
      };
    return SubscriptionDiscoveryServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * [#protodoc-title: SDS]
   * </pre>
   */
  public static abstract class SubscriptionDiscoveryServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public io.grpc.stub.StreamObserver<io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest> streamSubscriptions(
        io.grpc.stub.StreamObserver<io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse> responseObserver) {
      return asyncUnimplementedStreamingCall(getStreamSubscriptionsMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getStreamSubscriptionsMethod(),
            asyncBidiStreamingCall(
              new MethodHandlers<
                io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest,
                io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse>(
                  this, METHODID_STREAM_SUBSCRIPTIONS)))
          .build();
    }
  }

  /**
   * <pre>
   * [#protodoc-title: SDS]
   * </pre>
   */
  public static final class SubscriptionDiscoveryServiceStub extends io.grpc.stub.AbstractAsyncStub<SubscriptionDiscoveryServiceStub> {
    private SubscriptionDiscoveryServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SubscriptionDiscoveryServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SubscriptionDiscoveryServiceStub(channel, callOptions);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest> streamSubscriptions(
        io.grpc.stub.StreamObserver<io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse> responseObserver) {
      return asyncBidiStreamingCall(
          getChannel().newCall(getStreamSubscriptionsMethod(), getCallOptions()), responseObserver);
    }
  }

  /**
   * <pre>
   * [#protodoc-title: SDS]
   * </pre>
   */
  public static final class SubscriptionDiscoveryServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<SubscriptionDiscoveryServiceBlockingStub> {
    private SubscriptionDiscoveryServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SubscriptionDiscoveryServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SubscriptionDiscoveryServiceBlockingStub(channel, callOptions);
    }
  }

  /**
   * <pre>
   * [#protodoc-title: SDS]
   * </pre>
   */
  public static final class SubscriptionDiscoveryServiceFutureStub extends io.grpc.stub.AbstractFutureStub<SubscriptionDiscoveryServiceFutureStub> {
    private SubscriptionDiscoveryServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SubscriptionDiscoveryServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SubscriptionDiscoveryServiceFutureStub(channel, callOptions);
    }
  }

  private static final int METHODID_STREAM_SUBSCRIPTIONS = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final SubscriptionDiscoveryServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(SubscriptionDiscoveryServiceImplBase serviceImpl, int methodId) {
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
        case METHODID_STREAM_SUBSCRIPTIONS:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.streamSubscriptions(
              (io.grpc.stub.StreamObserver<io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class SubscriptionDiscoveryServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    SubscriptionDiscoveryServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return org.wso2.choreo.connect.discovery.service.subscription.SdsProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("SubscriptionDiscoveryService");
    }
  }

  private static final class SubscriptionDiscoveryServiceFileDescriptorSupplier
      extends SubscriptionDiscoveryServiceBaseDescriptorSupplier {
    SubscriptionDiscoveryServiceFileDescriptorSupplier() {}
  }

  private static final class SubscriptionDiscoveryServiceMethodDescriptorSupplier
      extends SubscriptionDiscoveryServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    SubscriptionDiscoveryServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (SubscriptionDiscoveryServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new SubscriptionDiscoveryServiceFileDescriptorSupplier())
              .addMethod(getStreamSubscriptionsMethod())
              .build();
        }
      }
    }
    return result;
  }
}
