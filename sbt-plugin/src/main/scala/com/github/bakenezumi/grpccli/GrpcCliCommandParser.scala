package com.github.bakenezumi.grpccli

import com.github.bakenezumi.grpccli.protobuf.PrintedTypeNameCache
import com.google.protobuf.DescriptorProtos.FileDescriptorSet
import sbt.internal.util.complete.DefaultParsers._

trait GrpcCliCommandParser {

  // Implementation required
  val serviceList: Seq[String]
  val methodList: Seq[String]
  val fileDescriptorSet: FileDescriptorSet

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
    (token("ls") ~> token(Space ~> "-l").? ~ OptService ~ token(Space ~> "-l").? <~ OptSpace)
      .map {
        case ((None, None), None) => LsCommand(fileDescriptorSet)
        case ((None, Some(service)), None) =>
          LsCommand(fileDescriptorSet, service)
        case ((Some(_), None), _) =>
          LsCommand(fileDescriptorSet, "", ServiceListFormat.LONG)
        case ((Some(_), Some(service)), _) =>
          LsCommand(fileDescriptorSet, service, ServiceListFormat.LONG)
        case ((_, None), Some(_)) =>
          LsCommand(fileDescriptorSet, "", ServiceListFormat.LONG)
        case ((_, Some(service)), Some(_)) =>
          LsCommand(fileDescriptorSet, service, ServiceListFormat.LONG)
      }

  lazy val PrintedType = {
    NotFlag.examples(PrintedTypeNameCache.getAll: _*)
  }

  lazy val tpe =
    (token("type") ~> Space ~> PrintedType)
      .map(tpe_ => TypeCommand(fileDescriptorSet, tpe_))

  lazy val call =
    (token("call") ~> Space ~> (NotFlag | KnownMethod))
      .map(service => CallCommand(service))

}
