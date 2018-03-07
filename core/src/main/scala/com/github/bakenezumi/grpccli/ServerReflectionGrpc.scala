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
import scalapb.grpc.{AbstractService, ServiceCompanion}

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

  trait ServerReflection extends AbstractService {
    override def serviceCompanion: ServiceCompanion[ServerReflection] =
      ServerReflection
    def serverReflectionInfo(
        responseObserver: StreamObserver[ServerReflectionResponse])
      : StreamObserver[ServerReflectionRequest]
  }

  object ServerReflection extends ServiceCompanion[ServerReflection] {
    implicit def serviceCompanion: ServiceCompanion[ServerReflection] = this
    def javaDescriptor: Descriptors.ServiceDescriptor =
      ServerReflectionProto.getDescriptor.getServices
        .get(0)
  }

  trait ServerReflectionBlockingClient {
    def serviceCompanion: ServiceCompanion[ServerReflection] = ServerReflection
  }

  class ServerReflectionBlockingStub(channel: Channel,
                                     options: CallOptions = CallOptions.DEFAULT)
      extends AbstractStub[ServerReflectionBlockingStub](channel, options)
      with ServerReflectionBlockingClient {
    override def build(channel: Channel,
                       options: CallOptions): ServerReflectionBlockingStub =
      new ServerReflectionBlockingStub(channel, options)
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

  def blockingStub(channel: Channel): ServerReflectionBlockingStub =
    new ServerReflectionBlockingStub(channel)

  def stub(channel: Channel): ServerReflectionStub =
    new ServerReflectionStub(channel)

  def javaDescriptor: _root_.com.google.protobuf.Descriptors.ServiceDescriptor =
    ServerReflectionProto.getDescriptor.getServices
      .get(0)

}
