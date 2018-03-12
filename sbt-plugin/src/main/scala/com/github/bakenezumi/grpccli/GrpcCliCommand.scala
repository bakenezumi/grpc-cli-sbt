package com.github.bakenezumi.grpccli

import java.nio.file.Path

import com.github.bakenezumi.grpccli.protobuf.ProtocInvoker
import com.google.common.base.Throwables
import com.google.protobuf.DescriptorProtos.FileDescriptorSet
import io.grpc.{Status, StatusRuntimeException}
import sbt.util.Logger

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
sealed trait GrpcCliCommand {

  def using[T](address: String)(f: GrpcClient => T): T = {
    val Array(host: String, port: String) = address.split(":")
    val client = GrpcClient(host, port.toInt)
    try { f(client) } finally {
      client.shutdown()
    }
  }
}

case class LsCommand(fileDescriptorSet: FileDescriptorSet,
                     method: String = "",
                     format: ServiceListFormat = ServiceListFormat.SHORT)
    extends GrpcCliCommand {
  def apply: Seq[String] =
    ServiceList.listServices(fileDescriptorSet, method, format)
}

case class TypeCommand(typeName: String) extends GrpcCliCommand {
  def apply(address: String): Seq[String] = using(address) { client =>
    val future = client.getType(typeName)
    Await.result(future, Duration(5, SECONDS))
  }
}

case class CallCommand(method: String) extends GrpcCliCommand {
  def apply(address: String,
            useReflection: Boolean,
            protoSources: Seq[Path],
            logger: Logger): Unit = {
    using(address) { client =>
      val fileDescriptorSet =
        (if (useReflection) {
           logger.info("Using proto descriptors fetched by reflection")
           Try {
             Await.result(client.getFileDescriptorProtoSet(method),
                          Duration(5, SECONDS))
           } match {
             case Success(ret) =>
               Some(ret)
             case Failure(t) =>
               Throwables.getRootCause(t) match {
                 case root: StatusRuntimeException
                     if root.getStatus.getCode == Status.Code.UNIMPLEMENTED =>
                   logger.warn(
                     "Could not list services because the remote host does not support " +
                       "reflection. To disable resolving services by reflection, either pass the flag " +
                       "--use_reflection=false or disable reflection in your config file.")
                   None
                 case _ => throw t
               }
           }
         } else {
           None
         }).getOrElse {
          logger.info("Using proto descriptors obtained from protoc")
          val invoker = new ProtocInvoker(protoSources.head, protoSources.tail)
          invoker.invoke
        }
      if (fileDescriptorSet.getFileCount < 1)
        logger.warn(s"Service or method $method not found.")
      else {
        Await.result(client.callDynamic(fileDescriptorSet, method),
                     Duration(5, SECONDS))
      }

    }
  }
}
