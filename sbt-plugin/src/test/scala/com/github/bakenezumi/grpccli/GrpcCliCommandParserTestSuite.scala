package com.github.bakenezumi.grpccli

import com.google.protobuf.DescriptorProtos
import org.scalatest.FunSuite
import sbt.internal.util.complete.Parser

class GrpcCliCommandParserTestSuite extends FunSuite {
  object parser extends GrpcCliCommandParser {
    override val serviceList: Seq[String] = Nil
    override val methodList: Seq[String] = Nil
    override val fileDescriptorSet: DescriptorProtos.FileDescriptorSet = null
  }

  test("parse ls") {
    assert(Parser.parse(" ls", parser.GrpcCliCommand) == Right(LsCommand(null)))
    assert(
      Parser.parse("  ls", parser.GrpcCliCommand) == Right(LsCommand(null)))
    assert(
      Parser.parse("  ls ", parser.GrpcCliCommand) == Right(LsCommand(null)))
    assert(
      Parser.parse(" ls foo.Bar", parser.GrpcCliCommand) == Right(
        LsCommand(null, "foo.Bar")))
    assert(
      Parser.parse(" ls   foo.Bar", parser.GrpcCliCommand) == Right(
        LsCommand(null, "foo.Bar")))
  }

  test("parse ls -l") {
    assert(
      Parser.parse(" ls -l", parser.GrpcCliCommand) == Right(
        LsCommand(null, "", ServiceListFormat.LONG)))
    assert(
      Parser.parse(" ls  -l", parser.GrpcCliCommand) == Right(
        LsCommand(null, "", ServiceListFormat.LONG)))
    assert(
      Parser.parse(" ls -l ", parser.GrpcCliCommand) == Right(
        LsCommand(null, "", ServiceListFormat.LONG)))
    assert(
      Parser.parse(" ls -l foo.Bar", parser.GrpcCliCommand) == Right(
        LsCommand(null, "foo.Bar", ServiceListFormat.LONG)))
    assert(
      Parser.parse(" ls foo.Bar -l", parser.GrpcCliCommand) == Right(
        LsCommand(null, "foo.Bar", ServiceListFormat.LONG)))
    assert(
      Parser.parse(" ls  foo.Bar -l", parser.GrpcCliCommand) == Right(
        LsCommand(null, "foo.Bar", ServiceListFormat.LONG)))
  }

  test(" type") {
    assert(
      Parser.parse(" type foo.Bar", parser.GrpcCliCommand) == Right(
        TypeCommand(null, "foo.Bar")))
  }

}
