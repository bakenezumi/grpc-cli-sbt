package com.github.bakenezumi.grpccli

import org.scalatest.{AsyncFunSuite, BeforeAndAfterAll}

// To run this test, please use server reflection of
// https://github.com/grpc/grpc-java/blob/master/examples/src/main/java/io/grpc/examples/helloworld/HelloWorldServer.java
// You need to enable it and start it up in advance.
class GrpcClientTestSuite extends AsyncFunSuite with BeforeAndAfterAll {
  val client = GrpcClient("localhost", 50051)

  test("getAllInOneFileDescriptorProtoSet") {

    val future = client.getAllInOneFileDescriptorProtoSet

    future.map(ret => assert(ret != null))
  }

}
