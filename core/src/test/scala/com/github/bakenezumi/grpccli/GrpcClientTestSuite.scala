package com.github.bakenezumi.grpccli

import java.io.InputStream

import org.scalatest.{AsyncFunSuite, BeforeAndAfterAll}

import scala.collection.mutable.ArrayBuffer

// To run this test, please use server reflection of
// https://github.com/grpc/grpc-java/blob/master/examples/src/main/java/io/grpc/examples/helloworld/HelloWorldServer.java
// You need to enable it and start it up in advance.
class GrpcClientTestSuite extends AsyncFunSuite with BeforeAndAfterAll {
  val client = GrpcClient("localhost", 50051)

  override def afterAll(): Unit = {
    client.shutdown()
    System.setIn(null)
  }

  test("ls") {
    val service = "helloworld.Greeter.SayHello"
    val future = client.getServiceList(service)
    future.map(ret => assert(ret == Seq("SayHello")))
  }

  test("ls -l") {
    val service = "grpc.reflection.v1alpha.ServerReflection"
    val future = client.getServiceList(service, format = ServiceListFormat.LONG)
    future.map(ret =>
      assert(ret == Seq("""|filename: io/grpc/reflection/v1alpha/reflection.proto
                           |package: grpc.reflection.v1alpha;
                           |service ServerReflection {
                           |  rpc ServerReflectionInfo(stream grpc.reflection.v1alpha.ServerReflectionRequest) returns (stream grpc.reflection.v1alpha.ServerReflectionResponse) {}
                           |}
                           |""".stripMargin)))
  }

  test("ls -l method") {
    val service =
      "grpc.reflection.v1alpha.ServerReflection.ServerReflectionInfo"
    val future = client.getServiceList(service, format = ServiceListFormat.LONG)
    future.map(ret =>
      assert(ret == Seq("""|  rpc ServerReflectionInfo(stream grpc.reflection.v1alpha.ServerReflectionRequest) returns (stream grpc.reflection.v1alpha.ServerReflectionResponse) {}
                           |""".stripMargin)))
  }

  test("type") {
    val tpe = "helloworld.HelloRequest"
    val future = client.getType(tpe)
    future.map(
      ret =>
        assert(ret.map(_.replace("\r", "")) ==
          Seq("""|message HelloRequest {
                 |  string name = 1[json_name = "name"];
                 |}
                 |""".stripMargin.replace("\r", ""))))
  }

  test("type has one of field") {
    val tpe = "grpc.reflection.v1alpha.ServerReflectionResponse"
    val future = client.getType(tpe)
    future.map(
      ret =>
        assert(ret.map(_.replace("\r", "")) ==
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
                 |""".stripMargin.replace("\r", ""))))
  }

  test("call") {
    System.setIn(new MockStandartInputStream("name:foo"))
    val service = "helloworld.Greeter.SayHello"
    val future = client.callDynamic(service)
    future.onComplete { _ =>
      System.setIn(null)
    }
    future.map(ret => assert(ret == ()))
  }

}

class MockStandartInputStream(mockString: String) extends InputStream {
  var buffer = new StringBuilder()
  buffer.append(mockString)
  override def read(): Int = {
    if (buffer.isEmpty) return -1
    val result = buffer.charAt(0)
    buffer.deleteCharAt(0)
    result
  }
}
