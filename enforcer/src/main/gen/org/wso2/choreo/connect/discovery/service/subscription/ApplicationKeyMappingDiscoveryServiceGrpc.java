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
 * [#protodoc-title: AppKeyMappingDS]
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler",
    comments = "Source: wso2/discovery/service/subscription/app_key_mapping_ds.proto")
public final class ApplicationKeyMappingDiscoveryServiceGrpc {

  private ApplicationKeyMappingDiscoveryServiceGrpc() {}

  public static final String SERVICE_NAME = "discovery.service.subscription.ApplicationKeyMappingDiscoveryService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest,
      io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse> getStreamApplicationKeyMappingsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "StreamApplicationKeyMappings",
      requestType = io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest.class,
      responseType = io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
  public static io.grpc.MethodDescriptor<io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest,
      io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse> getStreamApplicationKeyMappingsMethod() {
    io.grpc.MethodDescriptor<io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest, io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse> getStreamApplicationKeyMappingsMethod;
    if ((getStreamApplicationKeyMappingsMethod = ApplicationKeyMappingDiscoveryServiceGrpc.getStreamApplicationKeyMappingsMethod) == null) {
      synchronized (ApplicationKeyMappingDiscoveryServiceGrpc.class) {
        if ((getStreamApplicationKeyMappingsMethod = ApplicationKeyMappingDiscoveryServiceGrpc.getStreamApplicationKeyMappingsMethod) == null) {
          ApplicationKeyMappingDiscoveryServiceGrpc.getStreamApplicationKeyMappingsMethod = getStreamApplicationKeyMappingsMethod =
              io.grpc.MethodDescriptor.<io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest, io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "StreamApplicationKeyMappings"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ApplicationKeyMappingDiscoveryServiceMethodDescriptorSupplier("StreamApplicationKeyMappings"))
              .build();
        }
      }
    }
    return getStreamApplicationKeyMappingsMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ApplicationKeyMappingDiscoveryServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ApplicationKeyMappingDiscoveryServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ApplicationKeyMappingDiscoveryServiceStub>() {
        @java.lang.Override
        public ApplicationKeyMappingDiscoveryServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ApplicationKeyMappingDiscoveryServiceStub(channel, callOptions);
        }
      };
    return ApplicationKeyMappingDiscoveryServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ApplicationKeyMappingDiscoveryServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ApplicationKeyMappingDiscoveryServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ApplicationKeyMappingDiscoveryServiceBlockingStub>() {
        @java.lang.Override
        public ApplicationKeyMappingDiscoveryServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ApplicationKeyMappingDiscoveryServiceBlockingStub(channel, callOptions);
        }
      };
    return ApplicationKeyMappingDiscoveryServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static ApplicationKeyMappingDiscoveryServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ApplicationKeyMappingDiscoveryServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ApplicationKeyMappingDiscoveryServiceFutureStub>() {
        @java.lang.Override
        public ApplicationKeyMappingDiscoveryServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ApplicationKeyMappingDiscoveryServiceFutureStub(channel, callOptions);
        }
      };
    return ApplicationKeyMappingDiscoveryServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * [#protodoc-title: AppKeyMappingDS]
   * </pre>
   */
  public static abstract class ApplicationKeyMappingDiscoveryServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public io.grpc.stub.StreamObserver<io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest> streamApplicationKeyMappings(
        io.grpc.stub.StreamObserver<io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse> responseObserver) {
      return asyncUnimplementedStreamingCall(getStreamApplicationKeyMappingsMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getStreamApplicationKeyMappingsMethod(),
            asyncBidiStreamingCall(
              new MethodHandlers<
                io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest,
                io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse>(
                  this, METHODID_STREAM_APPLICATION_KEY_MAPPINGS)))
          .build();
    }
  }

  /**
   * <pre>
   * [#protodoc-title: AppKeyMappingDS]
   * </pre>
   */
  public static final class ApplicationKeyMappingDiscoveryServiceStub extends io.grpc.stub.AbstractAsyncStub<ApplicationKeyMappingDiscoveryServiceStub> {
    private ApplicationKeyMappingDiscoveryServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ApplicationKeyMappingDiscoveryServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ApplicationKeyMappingDiscoveryServiceStub(channel, callOptions);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest> streamApplicationKeyMappings(
        io.grpc.stub.StreamObserver<io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse> responseObserver) {
      return asyncBidiStreamingCall(
          getChannel().newCall(getStreamApplicationKeyMappingsMethod(), getCallOptions()), responseObserver);
    }
  }

  /**
   * <pre>
   * [#protodoc-title: AppKeyMappingDS]
   * </pre>
   */
  public static final class ApplicationKeyMappingDiscoveryServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<ApplicationKeyMappingDiscoveryServiceBlockingStub> {
    private ApplicationKeyMappingDiscoveryServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ApplicationKeyMappingDiscoveryServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ApplicationKeyMappingDiscoveryServiceBlockingStub(channel, callOptions);
    }
  }

  /**
   * <pre>
   * [#protodoc-title: AppKeyMappingDS]
   * </pre>
   */
  public static final class ApplicationKeyMappingDiscoveryServiceFutureStub extends io.grpc.stub.AbstractFutureStub<ApplicationKeyMappingDiscoveryServiceFutureStub> {
    private ApplicationKeyMappingDiscoveryServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ApplicationKeyMappingDiscoveryServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ApplicationKeyMappingDiscoveryServiceFutureStub(channel, callOptions);
    }
  }

  private static final int METHODID_STREAM_APPLICATION_KEY_MAPPINGS = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final ApplicationKeyMappingDiscoveryServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(ApplicationKeyMappingDiscoveryServiceImplBase serviceImpl, int methodId) {
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
        case METHODID_STREAM_APPLICATION_KEY_MAPPINGS:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.streamApplicationKeyMappings(
              (io.grpc.stub.StreamObserver<io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class ApplicationKeyMappingDiscoveryServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    ApplicationKeyMappingDiscoveryServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return org.wso2.choreo.connect.discovery.service.subscription.AppKeyMappingDSProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("ApplicationKeyMappingDiscoveryService");
    }
  }

  private static final class ApplicationKeyMappingDiscoveryServiceFileDescriptorSupplier
      extends ApplicationKeyMappingDiscoveryServiceBaseDescriptorSupplier {
    ApplicationKeyMappingDiscoveryServiceFileDescriptorSupplier() {}
  }

  private static final class ApplicationKeyMappingDiscoveryServiceMethodDescriptorSupplier
      extends ApplicationKeyMappingDiscoveryServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    ApplicationKeyMappingDiscoveryServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (ApplicationKeyMappingDiscoveryServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new ApplicationKeyMappingDiscoveryServiceFileDescriptorSupplier())
              .addMethod(getStreamApplicationKeyMappingsMethod())
              .build();
        }
      }
    }
    return result;
  }
}
