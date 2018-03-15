package com.github.bakenezumi.grpccli.service

import com.github.bakenezumi.grpccli.GrpcClient
import com.github.bakenezumi.grpccli.protobuf.{ProtoMethodName, ServiceResolver}
import org.scalatest.FunSuite
import sbt.internal.util.complete.Parser

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}

class TypeFieldParsersTestSuite extends FunSuite {
  import scala.concurrent.ExecutionContext.Implicits.global
  private[this] val fileDescriptorSet = Await.result(
    GrpcClient
      .apply("localhost", 50051)
      .getAllInOneFileDescriptorProtoSet,
    Duration(5, SECONDS))

  private[this] val methodName = "helloworld.Greeter/SayHello"
  private[this] val methodDescriptor =
    ServiceResolver
      .fromFileDescriptorSet(fileDescriptorSet)
      .resolveServiceMethod(ProtoMethodName.parseFullGrpcMethodName(methodName))

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
