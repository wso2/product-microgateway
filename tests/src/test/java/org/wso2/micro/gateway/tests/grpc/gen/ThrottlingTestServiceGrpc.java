package org.wso2.micro.gateway.tests.grpc.gen;

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
    value = "by gRPC proto compiler (version 1.27.0)",
    comments = "Source: test.proto")
public final class ThrottlingTestServiceGrpc {

  private ThrottlingTestServiceGrpc() {}

  public static final String SERVICE_NAME = "ThrottlingTestService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<org.wso2.micro.gateway.tests.grpc.gen.TestRequest,
      org.wso2.micro.gateway.tests.grpc.gen.TestResponse> getTestCallServiceThrottlingMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "testCallServiceThrottling",
      requestType = org.wso2.micro.gateway.tests.grpc.gen.TestRequest.class,
      responseType = org.wso2.micro.gateway.tests.grpc.gen.TestResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.wso2.micro.gateway.tests.grpc.gen.TestRequest,
      org.wso2.micro.gateway.tests.grpc.gen.TestResponse> getTestCallServiceThrottlingMethod() {
    io.grpc.MethodDescriptor<org.wso2.micro.gateway.tests.grpc.gen.TestRequest, org.wso2.micro.gateway.tests.grpc.gen.TestResponse> getTestCallServiceThrottlingMethod;
    if ((getTestCallServiceThrottlingMethod = ThrottlingTestServiceGrpc.getTestCallServiceThrottlingMethod) == null) {
      synchronized (ThrottlingTestServiceGrpc.class) {
        if ((getTestCallServiceThrottlingMethod = ThrottlingTestServiceGrpc.getTestCallServiceThrottlingMethod) == null) {
          ThrottlingTestServiceGrpc.getTestCallServiceThrottlingMethod = getTestCallServiceThrottlingMethod =
              io.grpc.MethodDescriptor.<org.wso2.micro.gateway.tests.grpc.gen.TestRequest, org.wso2.micro.gateway.tests.grpc.gen.TestResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "testCallServiceThrottling"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.wso2.micro.gateway.tests.grpc.gen.TestRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.wso2.micro.gateway.tests.grpc.gen.TestResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ThrottlingTestServiceMethodDescriptorSupplier("testCallServiceThrottling"))
              .build();
        }
      }
    }
    return getTestCallServiceThrottlingMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.wso2.micro.gateway.tests.grpc.gen.TestRequest,
      org.wso2.micro.gateway.tests.grpc.gen.TestResponse> getTestCallMethodThrottlingMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "testCallMethodThrottling",
      requestType = org.wso2.micro.gateway.tests.grpc.gen.TestRequest.class,
      responseType = org.wso2.micro.gateway.tests.grpc.gen.TestResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.wso2.micro.gateway.tests.grpc.gen.TestRequest,
      org.wso2.micro.gateway.tests.grpc.gen.TestResponse> getTestCallMethodThrottlingMethod() {
    io.grpc.MethodDescriptor<org.wso2.micro.gateway.tests.grpc.gen.TestRequest, org.wso2.micro.gateway.tests.grpc.gen.TestResponse> getTestCallMethodThrottlingMethod;
    if ((getTestCallMethodThrottlingMethod = ThrottlingTestServiceGrpc.getTestCallMethodThrottlingMethod) == null) {
      synchronized (ThrottlingTestServiceGrpc.class) {
        if ((getTestCallMethodThrottlingMethod = ThrottlingTestServiceGrpc.getTestCallMethodThrottlingMethod) == null) {
          ThrottlingTestServiceGrpc.getTestCallMethodThrottlingMethod = getTestCallMethodThrottlingMethod =
              io.grpc.MethodDescriptor.<org.wso2.micro.gateway.tests.grpc.gen.TestRequest, org.wso2.micro.gateway.tests.grpc.gen.TestResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "testCallMethodThrottling"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.wso2.micro.gateway.tests.grpc.gen.TestRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.wso2.micro.gateway.tests.grpc.gen.TestResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ThrottlingTestServiceMethodDescriptorSupplier("testCallMethodThrottling"))
              .build();
        }
      }
    }
    return getTestCallMethodThrottlingMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ThrottlingTestServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ThrottlingTestServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ThrottlingTestServiceStub>() {
        @java.lang.Override
        public ThrottlingTestServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ThrottlingTestServiceStub(channel, callOptions);
        }
      };
    return ThrottlingTestServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ThrottlingTestServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ThrottlingTestServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ThrottlingTestServiceBlockingStub>() {
        @java.lang.Override
        public ThrottlingTestServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ThrottlingTestServiceBlockingStub(channel, callOptions);
        }
      };
    return ThrottlingTestServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static ThrottlingTestServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ThrottlingTestServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ThrottlingTestServiceFutureStub>() {
        @java.lang.Override
        public ThrottlingTestServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ThrottlingTestServiceFutureStub(channel, callOptions);
        }
      };
    return ThrottlingTestServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class ThrottlingTestServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public void testCallServiceThrottling(org.wso2.micro.gateway.tests.grpc.gen.TestRequest request,
        io.grpc.stub.StreamObserver<org.wso2.micro.gateway.tests.grpc.gen.TestResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getTestCallServiceThrottlingMethod(), responseObserver);
    }

    /**
     */
    public void testCallMethodThrottling(org.wso2.micro.gateway.tests.grpc.gen.TestRequest request,
        io.grpc.stub.StreamObserver<org.wso2.micro.gateway.tests.grpc.gen.TestResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getTestCallMethodThrottlingMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getTestCallServiceThrottlingMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.wso2.micro.gateway.tests.grpc.gen.TestRequest,
                org.wso2.micro.gateway.tests.grpc.gen.TestResponse>(
                  this, METHODID_TEST_CALL_SERVICE_THROTTLING)))
          .addMethod(
            getTestCallMethodThrottlingMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.wso2.micro.gateway.tests.grpc.gen.TestRequest,
                org.wso2.micro.gateway.tests.grpc.gen.TestResponse>(
                  this, METHODID_TEST_CALL_METHOD_THROTTLING)))
          .build();
    }
  }

  /**
   */
  public static final class ThrottlingTestServiceStub extends io.grpc.stub.AbstractAsyncStub<ThrottlingTestServiceStub> {
    private ThrottlingTestServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ThrottlingTestServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ThrottlingTestServiceStub(channel, callOptions);
    }

    /**
     */
    public void testCallServiceThrottling(org.wso2.micro.gateway.tests.grpc.gen.TestRequest request,
        io.grpc.stub.StreamObserver<org.wso2.micro.gateway.tests.grpc.gen.TestResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getTestCallServiceThrottlingMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void testCallMethodThrottling(org.wso2.micro.gateway.tests.grpc.gen.TestRequest request,
        io.grpc.stub.StreamObserver<org.wso2.micro.gateway.tests.grpc.gen.TestResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getTestCallMethodThrottlingMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class ThrottlingTestServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<ThrottlingTestServiceBlockingStub> {
    private ThrottlingTestServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ThrottlingTestServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ThrottlingTestServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public org.wso2.micro.gateway.tests.grpc.gen.TestResponse testCallServiceThrottling(org.wso2.micro.gateway.tests.grpc.gen.TestRequest request) {
      return blockingUnaryCall(
          getChannel(), getTestCallServiceThrottlingMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.wso2.micro.gateway.tests.grpc.gen.TestResponse testCallMethodThrottling(org.wso2.micro.gateway.tests.grpc.gen.TestRequest request) {
      return blockingUnaryCall(
          getChannel(), getTestCallMethodThrottlingMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class ThrottlingTestServiceFutureStub extends io.grpc.stub.AbstractFutureStub<ThrottlingTestServiceFutureStub> {
    private ThrottlingTestServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ThrottlingTestServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ThrottlingTestServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.wso2.micro.gateway.tests.grpc.gen.TestResponse> testCallServiceThrottling(
        org.wso2.micro.gateway.tests.grpc.gen.TestRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getTestCallServiceThrottlingMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.wso2.micro.gateway.tests.grpc.gen.TestResponse> testCallMethodThrottling(
        org.wso2.micro.gateway.tests.grpc.gen.TestRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getTestCallMethodThrottlingMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_TEST_CALL_SERVICE_THROTTLING = 0;
  private static final int METHODID_TEST_CALL_METHOD_THROTTLING = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final ThrottlingTestServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(ThrottlingTestServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_TEST_CALL_SERVICE_THROTTLING:
          serviceImpl.testCallServiceThrottling((org.wso2.micro.gateway.tests.grpc.gen.TestRequest) request,
              (io.grpc.stub.StreamObserver<org.wso2.micro.gateway.tests.grpc.gen.TestResponse>) responseObserver);
          break;
        case METHODID_TEST_CALL_METHOD_THROTTLING:
          serviceImpl.testCallMethodThrottling((org.wso2.micro.gateway.tests.grpc.gen.TestRequest) request,
              (io.grpc.stub.StreamObserver<org.wso2.micro.gateway.tests.grpc.gen.TestResponse>) responseObserver);
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
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class ThrottlingTestServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    ThrottlingTestServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return org.wso2.micro.gateway.tests.grpc.gen.Test.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("ThrottlingTestService");
    }
  }

  private static final class ThrottlingTestServiceFileDescriptorSupplier
      extends ThrottlingTestServiceBaseDescriptorSupplier {
    ThrottlingTestServiceFileDescriptorSupplier() {}
  }

  private static final class ThrottlingTestServiceMethodDescriptorSupplier
      extends ThrottlingTestServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    ThrottlingTestServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (ThrottlingTestServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new ThrottlingTestServiceFileDescriptorSupplier())
              .addMethod(getTestCallServiceThrottlingMethod())
              .addMethod(getTestCallMethodThrottlingMethod())
              .build();
        }
      }
    }
    return result;
  }
}
