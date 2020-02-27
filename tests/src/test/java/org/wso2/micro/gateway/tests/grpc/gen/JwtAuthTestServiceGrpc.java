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
public final class JwtAuthTestServiceGrpc {

  private JwtAuthTestServiceGrpc() {}

  public static final String SERVICE_NAME = "JwtAuthTestService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<org.wso2.micro.gateway.tests.grpc.gen.TestRequest,
      org.wso2.micro.gateway.tests.grpc.gen.TestResponse> getTestCallMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "testCall",
      requestType = org.wso2.micro.gateway.tests.grpc.gen.TestRequest.class,
      responseType = org.wso2.micro.gateway.tests.grpc.gen.TestResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.wso2.micro.gateway.tests.grpc.gen.TestRequest,
      org.wso2.micro.gateway.tests.grpc.gen.TestResponse> getTestCallMethod() {
    io.grpc.MethodDescriptor<org.wso2.micro.gateway.tests.grpc.gen.TestRequest, org.wso2.micro.gateway.tests.grpc.gen.TestResponse> getTestCallMethod;
    if ((getTestCallMethod = JwtAuthTestServiceGrpc.getTestCallMethod) == null) {
      synchronized (JwtAuthTestServiceGrpc.class) {
        if ((getTestCallMethod = JwtAuthTestServiceGrpc.getTestCallMethod) == null) {
          JwtAuthTestServiceGrpc.getTestCallMethod = getTestCallMethod =
              io.grpc.MethodDescriptor.<org.wso2.micro.gateway.tests.grpc.gen.TestRequest, org.wso2.micro.gateway.tests.grpc.gen.TestResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "testCall"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.wso2.micro.gateway.tests.grpc.gen.TestRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.wso2.micro.gateway.tests.grpc.gen.TestResponse.getDefaultInstance()))
              .setSchemaDescriptor(new JwtAuthTestServiceMethodDescriptorSupplier("testCall"))
              .build();
        }
      }
    }
    return getTestCallMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.wso2.micro.gateway.tests.grpc.gen.TestRequest,
      org.wso2.micro.gateway.tests.grpc.gen.TestResponse> getTestCallWithScopesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "testCallWithScopes",
      requestType = org.wso2.micro.gateway.tests.grpc.gen.TestRequest.class,
      responseType = org.wso2.micro.gateway.tests.grpc.gen.TestResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.wso2.micro.gateway.tests.grpc.gen.TestRequest,
      org.wso2.micro.gateway.tests.grpc.gen.TestResponse> getTestCallWithScopesMethod() {
    io.grpc.MethodDescriptor<org.wso2.micro.gateway.tests.grpc.gen.TestRequest, org.wso2.micro.gateway.tests.grpc.gen.TestResponse> getTestCallWithScopesMethod;
    if ((getTestCallWithScopesMethod = JwtAuthTestServiceGrpc.getTestCallWithScopesMethod) == null) {
      synchronized (JwtAuthTestServiceGrpc.class) {
        if ((getTestCallWithScopesMethod = JwtAuthTestServiceGrpc.getTestCallWithScopesMethod) == null) {
          JwtAuthTestServiceGrpc.getTestCallWithScopesMethod = getTestCallWithScopesMethod =
              io.grpc.MethodDescriptor.<org.wso2.micro.gateway.tests.grpc.gen.TestRequest, org.wso2.micro.gateway.tests.grpc.gen.TestResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "testCallWithScopes"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.wso2.micro.gateway.tests.grpc.gen.TestRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.wso2.micro.gateway.tests.grpc.gen.TestResponse.getDefaultInstance()))
              .setSchemaDescriptor(new JwtAuthTestServiceMethodDescriptorSupplier("testCallWithScopes"))
              .build();
        }
      }
    }
    return getTestCallWithScopesMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static JwtAuthTestServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<JwtAuthTestServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<JwtAuthTestServiceStub>() {
        @java.lang.Override
        public JwtAuthTestServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new JwtAuthTestServiceStub(channel, callOptions);
        }
      };
    return JwtAuthTestServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static JwtAuthTestServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<JwtAuthTestServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<JwtAuthTestServiceBlockingStub>() {
        @java.lang.Override
        public JwtAuthTestServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new JwtAuthTestServiceBlockingStub(channel, callOptions);
        }
      };
    return JwtAuthTestServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static JwtAuthTestServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<JwtAuthTestServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<JwtAuthTestServiceFutureStub>() {
        @java.lang.Override
        public JwtAuthTestServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new JwtAuthTestServiceFutureStub(channel, callOptions);
        }
      };
    return JwtAuthTestServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class JwtAuthTestServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public void testCall(org.wso2.micro.gateway.tests.grpc.gen.TestRequest request,
        io.grpc.stub.StreamObserver<org.wso2.micro.gateway.tests.grpc.gen.TestResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getTestCallMethod(), responseObserver);
    }

    /**
     */
    public void testCallWithScopes(org.wso2.micro.gateway.tests.grpc.gen.TestRequest request,
        io.grpc.stub.StreamObserver<org.wso2.micro.gateway.tests.grpc.gen.TestResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getTestCallWithScopesMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getTestCallMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.wso2.micro.gateway.tests.grpc.gen.TestRequest,
                org.wso2.micro.gateway.tests.grpc.gen.TestResponse>(
                  this, METHODID_TEST_CALL)))
          .addMethod(
            getTestCallWithScopesMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.wso2.micro.gateway.tests.grpc.gen.TestRequest,
                org.wso2.micro.gateway.tests.grpc.gen.TestResponse>(
                  this, METHODID_TEST_CALL_WITH_SCOPES)))
          .build();
    }
  }

  /**
   */
  public static final class JwtAuthTestServiceStub extends io.grpc.stub.AbstractAsyncStub<JwtAuthTestServiceStub> {
    private JwtAuthTestServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected JwtAuthTestServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new JwtAuthTestServiceStub(channel, callOptions);
    }

    /**
     */
    public void testCall(org.wso2.micro.gateway.tests.grpc.gen.TestRequest request,
        io.grpc.stub.StreamObserver<org.wso2.micro.gateway.tests.grpc.gen.TestResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getTestCallMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void testCallWithScopes(org.wso2.micro.gateway.tests.grpc.gen.TestRequest request,
        io.grpc.stub.StreamObserver<org.wso2.micro.gateway.tests.grpc.gen.TestResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getTestCallWithScopesMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class JwtAuthTestServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<JwtAuthTestServiceBlockingStub> {
    private JwtAuthTestServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected JwtAuthTestServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new JwtAuthTestServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public org.wso2.micro.gateway.tests.grpc.gen.TestResponse testCall(org.wso2.micro.gateway.tests.grpc.gen.TestRequest request) {
      return blockingUnaryCall(
          getChannel(), getTestCallMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.wso2.micro.gateway.tests.grpc.gen.TestResponse testCallWithScopes(org.wso2.micro.gateway.tests.grpc.gen.TestRequest request) {
      return blockingUnaryCall(
          getChannel(), getTestCallWithScopesMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class JwtAuthTestServiceFutureStub extends io.grpc.stub.AbstractFutureStub<JwtAuthTestServiceFutureStub> {
    private JwtAuthTestServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected JwtAuthTestServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new JwtAuthTestServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.wso2.micro.gateway.tests.grpc.gen.TestResponse> testCall(
        org.wso2.micro.gateway.tests.grpc.gen.TestRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getTestCallMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.wso2.micro.gateway.tests.grpc.gen.TestResponse> testCallWithScopes(
        org.wso2.micro.gateway.tests.grpc.gen.TestRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getTestCallWithScopesMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_TEST_CALL = 0;
  private static final int METHODID_TEST_CALL_WITH_SCOPES = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final JwtAuthTestServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(JwtAuthTestServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_TEST_CALL:
          serviceImpl.testCall((org.wso2.micro.gateway.tests.grpc.gen.TestRequest) request,
              (io.grpc.stub.StreamObserver<org.wso2.micro.gateway.tests.grpc.gen.TestResponse>) responseObserver);
          break;
        case METHODID_TEST_CALL_WITH_SCOPES:
          serviceImpl.testCallWithScopes((org.wso2.micro.gateway.tests.grpc.gen.TestRequest) request,
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

  private static abstract class JwtAuthTestServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    JwtAuthTestServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return org.wso2.micro.gateway.tests.grpc.gen.Test.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("JwtAuthTestService");
    }
  }

  private static final class JwtAuthTestServiceFileDescriptorSupplier
      extends JwtAuthTestServiceBaseDescriptorSupplier {
    JwtAuthTestServiceFileDescriptorSupplier() {}
  }

  private static final class JwtAuthTestServiceMethodDescriptorSupplier
      extends JwtAuthTestServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    JwtAuthTestServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (JwtAuthTestServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new JwtAuthTestServiceFileDescriptorSupplier())
              .addMethod(getTestCallMethod())
              .addMethod(getTestCallWithScopesMethod())
              .build();
        }
      }
    }
    return result;
  }
}
