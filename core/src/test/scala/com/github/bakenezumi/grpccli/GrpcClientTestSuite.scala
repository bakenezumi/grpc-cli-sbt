package com.github.bakenezumi.grpccli

import java.io.InputStream

import org.scalatest.AsyncFunSuite

import scala.collection.mutable.ArrayBuffer

// To run this test, please use server reflection of
// https://github.com/grpc/grpc-java/blob/master/examples/src/main/java/io/grpc/examples/helloworld/HelloWorldServer.java
// You need to enable it and start it up in advance.
class GrpcClientTestSuite extends AsyncFunSuite {
  test("ls") {
    val client = GrpcClient("localhost", 50051)
    val service = "helloworld.Greeter.SayHello"
    val future = client.getServiceList(service)
    future.onComplete(_ => client.shutdown())
    future.map(ret => assert(ret == Seq("SayHello")))
  }

  test("type") {
    val client = GrpcClient("localhost", 50051)
    val tpe = "helloworld.HelloRequest"
    val future = client.getType(tpe)
    future.onComplete(_ => client.shutdown())
    future.map(
      ret =>
        assert(ret.map(_.replace("\r", "")) ==
          Seq("""|name: "HelloRequest"
                 |field {
                 |  name: "name"
                 |  number: 1
                 |  label: LABEL_OPTIONAL
                 |  type: TYPE_STRING
                 |  json_name: "name"
                 |}
                 |""".stripMargin.replace("\r", ""))))
  }

  test("call") {
    System.setIn(new MockStandartInputStream("name:foo"))
    val client = GrpcClient("localhost", 50051)
    val service = "helloworld.Greeter.SayHello"
    val future = client.callDynamic(service)
    future.onComplete { _ =>
      client.shutdown()
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
