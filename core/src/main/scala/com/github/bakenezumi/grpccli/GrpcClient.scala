package com.github.bakenezumi.grpccli

import java.util.concurrent.TimeUnit
import java.util.logging.Logger

import com.github.bakenezumi.grpccli.ServerReflectionGrpc.ServerReflectionStub
import com.github.bakenezumi.grpccli.protobuf.{ProtoMethodName, ServiceResolver}
import com.google.protobuf.DescriptorProtos.{
  FileDescriptorProto,
  FileDescriptorSet
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
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.language.postfixOps

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
    private val asyncStub: ServerReflectionStub
)(implicit executorContext: ExecutionContext) {

  private[this] val logger = Logger.getLogger(classOf[GrpcClient].getName)

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

  def getServiceList(serviceNameParameter: String = "",
                     format: ServiceListFormat = ServiceListFormat.SHORT)
    : Future[Seq[String]] = {
    val serviceNamesFuture = callServerReflection(
      ServerReflectionRequest.newBuilder
        .setListServices(serviceNameParameter)
        .build,
      _.getListServicesResponse.getServiceList.asScala.toList
        .map(_.getName)
    )

    def serviceDetail(serviceName: String): Future[String] = {
      getFileDescriptorProtoList(serviceName).map(
        _.flatMap(file =>
          file.getServiceList.asScala.map { serviceDescriptor =>
            (file.getName, file.getPackage, serviceDescriptor)
        }).find {
            case (_, pkg, serviceDescriptor) =>
              if (serviceNameParameter.isEmpty)
                serviceName.startsWith(
                  formatPackage(pkg) + serviceDescriptor.getName)
              else
                serviceNameParameter.startsWith(
                  formatPackage(pkg) + serviceDescriptor.getName)
          }
          .flatMap {
            case (file, pkg, serviceDescriptor) =>
              // print service
              if (serviceNameParameter.isEmpty || serviceNameParameter
                    .endsWith(serviceDescriptor.getName))
                Some(
                  format match {
                    case ServiceListFormat.SHORT =>
                      serviceDescriptor.getMethodList.asScala.toList
                        .map(_.getName)
                        .mkString(System.lineSeparator)
                    case ServiceListFormat.LONG =>
                      s"""|filename: "$file"
                          |package: "$pkg"
                          |$serviceDescriptor""".stripMargin // TODO: to protobuf format
                  }
                )
              // print method
              else {
                serviceDescriptor.getMethodList.asScala
                  .find(method =>
                    serviceNameParameter == formatPackage(pkg) + serviceDescriptor.getName + "." + method.getName)
                  .map(method =>
                    format match {
                      case ServiceListFormat.SHORT =>
                        method.getName
                      case ServiceListFormat.LONG =>
                        method.toString // TODO: to protobuf format
                  })
              }
          }
          .getOrElse("")
      )
    }

    format match {
      case ServiceListFormat.SHORT =>
        if (serviceNameParameter.isEmpty)
          serviceNamesFuture
        else
          serviceDetail(serviceNameParameter).map(s => Seq(s))

      case ServiceListFormat.LONG =>
        serviceNamesFuture.flatMap { serviceNames =>
          val futures = serviceNames.map(serviceDetail)

          Future.foldLeft[String, Seq[String]](futures)(Seq[String]()) {
            (acc: Seq[String], v: String) =>
              if (v.nonEmpty) acc ++ Seq(v) else acc
          }
        }
    }

  }

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
                  descriptor.toString // TODO: to protobuf format
            }
        ))
  }

  def getFileDescriptorProtoList(
      symbol: String): Future[Seq[FileDescriptorProto]] = {
    callServerReflection(
      ServerReflectionRequest.newBuilder.setFileContainingSymbol(symbol).build,
      _.getFileDescriptorResponse.getFileDescriptorProtoList.asScala
        .map(FileDescriptorProto.parseFrom)
    )
  }

  private def formatMethodName(methodName: String): String =
    if (methodName.contains('/')) methodName
    else {
      val words = methodName.split("\\.").toList
      if (words.isEmpty) methodName
      else words.init.mkString(".") + "/" + words.last
    }

  def callDynamic(methodName: String): Future[Unit] = {
    val fileDescriptors =
      Await.result(getFileDescriptorProtoList(methodName), 5 second)

    val descriptorSet = FileDescriptorSet
      .newBuilder()
      .addAllFile(fileDescriptors.asJava)
      .build()
    val serviceResolver =
      ServiceResolver.fromFileDescriptorSet(descriptorSet)
    val protoMethodName =
      ProtoMethodName.parseFullGrpcMethodName(formatMethodName(methodName))
    val methodDescriptor =
      serviceResolver.resolveServiceMethod(protoMethodName)
    val dynamicClient = DynamicGrpcClient(methodDescriptor, channel)
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
          p.success()
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
