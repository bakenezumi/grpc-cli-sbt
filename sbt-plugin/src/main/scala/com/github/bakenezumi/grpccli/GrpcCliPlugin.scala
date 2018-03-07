package com.github.bakenezumi.grpccli

import com.github.bakenezumi.grpccli.protobuf.PrintedTypeNameCache
import sbt.Keys._
import sbt._

object GrpcCliPlugin extends AutoPlugin {

  override def trigger = PluginTrigger.AllRequirements

  lazy val gRPCServiceList =
    SettingKey[Seq[String]]("gRPC-service-list", "service list")

  lazy val gRPCServiceMethodList =
    SettingKey[Seq[String]]("gRPC-service-method-list", "service method list")

  object autoImport {

    lazy val gRPCEndpoint =
      SettingKey[String]("gRPC-endpoint", "host:port")

    val help =
      Help.briefOnly(
        Seq(("grpc-cli ls ...", "List services"),
            ("grpc_cli call ...", "Call method"),
            ("grpc_cli type ...", "Print type")))

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
      parsed match {
        case ls @ LsCommand(method, _) =>
          ls.apply(getOpt(gRPCEndpoint).get) match {
            case Nil =>
              println(s"Service or method $method not found.")
            case result => result.foreach(println)
          }
        case tpe @ TypeCommand(typeName) =>
          tpe.apply(getOpt(gRPCEndpoint).get) match {
            case Nil =>
              println(s"Type $typeName not found.")
            case result => result.foreach(println)
          }
        case call @ CallCommand(_) =>
          call.apply(getOpt(gRPCEndpoint).get)
      }
      state
    }
  }

  def grpcCliSettings = Seq(
    autoImport.gRPCEndpoint := "localhost:50051",
    commands += autoImport.grpcCli,
    gRPCServiceList := {
      try {
        PrintedTypeNameCache.clear()
        LsCommand().apply(autoImport.gRPCEndpoint.value)
      } catch {
        case _: _root_.io.grpc.StatusRuntimeException =>
          println(
            s"Received an error when querying services endpoint: ${autoImport.gRPCEndpoint.value}")
          Nil
      }
    },
    gRPCServiceMethodList := {
      try {
        gRPCServiceList.value.flatMap(
          serviceName =>
            LsCommand(serviceName)
              .apply(autoImport.gRPCEndpoint.value)
              .map(methodName => serviceName + "." + methodName))
      } catch {
        case _: _root_.io.grpc.StatusRuntimeException =>
          println(
            s"Received an error when querying services endpoint: ${autoImport.gRPCEndpoint.value}")
          Nil
      }
    }
  )

  override def projectSettings: Seq[Def.Setting[_]] =
    grpcCliSettings
}
