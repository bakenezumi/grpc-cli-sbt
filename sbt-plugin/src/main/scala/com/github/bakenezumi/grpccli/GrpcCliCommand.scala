package com.github.bakenezumi.grpccli

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

sealed trait GrpcCliCommand {
  def using[T](address: String)(f: GrpcClient => T): T = {
    val Array(host: String, port: String) = address.split(":")
    val client = GrpcClient(host, port.toInt)
    try { f(client) } finally {
      client.shutdown()
    }
  }
}

case class LsCommand(method: String = "",
                     format: ServiceListFormat = ServiceListFormat.SHORT)
    extends GrpcCliCommand {
  def apply(address: String): Seq[String] = using(address) { client =>
    val future = client.getServiceList(method, format)
    Await.result(future, 5 second)
  }
}

case class TypeCommand(typeName: String) extends GrpcCliCommand {
  def apply(address: String): Seq[String] = using(address) { client =>
    val future = client.getType(typeName)
    Await.result(future, 5 second)
  }
}

case class CallCommand(method: String) extends GrpcCliCommand {
  def apply(address: String): Unit = using(address) { client =>
    val future = client.callDynamic(method)
    Await.result(future, 5 second)
  }
}
