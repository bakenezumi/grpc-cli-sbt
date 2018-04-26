package com.github.bakenezumi.grpccli.service

import com.github.bakenezumi.grpccli.GrpcClient
import com.github.bakenezumi.grpccli.protobuf.{ProtoMethodName, ServiceResolver}
import io.grpc.examples.helloworld.HelloWorldServer
import org.scalatest.{BeforeAndAfterAll, FunSuite}
import sbt.internal.util.complete.Parser

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.{Duration, SECONDS}

class TypeFieldParsersTestSuite extends FunSuite with BeforeAndAfterAll {
  import scala.concurrent.ExecutionContext.Implicits.global
  private[this] val port = 50051
  private[this] lazy val server =
    new HelloWorldServer(port, ExecutionContext.global)
  private[this] lazy val fileDescriptorSet = Await.result(
    GrpcClient
      .apply("localhost", port)
      .getAllInOneFileDescriptorProtoSet,
    Duration(5, SECONDS))
  private[this] val methodName = "helloworld.Greeter/SayHello"
  private[this] lazy val methodDescriptor =
    ServiceResolver
      .fromFileDescriptorSet(fileDescriptorSet)
      .resolveServiceMethod(ProtoMethodName.parseFullGrpcMethodName(methodName))

  override def beforeAll: Unit = {
    server.start()
  }

  override def afterAll(): Unit = {
    server.stop()
    System.setIn(null)
  }

  test("Field") {

    val parser = TypeFieldParsers(methodDescriptor.getInputType, Nil)
    assert(
      Parser.parse("name: foo", parser.Field) == Right(
        Parsed.Field("name", Seq(Parsed.Value(Some("foo"))), false)))
    assert(
      Parser.parse("name:foo", parser.Field) == Right(
        Parsed.Field("name", Seq(Parsed.Value(Some("foo"))), false)))
    assert(
      Parser.parse("name:", parser.Field) == Right(Parsed
        .Field("name", Seq(Parsed.Value(None)), false)))
    assert(Parser.parse("name foo", parser.Field).isLeft)

  }

  test("FieldName") {

    val parser = TypeFieldParsers(methodDescriptor.getInputType, Nil)
    assert(
      Parser.parse("name:foo", TypeFieldParsers.FieldName) == Right("name"))

  }

}
