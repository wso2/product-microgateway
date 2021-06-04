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
 * [#protodoc-title: SubPolicyDS]
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler",
    comments = "Source: wso2/discovery/service/subscription/sub_policy_ds.proto")
public final class SubscriptionPolicyDiscoveryServiceGrpc {

  private SubscriptionPolicyDiscoveryServiceGrpc() {}

  public static final String SERVICE_NAME = "discovery.service.subscription.SubscriptionPolicyDiscoveryService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest,
      io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse> getStreamSubscriptionPoliciesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "StreamSubscriptionPolicies",
      requestType = io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest.class,
      responseType = io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
  public static io.grpc.MethodDescriptor<io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest,
      io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse> getStreamSubscriptionPoliciesMethod() {
    io.grpc.MethodDescriptor<io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest, io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse> getStreamSubscriptionPoliciesMethod;
    if ((getStreamSubscriptionPoliciesMethod = SubscriptionPolicyDiscoveryServiceGrpc.getStreamSubscriptionPoliciesMethod) == null) {
      synchronized (SubscriptionPolicyDiscoveryServiceGrpc.class) {
        if ((getStreamSubscriptionPoliciesMethod = SubscriptionPolicyDiscoveryServiceGrpc.getStreamSubscriptionPoliciesMethod) == null) {
          SubscriptionPolicyDiscoveryServiceGrpc.getStreamSubscriptionPoliciesMethod = getStreamSubscriptionPoliciesMethod =
              io.grpc.MethodDescriptor.<io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest, io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "StreamSubscriptionPolicies"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SubscriptionPolicyDiscoveryServiceMethodDescriptorSupplier("StreamSubscriptionPolicies"))
              .build();
        }
      }
    }
    return getStreamSubscriptionPoliciesMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static SubscriptionPolicyDiscoveryServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SubscriptionPolicyDiscoveryServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SubscriptionPolicyDiscoveryServiceStub>() {
        @java.lang.Override
        public SubscriptionPolicyDiscoveryServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SubscriptionPolicyDiscoveryServiceStub(channel, callOptions);
        }
      };
    return SubscriptionPolicyDiscoveryServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static SubscriptionPolicyDiscoveryServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SubscriptionPolicyDiscoveryServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SubscriptionPolicyDiscoveryServiceBlockingStub>() {
        @java.lang.Override
        public SubscriptionPolicyDiscoveryServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SubscriptionPolicyDiscoveryServiceBlockingStub(channel, callOptions);
        }
      };
    return SubscriptionPolicyDiscoveryServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static SubscriptionPolicyDiscoveryServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SubscriptionPolicyDiscoveryServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SubscriptionPolicyDiscoveryServiceFutureStub>() {
        @java.lang.Override
        public SubscriptionPolicyDiscoveryServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SubscriptionPolicyDiscoveryServiceFutureStub(channel, callOptions);
        }
      };
    return SubscriptionPolicyDiscoveryServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * [#protodoc-title: SubPolicyDS]
   * </pre>
   */
  public static abstract class SubscriptionPolicyDiscoveryServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public io.grpc.stub.StreamObserver<io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest> streamSubscriptionPolicies(
        io.grpc.stub.StreamObserver<io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse> responseObserver) {
      return asyncUnimplementedStreamingCall(getStreamSubscriptionPoliciesMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getStreamSubscriptionPoliciesMethod(),
            asyncBidiStreamingCall(
              new MethodHandlers<
                io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest,
                io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse>(
                  this, METHODID_STREAM_SUBSCRIPTION_POLICIES)))
          .build();
    }
  }

  /**
   * <pre>
   * [#protodoc-title: SubPolicyDS]
   * </pre>
   */
  public static final class SubscriptionPolicyDiscoveryServiceStub extends io.grpc.stub.AbstractAsyncStub<SubscriptionPolicyDiscoveryServiceStub> {
    private SubscriptionPolicyDiscoveryServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SubscriptionPolicyDiscoveryServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SubscriptionPolicyDiscoveryServiceStub(channel, callOptions);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest> streamSubscriptionPolicies(
        io.grpc.stub.StreamObserver<io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse> responseObserver) {
      return asyncBidiStreamingCall(
          getChannel().newCall(getStreamSubscriptionPoliciesMethod(), getCallOptions()), responseObserver);
    }
  }

  /**
   * <pre>
   * [#protodoc-title: SubPolicyDS]
   * </pre>
   */
  public static final class SubscriptionPolicyDiscoveryServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<SubscriptionPolicyDiscoveryServiceBlockingStub> {
    private SubscriptionPolicyDiscoveryServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SubscriptionPolicyDiscoveryServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SubscriptionPolicyDiscoveryServiceBlockingStub(channel, callOptions);
    }
  }

  /**
   * <pre>
   * [#protodoc-title: SubPolicyDS]
   * </pre>
   */
  public static final class SubscriptionPolicyDiscoveryServiceFutureStub extends io.grpc.stub.AbstractFutureStub<SubscriptionPolicyDiscoveryServiceFutureStub> {
    private SubscriptionPolicyDiscoveryServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SubscriptionPolicyDiscoveryServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SubscriptionPolicyDiscoveryServiceFutureStub(channel, callOptions);
    }
  }

  private static final int METHODID_STREAM_SUBSCRIPTION_POLICIES = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final SubscriptionPolicyDiscoveryServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(SubscriptionPolicyDiscoveryServiceImplBase serviceImpl, int methodId) {
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
        case METHODID_STREAM_SUBSCRIPTION_POLICIES:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.streamSubscriptionPolicies(
              (io.grpc.stub.StreamObserver<io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class SubscriptionPolicyDiscoveryServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    SubscriptionPolicyDiscoveryServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return org.wso2.choreo.connect.discovery.service.subscription.SubPolicyDSProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("SubscriptionPolicyDiscoveryService");
    }
  }

  private static final class SubscriptionPolicyDiscoveryServiceFileDescriptorSupplier
      extends SubscriptionPolicyDiscoveryServiceBaseDescriptorSupplier {
    SubscriptionPolicyDiscoveryServiceFileDescriptorSupplier() {}
  }

  private static final class SubscriptionPolicyDiscoveryServiceMethodDescriptorSupplier
      extends SubscriptionPolicyDiscoveryServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    SubscriptionPolicyDiscoveryServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (SubscriptionPolicyDiscoveryServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new SubscriptionPolicyDiscoveryServiceFileDescriptorSupplier())
              .addMethod(getStreamSubscriptionPoliciesMethod())
              .build();
        }
      }
    }
    return result;
  }
}
