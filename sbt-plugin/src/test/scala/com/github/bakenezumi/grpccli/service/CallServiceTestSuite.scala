package com.github.bakenezumi.grpccli.service

import java.io.InputStream

import com.github.bakenezumi.grpccli.{GrpcCliCommandParser, GrpcClient}
import com.google.protobuf.DescriptorProtos
import org.scalatest.{BeforeAndAfterAll, FunSuite}
import sbt.internal.util.FullReader
import sbt.util.{Level, Logger}

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}

// To run this test, please use server reflection of
// https://github.com/grpc/grpc-java/blob/master/examples/src/main/java/io/grpc/examples/helloworld/HelloWorldServer.java
// You need to enable it and start it up in advance.
class CallServiceTestSuite extends FunSuite with BeforeAndAfterAll {
  import scala.concurrent.ExecutionContext.Implicits.global
  private[this] val fileDescriptorSet = Await.result(
    GrpcClient
      .apply("localhost", 50051)
      .getAllInOneFileDescriptorProtoSet,
    Duration(5, SECONDS))

  override def afterAll(): Unit = {
    System.setIn(null)
  }

  test("call") {
    System.setIn(new MockStandardInputStream("name:foo"))
    val service = "helloworld.Greeter.SayHello"
    CallService.call(service,
                     "localhost:50051",
                     fileDescriptorSet,
                     MessageReader.forStdin,
                     MockLogger)
  }

  object parser extends GrpcCliCommandParser {
    override val serviceList: Seq[String] = Nil
    override val methodList: Seq[String] = Nil
    override val fileDescriptorSet: DescriptorProtos.FileDescriptorSet = null
  }

}

class MockStandardInputStream(mockString: String) extends InputStream {
  val buffer = new StringBuilder()
  buffer.append(mockString)
  override def read(): Int = {
    if (buffer.isEmpty) return -1
    val result = buffer.charAt(0)
    buffer.deleteCharAt(0)
    result
  }
}

object MockLogger extends Logger {
  override def trace(t: => Throwable): Unit = ()

  override def success(message: => String): Unit = ()

  override def log(level: Level.Value, message: => String): Unit = ()
}
