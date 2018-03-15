package com.github.bakenezumi.grpccli.service

import com.github.bakenezumi.grpccli.{GrpcClient, ServiceListFormat}
import org.scalatest.{BeforeAndAfterAll, FunSuite}

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}

// To run this test, please use server reflection of
// https://github.com/grpc/grpc-java/blob/master/examples/src/main/java/io/grpc/examples/helloworld/HelloWorldServer.java
// You need to enable it and start it up in advance.
class LsServiceTestSuite extends FunSuite with BeforeAndAfterAll {
  import scala.concurrent.ExecutionContext.Implicits.global
  private[this] val fileDescriptorSet = Await.result(
    GrpcClient
      .apply("localhost", 50051)
      .getAllInOneFileDescriptorProtoSet,
    Duration(5, SECONDS))

  override def afterAll(): Unit = {
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
