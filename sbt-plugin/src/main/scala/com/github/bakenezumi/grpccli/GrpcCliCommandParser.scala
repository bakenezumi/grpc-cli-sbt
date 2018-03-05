package com.github.bakenezumi.grpccli

import sbt.Help

import sbt.internal.util.complete.DefaultParsers._

object GrpcCliCommandParser {
  val NotHyphen = charClass(c => c != '-' && c != ' ')

  val NotFlag = Space ~> NotHyphen ~ NotSpace

  lazy val grpcCliCommand = Space ~> (ls | ls_l | tpe)

  lazy val ls =
    (token("ls") ~> (OptSpace || NotFlag) <~ OptSpace)
      .map {
        case Left(_)             => LsCommand()
        case Right((head, tail)) => LsCommand(head + tail)
      }

  lazy val ls_l =
    (
      (token("ls") ~> Space ~> token("-l") ~> (OptSpace || NotFlag)) |
        (token("ls") ~> (OptSpace || NotFlag) <~ Space <~ token("-l") <~ OptSpace)
    ).map {
      case Left(_)             => LsCommand("", ServiceListFormat.LONG)
      case Right((head, tail)) => LsCommand(head + tail, ServiceListFormat.LONG)
    }

  lazy val tpe =
    (token("type") ~> NotFlag)
      .map {
        case (head, tail) => TypeCommand(head + tail)
      }

  val help =
    Help.briefOnly(
      Seq(("grpc-cli ls ...", "List services"),
          ("grpc_cli type ...", "Print type")))
}
