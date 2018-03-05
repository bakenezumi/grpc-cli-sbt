package com.github.bakenezumi.grpccli

import java.util.concurrent.TimeUnit
import java.util.logging.Logger

import com.github.bakenezumi.grpccli.ServerReflectionGrpc.ServerReflectionStub
import com.google.protobuf.DescriptorProtos.FileDescriptorProto
import io.grpc.reflection.v1alpha.{
  ServerReflectionRequest,
  ServerReflectionResponse
}
import io.grpc.stub.StreamObserver
import io.grpc.{ManagedChannel, ManagedChannelBuilder}

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future, Promise}

object GrpcClient {
  def apply(host: String, port: Int)(
      implicit executorContext: ExecutionContext): GrpcClient = {
    val channel =
      ManagedChannelBuilder.forAddress(host, port).usePlaintext(true).build
    val asyncStub = ServerReflectionGrpc.stub(channel)
    new GrpcClient(channel, asyncStub)
  }

  def main(args: Array[String]): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val client = GrpcClient("localhost", 50051)
    try {
//      val future = client.getServiceList(service, ServiceListFormat.SHORT)
//      val future = client.getType(service)

      //val tpe = args.headOption.getOrElse("helloworld.HelloRequest")
      val tpe = args.headOption.getOrElse(
        "grpc.reflection.v1alpha.ServerReflectionResponse")

      val future = client.getType(tpe)

      val ret = Await.result(future, 5 second)
      println(ret)
    } finally {
      client.shutdown()
    }
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

  private def callServer[RETURN](
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
    val serviceNamesFuture = callServer(
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
              if (serviceNameParameter.isEmpty || serviceNameParameter
                    .endsWith(serviceDescriptor.getName))
                Some(
                  format match {
                    case ServiceListFormat.SHORT =>
                      serviceDescriptor.getMethodList.asScala.toList
                        .map(_.getName)
                        .mkString("\n")
                    case ServiceListFormat.LONG =>
                      s"""|filename: "$file"
                          |package: "$pkg"
                          |$serviceDescriptor""".stripMargin // TODO: to protobuf format
                  }
                )
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
          file =>
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
    callServer(
      ServerReflectionRequest.newBuilder.setFileContainingSymbol(symbol).build,
      _.getFileDescriptorResponse.getFileDescriptorProtoList.asScala
        .map(FileDescriptorProto.parseFrom)
    )
  }

}

sealed trait ServiceListFormat
object ServiceListFormat {
  object SHORT extends ServiceListFormat
  object LONG extends ServiceListFormat
}
