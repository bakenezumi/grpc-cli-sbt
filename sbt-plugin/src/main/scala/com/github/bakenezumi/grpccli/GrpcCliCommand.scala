package com.github.bakenezumi.grpccli

import com.google.protobuf.DescriptorProtos.FileDescriptorSet
import sbt.util.Logger

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
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
    LsService(fileDescriptorSet, method, format)
}

case class TypeCommand(fileDescriptorSet: FileDescriptorSet, typeName: String)
    extends GrpcCliCommand {
  def apply: Seq[String] =
    TypeService(fileDescriptorSet, typeName)
}

case class CallCommand(method: String) extends GrpcCliCommand {
  def apply(address: String,
            fileDescriptorSet: FileDescriptorSet,
            logger: Logger): Unit = {
    using(address) { client =>
      Await.result(client.callDynamic(fileDescriptorSet, method),
                   Duration(5, SECONDS))
    }
  }
}
