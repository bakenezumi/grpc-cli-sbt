package com.github.bakenezumi.grpccli

import org.scalatest.FunSuite
import sbt.internal.util.complete.Parser

import GrpcCliCommandParser._

class GrpcCliCommandParserTestSuite extends FunSuite {

  test("parse ls") {
    assert(Parser.parse(" ls", grpcCliCommand) == Right(LsCommand()))
    assert(Parser.parse("  ls", grpcCliCommand) == Right(LsCommand()))
    assert(Parser.parse("  ls ", grpcCliCommand) == Right(LsCommand()))
    assert(
      Parser.parse(" ls foo.Bar", grpcCliCommand) == Right(
        LsCommand("foo.Bar")))
    assert(
      Parser.parse(" ls   foo.Bar", grpcCliCommand) == Right(
        LsCommand("foo.Bar")))
    assert(
      Parser.parse(" ls   foo.Bar ", grpcCliCommand) == Right(
        LsCommand("foo.Bar")))
  }

  test("parse ls -l") {
    assert(
      Parser.parse(" ls -l", grpcCliCommand) == Right(
        LsCommand("", ServiceListFormat.LONG)))
    assert(
      Parser.parse(" ls  -l", grpcCliCommand) == Right(
        LsCommand("", ServiceListFormat.LONG)))
    assert(
      Parser.parse(" ls -l ", grpcCliCommand) == Right(
        LsCommand("", ServiceListFormat.LONG)))
    assert(
      Parser.parse(" ls -l foo.Bar", grpcCliCommand) == Right(
        LsCommand("foo.Bar", ServiceListFormat.LONG)))
    assert(
      Parser.parse(" ls foo.Bar -l", grpcCliCommand) == Right(
        LsCommand("foo.Bar", ServiceListFormat.LONG)))
    assert(
      Parser.parse(" ls  foo.Bar -l", grpcCliCommand) == Right(
        LsCommand("foo.Bar", ServiceListFormat.LONG)))
  }

  test(" type") {
    assert(
      Parser.parse(" type foo.Bar", grpcCliCommand) == Right(
        TypeCommand("foo.Bar")))
  }

}
