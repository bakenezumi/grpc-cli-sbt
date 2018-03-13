package com.github.bakenezumi.grpccli.service

import java.io.File

import com.github.bakenezumi.grpccli.GrpcClient
import com.github.bakenezumi.grpccli.protobuf.ProtocInvoker
import com.google.common.base.Throwables
import com.google.protobuf.DescriptorProtos.FileDescriptorSet
import io.grpc.{Status, StatusRuntimeException}
import sbt.util.Logger

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}
import scala.util.{Failure, Success, Try}

object InitService {
  def getFileDescriptorSet(useReflection: Boolean,
                           endpoint: String,
                           protoSources: Seq[File],
                           logger: Logger): FileDescriptorSet = {
    (if (useReflection) {
       import scala.concurrent.ExecutionContext.Implicits.global
       Try(GrpcClient.using(endpoint) { client =>
         Await.result(client.getAllInOneFileDescriptorProtoSet,
                      Duration(5, SECONDS))
       }) match {
         case Success(ret) => Some(ret)
         case Failure(t) =>
           Throwables.getRootCause(t) match {
             case root: StatusRuntimeException
                 if root.getStatus.getCode == Status.Code.UNIMPLEMENTED =>
               logger.warn(
                 "Could not list services because the remote host does not support " +
                   "reflection. To disable resolving services by reflection, either pass the flag " +
                   "--use_reflection=false or disable reflection in your config file.")
               None
             case e =>
               logger.err(e.getMessage)
               None
           }
       }
     } else None).getOrElse {
      val protoPaths = protoSources.map(_.toPath)
      val invoker =
        new ProtocInvoker(protoPaths.head, protoPaths.tail)
      invoker.invoke
    }
  }

}
