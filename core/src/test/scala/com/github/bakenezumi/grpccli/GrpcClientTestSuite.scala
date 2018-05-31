package com.github.bakenezumi.grpccli

import com.github.bakenezumi.grpccli.testing.TestServer
import org.scalatest.{AsyncFunSuite, BeforeAndAfterAll}

import scala.concurrent.ExecutionContext

class GrpcClientTestSuite extends AsyncFunSuite with BeforeAndAfterAll {
  private[this] val port = 50051
  private[this] lazy val server =
    new TestServer(port, ExecutionContext.global, None)
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
