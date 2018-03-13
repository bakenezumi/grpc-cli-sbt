package com.github.bakenezumi.grpccli

import com.github.bakenezumi.grpccli.service.{
  CallService,
  LsService,
  TypeService
}
import com.google.protobuf.DescriptorProtos.FileDescriptorSet
import sbt.util.Logger

import scala.concurrent.ExecutionContext.Implicits.global

sealed trait GrpcCliCommand
case class LsCommand(fileDescriptorSet: FileDescriptorSet,
                     method: String = "",
                     format: ServiceListFormat = ServiceListFormat.SHORT)
    extends GrpcCliCommand {
  def apply: Seq[String] =
    LsService(fileDescriptorSet, method, format)
}

case class TypeCommand(fileDescriptorSet: FileDescriptorSet, typeName: String)
    extends GrpcCliCommand {
  def apply: Seq[String] =
    TypeService(fileDescriptorSet, typeName)
}

case class CallCommand(method: String) extends GrpcCliCommand {
  def apply(address: String,
            fileDescriptorSet: FileDescriptorSet,
            logger: Logger): Unit =
    CallService.apply(method, address, fileDescriptorSet, logger)
}
