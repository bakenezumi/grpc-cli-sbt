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
    val client = GrpcClient(host, port.toInt)
    try {
      val future = client.getServiceList(method, format)
      Await.result(future, 5 second)
    } finally {
      client.shutdown()
    }
  }
}

case class TypeCommand(typeName: String) extends GrpcCliCommand {
  def apply(address: String): Seq[String] = {
    val Array(host: String, port: String) = address.split(":")
    val client = GrpcClient(host, port.toInt)
    try {
      val future = client.getType(typeName)
      Await.result(future, 5 second)
    } finally {
      client.shutdown()
    }
  }
}

case class CallCommand(method: String) extends GrpcCliCommand {
  def apply(address: String): Unit = {
    val Array(host: String, port: String) = address.split(":")
    val client = GrpcClient(host, port.toInt)
    try {
      val future = client.callDynamic(method)
      Await.result(future, 5 second)
    } finally {
      client.shutdown()
    }

  }
}
