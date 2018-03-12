package com.github.bakenezumi.grpccli

import java.io.InputStream

import org.scalatest.{AsyncFunSuite, BeforeAndAfterAll}

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}

// To run this test, please use server reflection of
// https://github.com/grpc/grpc-java/blob/master/examples/src/main/java/io/grpc/examples/helloworld/HelloWorldServer.java
// You need to enable it and start it up in advance.
class GrpcClientTestSuite extends AsyncFunSuite with BeforeAndAfterAll {
  val client = GrpcClient("localhost", 50051)

  override def afterAll(): Unit = {
    client.shutdown()
    System.setIn(null)
  }

  test("call") {
    System.setIn(new MockStandartInputStream("name:foo"))
    val service = "helloworld.Greeter.SayHello"

    val fileDescriptorProtoSet =
      Await.result(client.getFileDescriptorProtoSet(service),
                   Duration(5, SECONDS))
    val future = client.callDynamic(fileDescriptorProtoSet, service)

    future.onComplete { _ =>
      System.setIn(null)
    }
    future.map(_ => succeed)
  }

  test("getAllInOneFileDescriptorProtoSet") {

    val future = client.getAllInOneFileDescriptorProtoSet

    future.map(ret => assert(ret != null))
  }

}

class MockStandartInputStream(mockString: String) extends InputStream {
  val buffer = new StringBuilder()
  buffer.append(mockString)
  override def read(): Int = {
    if (buffer.isEmpty) return -1
    val result = buffer.charAt(0)
    buffer.deleteCharAt(0)
    result
  }
}
