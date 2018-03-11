package com.github.bakenezumi.grpccli

import com.github.bakenezumi.grpccli.protobuf.{
  PrintedTypeNameCache,
  ProtocInvoker
}
import com.google.protobuf.DescriptorProtos
import sbt.Keys._
import sbt._

object GrpcCliPlugin extends AutoPlugin {

  override def trigger = PluginTrigger.AllRequirements

  lazy val gRPCServiceList =
    SettingKey[Seq[String]]("gRPC-service-list", "service list")

  lazy val gRPCServiceMethodList =
    SettingKey[Seq[String]]("gRPC-service-method-list", "service method list")

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
        override lazy val serviceList: Seq[String] =
          getOpt(gRPCServiceList).get
        override lazy val methodList: Seq[String] =
          getOpt(gRPCServiceMethodList).get
      }
      parser.GrpcCliCommand
    } { (state, parsed) =>
      val extracted = Project extract state
      import extracted._
      println(getOpt(gRPCEndpoint).get)
      val logger = getOpt(sLog).get
      parsed match {
        case ls @ LsCommand(method, _) =>
          ls.apply(getOpt(gRPCEndpoint).get) match {
            case Nil =>
              logger.info(s"Service or method $method not found.")
            case result => result.foreach(println)
          }
        case tpe @ TypeCommand(typeName) =>
          tpe.apply(getOpt(gRPCEndpoint).get) match {
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
    gRPCServiceList := {
      if (gRPCUseReflection.value) {
        try {
          PrintedTypeNameCache.clear()
          LsCommand().apply(gRPCEndpoint.value)
        } catch {
          case _: _root_.io.grpc.StatusRuntimeException =>
            sLog.value.warn(
              s"Received an error when querying services endpoint: ${gRPCEndpoint.value}")
            Nil
        }
      } else { // TODO: WIP
        Nil
      }
    },
    gRPCServiceMethodList := {
      if (gRPCUseReflection.value) {
        try {
          gRPCServiceList.value.flatMap(
            serviceName =>
              LsCommand(serviceName)
                .apply(gRPCEndpoint.value)
                .map(methodName => serviceName + "." + methodName))
        } catch {
          case _: _root_.io.grpc.StatusRuntimeException =>
            sLog.value.warn(
              s"Received an error when querying services endpoint: ${autoImport.gRPCEndpoint.value}")
            Nil
        }
      } else { // TODO: WIP
        import scala.collection.JavaConverters._
        val protoPaths = gRPCProtoSources.value.map(_.toPath)
        val invoker = new ProtocInvoker(protoPaths.head, protoPaths.tail)
        val fileDescriptorSet: DescriptorProtos.FileDescriptorSet =
          invoker.invoke
        fileDescriptorSet.getFileList.asScala.toList.flatMap(file =>
          file.getServiceList.asScala.flatMap(service =>
            service.getMethodList.asScala.map(method =>
              file.getPackage + "." + service.getName + "." + method.getName)))

      }
    },
  )

  override def projectSettings: Seq[Def.Setting[_]] =
    grpcCliSettings
}
