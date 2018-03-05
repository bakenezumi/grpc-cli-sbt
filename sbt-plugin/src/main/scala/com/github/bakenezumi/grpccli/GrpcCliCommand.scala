package com.github.bakenezumi.grpccli

import scala.concurrent.Await
import scala.concurrent.duration._
sealed trait GrpcCliCommand

import scala.concurrent.ExecutionContext.Implicits.global

case class LsCommand(method: String = "",
                     format: ServiceListFormat = ServiceListFormat.SHORT)
    extends GrpcCliCommand {
  def apply(address: String): Seq[String] = {
    val Array(host: String, port: String) = address.split(":")
    val future = GrpcClient(host, port.toInt).getServiceList(method, format)
    Await.result(future, 5 second)
  }
}

case class TypeCommand(typeName: String) extends GrpcCliCommand {
  def apply(address: String): Seq[String] = {
    val Array(host: String, port: String) = address.split(":")
    val future = GrpcClient(host, port.toInt).getType(typeName)
    Await.result(future, 5 second)
  }
}
