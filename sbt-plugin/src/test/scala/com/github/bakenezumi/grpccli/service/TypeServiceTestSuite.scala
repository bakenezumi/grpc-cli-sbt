package com.github.bakenezumi.grpccli.service

import com.github.bakenezumi.grpccli.GrpcClient
import org.scalatest.FunSuite

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}

// To run this test, please use server reflection of
// https://github.com/grpc/grpc-java/blob/master/examples/src/main/java/io/grpc/examples/helloworld/HelloWorldServer.java
// You need to enable it and start it up in advance.
class TypeServiceTestSuite extends FunSuite {
  import scala.concurrent.ExecutionContext.Implicits.global
  private[this] val fileDescriptorSet = Await.result(
    GrpcClient
      .apply("localhost", 50051)
      .getAllInOneFileDescriptorProtoSet,
    Duration(5, SECONDS))

  test("type") {
    val tpe = "helloworld.HelloRequest"
    assert(
      TypeService.getType(fileDescriptorSet, tpe).map(_.replace("\r", "")) ==
        Seq("""|message HelloRequest {
               |  string name = 1[json_name = "name"];
               |}
               |""".stripMargin.replace("\r", "")))
  }

  test("type has one of field") {
    val tpe = "grpc.reflection.v1alpha.ServerReflectionResponse"

    assert(
      TypeService.getType(fileDescriptorSet, tpe).map(_.replace("\r", "")) ==
        Seq("""|message ServerReflectionResponse {
               |  string valid_host = 1;
               |  .grpc.reflection.v1alpha.ServerReflectionRequest original_request = 2;
               |  oneof message_response {
               |    .grpc.reflection.v1alpha.FileDescriptorResponse file_descriptor_response = 4;
               |    .grpc.reflection.v1alpha.ExtensionNumberResponse all_extension_numbers_response = 5;
               |    .grpc.reflection.v1alpha.ListServiceResponse list_services_response = 6;
               |    .grpc.reflection.v1alpha.ErrorResponse error_response = 7;
               |  }
               |}
               |""".stripMargin.replace("\r", "")))
  }

  test("type not found") {
    val tpe = "not.found"
    assert(TypeService.getType(fileDescriptorSet, tpe) == Nil)
  }

}
