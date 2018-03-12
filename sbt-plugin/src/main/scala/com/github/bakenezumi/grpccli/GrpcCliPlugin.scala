package com.github.bakenezumi.grpccli

import com.github.bakenezumi.grpccli.protobuf.{
  PrintedTypeNameCache,
  ProtocInvoker
}
import com.google.common.base.Throwables
import com.google.protobuf.DescriptorProtos.FileDescriptorSet
import sbt.Keys._
import sbt._
import _root_.io.grpc.{Status, StatusRuntimeException}
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}
import scala.util.{Failure, Success, Try}

object GrpcCliPlugin extends AutoPlugin {

  override def trigger = PluginTrigger.AllRequirements

  lazy val gRPCFileDescriptorSet =
    SettingKey[FileDescriptorSet](
      "gRPC-local-File-descriptor-set",
      "FileDescriptorSet retrieved remotely by server reflection")

  lazy val gRPCServiceList =
    SettingKey[Seq[String]]("gRPC-service-list-local",
                            "a list of service methods")

  lazy val gRPCServiceMethodList =
    SettingKey[Seq[String]]("gRPC-service-method-list-local",
                            "a list of service methods")

  val help =
    Help.briefOnly(
      Seq(("grpc-cli ls ...", "List services"),
          ("grpc_cli call ...", "Call method"),
          ("grpc_cli type ...", "Print type")))

  object autoImport {

    lazy val gRPCEndpoint =
      SettingKey[String]("gRPC-endpoint", "host:port")

    lazy val gRPCUseReflection =
      SettingKey[Boolean](
        "gRPC-use-reflection",
        " If true, protos will first be resolved by reflection if applicable.")

    lazy val gRPCProtoSources =
      SettingKey[Seq[File]](
        "gRPC-proto-sources",
        "look for protos to compile (default src/main/protobuf)")

    val grpcCli = Command("grpc-cli", help) { state =>
      val extracted = Project extract state
      import extracted._
      object parser extends GrpcCliCommandParser {
        override lazy val fileDescriptorSet: FileDescriptorSet =
          getOpt(gRPCFileDescriptorSet).get
        override lazy val serviceList: Seq[String] =
          getOpt(gRPCServiceList).get
        override lazy val methodList: Seq[String] =
          getOpt(gRPCServiceMethodList).get
      }
      parser.GrpcCliCommand
    } { (state, parsed) =>
      val extracted = Project extract state
      import extracted._
      val logger = getOpt(sLog).get
      parsed match {
        case ls @ LsCommand(_, method, _) =>
          ls.apply match {
            case Nil =>
              logger.info(s"Service or method $method not found.")
            case result => result.foreach(println)
          }
        case tpe @ TypeCommand(_, typeName) =>
          tpe.apply match {
            case Nil =>
              logger.info(s"Type $typeName not found.")
            case result => result.foreach(println)
          }
        case call @ CallCommand(_) =>
          call.apply(getOpt(gRPCEndpoint).get,
                     getOpt(gRPCUseReflection).get,
                     getOpt(gRPCProtoSources).get.map(_.toPath),
                     logger)
      }
      state
    }
  }

  import GrpcCliPlugin.autoImport._

  def grpcCliSettings = Seq(
    gRPCEndpoint := "localhost:50051",
    gRPCUseReflection := true,
    gRPCProtoSources := Seq((sourceDirectory in Compile).value / "protobuf"),
    commands += grpcCli,
    gRPCFileDescriptorSet := {
      PrintedTypeNameCache.clear()
      (if (gRPCUseReflection.value) {
         import scala.concurrent.ExecutionContext.Implicits.global
         val Array(host: String, port: String) =
           gRPCEndpoint.value.split(":")
         Try {
           Await.result(GrpcClient
                          .apply(host, port.toInt)
                          .getAllInOneFileDescriptorProtoSet,
                        Duration(5, SECONDS))
         } match {
           case Success(ret) => Some(ret)
           case Failure(t) =>
             Throwables.getRootCause(t) match {
               case root: StatusRuntimeException
                   if root.getStatus.getCode == Status.Code.UNIMPLEMENTED =>
                 sLog.value.warn(
                   "Could not list services because the remote host does not support " +
                     "reflection. To disable resolving services by reflection, either pass the flag " +
                     "--use_reflection=false or disable reflection in your config file.")
                 None
               case e =>
                 sLog.value.err(e.getMessage)
                 None
             }
         }
       } else None).getOrElse {
        val protoPaths = gRPCProtoSources.value.map(_.toPath)
        val invoker =
          new ProtocInvoker(protoPaths.head, protoPaths.tail)
        invoker.invoke
      }

    },
    gRPCServiceList := {
      import scala.collection.JavaConverters._
      for {
        file <- gRPCFileDescriptorSet.value.getFileList.asScala.toList
        service <- file.getServiceList.asScala
      } yield file.getPackage + "." + service.getName
    },
    gRPCServiceMethodList := {
      import scala.collection.JavaConverters._
      for {
        file <- gRPCFileDescriptorSet.value.getFileList.asScala.toList
        service <- file.getServiceList.asScala
        method <- service.getMethodList.asScala
      } yield file.getPackage + "." + service.getName + "." + method.getName
    }
  )

  override def projectSettings: Seq[Def.Setting[_]] =
    grpcCliSettings
}
