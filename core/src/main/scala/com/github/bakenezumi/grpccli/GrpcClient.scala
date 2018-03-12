package com.github.bakenezumi.grpccli

import java.util.concurrent.TimeUnit
import java.util.logging.Logger

import com.github.bakenezumi.grpccli.ServerReflectionGrpc.ServerReflection
import com.github.bakenezumi.grpccli.protobuf.{ProtoMethodName, ServiceResolver}
import com.google.protobuf.DescriptorProtos.{
  FileDescriptorProto,
  FileDescriptorSet
}
import com.google.protobuf.DynamicMessage
import com.google.protobuf.util.JsonFormat.TypeRegistry
import io.grpc.reflection.v1alpha.{
  ServerReflectionRequest,
  ServerReflectionResponse,
  ServiceResponse
}
import io.grpc.stub.StreamObserver
import io.grpc.{CallOptions, ManagedChannel, ManagedChannelBuilder}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future, Promise}

object GrpcClient {
  def apply(host: String, port: Int)(
      implicit executorContext: ExecutionContext): GrpcClient = {
    val channel =
      ManagedChannelBuilder.forAddress(host, port).usePlaintext(true).build
    val serverReflectionStub = ServerReflectionGrpc.stub(channel)
    new GrpcClient(channel, serverReflectionStub)
  }

}

class GrpcClient private (
    private val channel: ManagedChannel,
    private val asyncStub: ServerReflection
)(implicit executorContext: ExecutionContext) {

  private[this] val logger =
    Logger.getLogger(classOf[GrpcClient].getName)

  def shutdown(): Unit = {
    channel.shutdown.awaitTermination(5, TimeUnit.SECONDS)
  }

  private def callServerReflection[RETURN](
      request: ServerReflectionRequest,
      handler: ServerReflectionResponse => RETURN): Future[RETURN] = {

    val p = Promise[RETURN]()
    val requestObserver = asyncStub.serverReflectionInfo(
      new StreamObserver[ServerReflectionResponse] {
        override def onNext(v: ServerReflectionResponse): Unit = {
          //logger.info(v.toString)
          p.success(handler(v))
        }

        override def onError(throwable: Throwable): Unit = {
          logger.warning(throwable.getLocalizedMessage)
          p.failure(throwable)
        }

        override def onCompleted(): Unit = {}

      })

    requestObserver.onNext(request)
    requestObserver.onCompleted()
    p.future
  }

  /** Get a list of `ServiceResponse` by server reflection
    * */
  def getServiceResponses(
      serviceName: String = ""): Future[List[ServiceResponse]] =
    callServerReflection(
      ServerReflectionRequest.newBuilder.setListServices(serviceName).build,
      _.getListServicesResponse.getServiceList.asScala.toList)

  /** Get a list of service name by server reflection
    * */
  def getServiceNames(serviceName: String = ""): Future[List[String]] =
    getServiceResponses().map(_.map(_.getName))

  /** Get all in one `FileDescriptorProtoSet` by server reflection
    * */
  def getAllInOneFileDescriptorProtoSet: Future[FileDescriptorSet] =
    getServiceNames()
      .flatMap { serviceNames =>
        val futures =
          serviceNames.map { serviceName =>
            getFileDescriptorProtoList(serviceName)
          }
        Future.foldLeft(futures)(Set[FileDescriptorProto]()) { (acc, v) =>
          acc ++ v
        }
      }
      .map(
        fileDescriptorProtoList =>
          FileDescriptorSet
            .newBuilder()
            .addAllFile(fileDescriptorProtoList.asJava)
            .build())

  /** Get a list of `FileDescriptorProto` by server reflection
    * */
  def getFileDescriptorProtoList(
      symbol: String): Future[Seq[FileDescriptorProto]] = {
    callServerReflection(
      ServerReflectionRequest.newBuilder.setFileContainingSymbol(symbol).build,
      _.getFileDescriptorResponse.getFileDescriptorProtoList.asScala
        .map(FileDescriptorProto.parseFrom)
    )
  }

  /** Get `FileDescriptorProtoSet` by server reflection
    * */
  def getFileDescriptorProtoSet(symbol: String): Future[FileDescriptorSet] = {
    callServerReflection(
      ServerReflectionRequest.newBuilder
        .setFileContainingSymbol(symbol)
        .build, { serverReflectionResponse =>
        val fileDescriptorProtoList =
          serverReflectionResponse.getFileDescriptorResponse.getFileDescriptorProtoList.asScala
            .map(FileDescriptorProto.parseFrom)
        FileDescriptorSet
          .newBuilder()
          .addAllFile(fileDescriptorProtoList.asJava)
          .build()
      }
    )
  }

  private def formatMethodName(methodName: String): String =
    if (methodName.contains('/')) methodName
    else {
      val words = methodName.split("\\.").toList
      if (words.isEmpty) methodName
      else words.init.mkString(".") + "/" + words.last
    }

  /** call gRPC service method
    * */
  def callDynamic(fileDescriptorSet: FileDescriptorSet,
                  methodName: String): Future[Unit] = {
    val serviceResolver =
      ServiceResolver.fromFileDescriptorSet(fileDescriptorSet)
    val protoMethodName =
      ProtoMethodName.parseFullGrpcMethodName(formatMethodName(methodName))
    val methodDescriptor =
      serviceResolver.resolveServiceMethod(protoMethodName)
    val dynamicClient = DynamicGrpc(methodDescriptor, channel)
    val registry = TypeRegistry
      .newBuilder()
      .add(serviceResolver.listMessageTypes.asJava)
      .build()

    val requestMessages =
      MessageReader.forStdin(methodDescriptor.getInputType, registry).read

    val p = Promise[Unit]()

    dynamicClient.call(
      requestMessages,
      new StreamObserver[DynamicMessage] {
        override def onNext(v: DynamicMessage): Unit = {
          println(v.toString)
        }

        override def onError(throwable: Throwable): Unit = {
          logger.warning(throwable.getLocalizedMessage)
          p.failure(throwable)
        }

        override def onCompleted(): Unit = {
          p.success(())
        }

      },
      CallOptions.DEFAULT
    )
    p.future
  }

}

sealed trait ServiceListFormat
object ServiceListFormat {
  object SHORT extends ServiceListFormat
  object LONG extends ServiceListFormat
}
