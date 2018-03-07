package com.github.bakenezumi.grpccli

import sbt.Help

import sbt.internal.util.complete.DefaultParsers._

object GrpcCliCommandParser {
  val NotHyphen = charClass(c => c != '-' && c != ' ')

  val NotFlag = (Space ~> NotHyphen ~ NotSpace).map {
    case (head, tail) => head + tail
  }

  lazy val grpcCliCommand = Space ~> (ls | ls_l | tpe | call)

  lazy val ls =
    (token("ls") ~> (OptSpace || NotFlag) <~ OptSpace)
      .map {
        case Left(_)        => LsCommand()
        case Right(service) => LsCommand(service)
      }

  lazy val ls_l =
    (
      (token("ls") ~> Space ~> token("-l") ~> (OptSpace || NotFlag)) |
        (token("ls") ~> (OptSpace || NotFlag) <~ Space <~ token("-l") <~ OptSpace)
    ).map {
      case Left(_)        => LsCommand("", ServiceListFormat.LONG)
      case Right(service) => LsCommand(service, ServiceListFormat.LONG)
    }

  lazy val tpe =
    (token("type") ~> NotFlag)
      .map(tpe_ => TypeCommand(tpe_))

  lazy val call =
    (token("call") ~> NotFlag)
      .map(service => CallCommand(service))

  val help =
    Help.briefOnly(
      Seq(("grpc-cli ls ...", "List services"),
          ("grpc_cli call ...", "Call method"),
          ("grpc_cli type ...", "Print type")))
}
