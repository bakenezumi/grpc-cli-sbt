package com.github.bakenezumi.grpccli

import com.github.bakenezumi.grpccli.protobuf.PrintedTypeNameCache
import sbt.internal.util.complete.DefaultParsers._

trait GrpcCliCommandParser {

  // Implementation required
  val serviceList: Seq[String]
  val methodList: Seq[String]

  lazy val NotHyphen = charClass(c => c != '-' && !c.isWhitespace)

  lazy val NotFlag = (NotHyphen ~ NotSpace).map {
    case (head, tail) => head + tail
  }

  lazy val GrpcCliCommand = Space ~> (ls | tpe | call)

  lazy val KnownService = {
    NotFlag.examples(serviceList ++ methodList: _*)
  }

  lazy val KnownMethod = {
    NotFlag.examples(methodList: _*)
  }

  lazy val OptService = (Space ~> KnownService).?

  lazy val ls =
    (token("ls") ~> token(Space ~> "-l").? ~ OptService ~ token(Space ~> "-l").?)
      .map {
        case ((None, None), None)          => LsCommand()
        case ((None, Some(service)), None) => LsCommand(service)
        case ((Some(_), None), _)          => LsCommand("", ServiceListFormat.LONG)
        case ((Some(_), Some(service)), _) =>
          LsCommand(service, ServiceListFormat.LONG)
        case ((_, None), Some(_)) => LsCommand("", ServiceListFormat.LONG)
        case ((_, Some(service)), Some(_)) =>
          LsCommand(service, ServiceListFormat.LONG)
      }

  lazy val PrintedType = {
    NotFlag.examples(PrintedTypeNameCache.getAll: _*)
  }

  lazy val tpe =
    (token("type") ~> Space ~> PrintedType)
      .map(tpe_ => TypeCommand(tpe_))

  lazy val call =
    (token("call") ~> Space ~> (NotFlag | KnownMethod))
      .map(service => CallCommand(service))

}
