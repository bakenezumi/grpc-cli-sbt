package com.github.bakenezumi.grpccli

import java.util.concurrent.TimeUnit
import java.util.logging.Logger

import com.github.bakenezumi.grpccli.ServerReflectionGrpc.ServerReflection
import com.github.bakenezumi.grpccli.protobuf.{
  ProtoMethodName,
  ProtobufFormat,
  ServiceResolver
}
import com.google.protobuf.DescriptorProtos.{
  FileDescriptorProto,
  FileDescriptorSet,
  MethodDescriptorProto,
  ServiceDescriptorProto
}
import com.google.protobuf.DynamicMessage
import com.google.protobuf.util.JsonFormat.TypeRegistry
import io.grpc.reflection.v1alpha.{
  ServerReflectionRequest,
  ServerReflectionResponse
}
import io.grpc.stub.StreamObserver
import io.grpc.{CallOptions, ManagedChannel, ManagedChannelBuilder}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future, Promise}

object ServerReflectionGrpcClient {
  def apply(host: String, port: Int)(implicit executorContext: ExecutionContext)
    : ServerReflectionGrpcClient = {
    val channel =
      ManagedChannelBuilder.forAddress(host, port).usePlaintext(true).build
    val serverReflectionStub = ServerReflectionGrpc.stub(channel)
    new ServerReflectionGrpcClient(channel, serverReflectionStub)
  }

}

class ServerReflectionGrpcClient private (
    private val channel: ManagedChannel,
    private val asyncStub: ServerReflection
)(implicit executorContext: ExecutionContext) {

  private[this] val logger =
    Logger.getLogger(classOf[ServerReflectionGrpcClient].getName)

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

  private def formatPackage(packageName: String): String =
    if (packageName.isEmpty) ""
    else packageName + "."

  /** Get a list of service by server reflection
    * */
  def getServiceList(serviceNameParameter: String = "",
                     format: ServiceListFormat = ServiceListFormat.SHORT)
    : Future[Seq[String]] = {

    val serviceNamesFuture = () =>
      callServerReflection(
        ServerReflectionRequest.newBuilder
          .setListServices(serviceNameParameter)
          .build,
        _.getListServicesResponse.getServiceList.asScala.toList
          .map(_.getName)
    )

    def separateHandler(serviceHandler: (
                            (FileDescriptorProto,
                             ServiceDescriptorProto) => Seq[String]),
                        methodHandler: MethodDescriptorProto => Seq[String])
      : (FileDescriptorProto, ServiceDescriptorProto) => Seq[String] =
      (fileDescriptorProto: FileDescriptorProto,
       serviceDescriptorProto: ServiceDescriptorProto) =>
        // handle service
        if (serviceNameParameter.isEmpty ||
            serviceNameParameter
              .endsWith(serviceDescriptorProto.getName))
          serviceHandler(fileDescriptorProto, serviceDescriptorProto)
        // handle methods
        else {
          val pkg = fileDescriptorProto.getPackage
          serviceDescriptorProto.getMethodList.asScala
            .find(method =>
              serviceNameParameter == formatPackage(pkg) + serviceDescriptorProto.getName + "." + method.getName ||
                serviceNameParameter == formatPackage(pkg) + serviceDescriptorProto.getName + "/" + method.getName)
            .map(methodHandler)
            .getOrElse(Nil)
      }

    format match {
      // ls
      case ServiceListFormat.SHORT if serviceNameParameter.isEmpty =>
        serviceNamesFuture()

      // ls service
      case ServiceListFormat.SHORT =>
        val handler = separateHandler(
          (_, serviceDescriptorProto) =>
            serviceDescriptorProto.getMethodList.asScala.toList
              .map(_.getName),
          method => Seq(method.getName)
        )
        getServiceDescriptorProto(
          serviceNameParameter,
          handler
        )

      // ls -l [service]
      case ServiceListFormat.LONG =>
        val handler = separateHandler(
          (fileDescriptorProto, serviceDescriptorProto) =>
            Seq(
              ProtobufFormat.print(fileDescriptorProto,
                                   serviceDescriptorProto)),
          method =>
            Seq(
              "  " + ProtobufFormat
                .print(method) + System.lineSeparator)
        )
        serviceNamesFuture().flatMap { serviceNames =>
          val futures =
            serviceNames.map(
              serviceName =>
                getServiceDescriptorProto(
                  serviceName,
                  handler
              ))
          // Seq[Future[Seq[String]]] to Future[Seq[String]]
          Future.foldLeft(futures)(Seq[String]()) { (acc, v) =>
            if (v.nonEmpty) acc ++ v else acc
          }
        }

    }

  }

  /** Get a message type  by server reflection
    * */
  def getType(typeName: String): Future[Seq[String]] = {
    getFileDescriptorProtoList(typeName)
      .map(
        _.flatMap(
          (file: FileDescriptorProto) =>
            file.getMessageTypeList.asScala
              .map(descriptor => (file.getName, file.getPackage, descriptor))
              .collect {
                case (_, pkg, descriptor)
                    if typeName == formatPackage(pkg) + descriptor.getName =>
                  ProtobufFormat
                    .print(descriptor)
            }
        ))
  }

  private def getServiceDescriptorProto[T](
      serviceName: String,
      handler: ((FileDescriptorProto, ServiceDescriptorProto) => Seq[T]))
    : Future[Seq[T]] = {
    getFileDescriptorProtoList(serviceName).map {
      _.flatMap(fileDescriptorProto =>
        fileDescriptorProto.getServiceList.asScala.map {
          serviceDescriptorProto =>
            (fileDescriptorProto, serviceDescriptorProto)
      }).headOption
        .map {
          case (fileDescriptorProto, serviceDescriptorProto) =>
            handler(fileDescriptorProto, serviceDescriptorProto)
        }
        .getOrElse(Nil)
    }
  }

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
