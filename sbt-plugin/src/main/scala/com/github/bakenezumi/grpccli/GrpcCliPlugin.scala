package com.github.bakenezumi.grpccli

import com.github.bakenezumi.grpccli.protobuf.PrintedTypeNameCache
import com.github.bakenezumi.grpccli.service.InitService
import com.google.protobuf.DescriptorProtos.FileDescriptorSet
import sbt.Keys._
import sbt._

object GrpcCliPlugin extends AutoPlugin {

  override def trigger = PluginTrigger.AllRequirements

  val help =
    Help.briefOnly(
      Seq(("grpc-cli ls ...", "List services"),
          ("grpc_cli call ...", "Call method"),
          ("grpc_cli type ...", "Print type")))

  object internalKeys {
    lazy val gRPCFileDescriptorSet =
      SettingKey[FileDescriptorSet](
        "gRPC-File-descriptor-set",
        "The protocol compiler can output a FileDescriptorSet containing the .proto files it parses.")

    lazy val gRPCServiceList =
      SettingKey[Seq[String]]("gRPC-service-list", "a list of gRPC services")

    lazy val gRPCServiceMethodList =
      SettingKey[Seq[String]]("gRPC-service-method-list",
                              "a list of gRPC service methods")
  }

  import internalKeys._

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
                     getOpt(gRPCFileDescriptorSet).get,
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
      InitService.getFileDescriptorSet(gRPCUseReflection.value,
                                       gRPCEndpoint.value,
                                       gRPCProtoSources.value,
                                       sLog.value)
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
