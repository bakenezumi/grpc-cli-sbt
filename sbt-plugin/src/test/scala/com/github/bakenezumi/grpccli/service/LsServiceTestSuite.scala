package com.github.bakenezumi.grpccli.service

import com.github.bakenezumi.grpccli.{GrpcClient, ServiceListFormat}
import io.grpc.examples.helloworld.HelloWorldServer
import org.scalatest.{BeforeAndAfterAll, FunSuite}

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.{Duration, SECONDS}

class LsServiceTestSuite extends FunSuite with BeforeAndAfterAll {
  import scala.concurrent.ExecutionContext.Implicits.global
  private[this] val port = 50051
  private[this] lazy val server =
    new HelloWorldServer(port, ExecutionContext.global)
  private[this] lazy val fileDescriptorSet = Await.result(
    GrpcClient
      .apply("localhost", port)
      .getAllInOneFileDescriptorProtoSet,
    Duration(5, SECONDS))

  override def beforeAll: Unit = {
    server.start()
  }

  override def afterAll(): Unit = {
    server.stop()
    System.setIn(null)
  }

  test("ls") {
    val service = "helloworld.Greeter.SayHello"
    assert(LsService.getList(fileDescriptorSet, service) == Seq("SayHello"))
  }

  test("ls not found") {
    val service = "not.found"
    assert(LsService.getList(fileDescriptorSet, service) == Nil)

  }

  test("ls -l") {
    val service = "grpc.reflection.v1alpha.ServerReflection"
    assert(
      LsService.getList(fileDescriptorSet,
                        service,
                        format = ServiceListFormat.LONG) == Seq(
        """|filename: io/grpc/reflection/v1alpha/reflection.proto
           |package: grpc.reflection.v1alpha;
           |service ServerReflection {
           |  rpc ServerReflectionInfo(stream grpc.reflection.v1alpha.ServerReflectionRequest) returns (stream grpc.reflection.v1alpha.ServerReflectionResponse) {}
           |}
           |""".stripMargin))
  }

  test("ls -l method") {
    val service =
      "grpc.reflection.v1alpha.ServerReflection.ServerReflectionInfo"
    assert(
      LsService.getList(fileDescriptorSet,
                        service,
                        format = ServiceListFormat.LONG) == Seq(
        """|  rpc ServerReflectionInfo(stream grpc.reflection.v1alpha.ServerReflectionRequest) returns (stream grpc.reflection.v1alpha.ServerReflectionResponse) {}
           |""".stripMargin))
  }

}
