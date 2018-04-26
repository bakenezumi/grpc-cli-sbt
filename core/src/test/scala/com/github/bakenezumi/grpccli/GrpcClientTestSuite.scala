package com.github.bakenezumi.grpccli

import io.grpc.examples.helloworld.HelloWorldServer
import org.scalatest.{AsyncFunSuite, BeforeAndAfterAll}

import scala.concurrent.ExecutionContext

class GrpcClientTestSuite extends AsyncFunSuite with BeforeAndAfterAll {
  private[this] val port = 50051
  private[this] lazy val server =
    new HelloWorldServer(port, ExecutionContext.global)
  private[this] lazy val client = GrpcClient("localhost", port)

  override def beforeAll: Unit = {
    server.start()
  }

  override def afterAll: Unit = {
    server.stop()
  }

  test("getAllInOneFileDescriptorProtoSet") {

    val future = client.getAllInOneFileDescriptorProtoSet

    future.map(ret => assert(ret != null))
  }

}
