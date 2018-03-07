package com.github.bakenezumi.grpccli

import com.google.protobuf.Descriptors
import io.grpc._
import io.grpc.protobuf.ProtoUtils
import io.grpc.reflection.v1alpha.{
  ServerReflectionProto,
  ServerReflectionRequest,
  ServerReflectionResponse
}
import io.grpc.stub.{AbstractStub, ClientCalls, StreamObserver}

object ServerReflectionGrpc {
  private[this] val METHOD_SERVER_REFLECTION_INFO
    : MethodDescriptor[ServerReflectionRequest, ServerReflectionResponse] =
    MethodDescriptor
      .newBuilder()
      .setType(MethodDescriptor.MethodType.BIDI_STREAMING)
      .setFullMethodName(
        MethodDescriptor
          .generateFullMethodName("grpc.reflection.v1alpha.ServerReflection",
                                  "ServerReflectionInfo"))
      .setRequestMarshaller(
        ProtoUtils.marshaller(ServerReflectionRequest.getDefaultInstance))
      .setResponseMarshaller(
        ProtoUtils.marshaller(ServerReflectionResponse.getDefaultInstance))
      .build()

  trait ServerReflection {
    def serverReflectionInfo(
        responseObserver: StreamObserver[ServerReflectionResponse])
      : StreamObserver[ServerReflectionRequest]
  }

  object ServerReflection {
    def javaDescriptor: Descriptors.ServiceDescriptor =
      ServerReflectionProto.getDescriptor.getServices
        .get(0)
  }

  class ServerReflectionStub(channel: Channel,
                             options: CallOptions = CallOptions.DEFAULT)
      extends AbstractStub[ServerReflectionStub](channel, options)
      with ServerReflection {
    override def serverReflectionInfo(
        responseObserver: StreamObserver[ServerReflectionResponse])
      : StreamObserver[ServerReflectionRequest] = {
      ClientCalls.asyncBidiStreamingCall(
        channel.newCall(METHOD_SERVER_REFLECTION_INFO, options),
        responseObserver)
    }

    override def build(channel: Channel,
                       options: CallOptions): ServerReflectionStub =
      new ServerReflectionStub(channel, options)
  }

  def stub(channel: Channel): ServerReflectionStub =
    new ServerReflectionStub(channel)

  def javaDescriptor: _root_.com.google.protobuf.Descriptors.ServiceDescriptor =
    ServerReflectionProto.getDescriptor.getServices
      .get(0)

}
