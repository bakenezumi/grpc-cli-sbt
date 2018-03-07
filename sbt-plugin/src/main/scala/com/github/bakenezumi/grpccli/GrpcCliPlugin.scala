package com.github.bakenezumi.grpccli

import com.github.bakenezumi.grpccli.GrpcCliCommandParser._
import sbt.Keys._
import sbt._

object GrpcCliPlugin extends AutoPlugin {

  override def trigger = PluginTrigger.AllRequirements

  object autoImport {

    lazy val grpcServerAddress =
      SettingKey[String]("grpc-server-address", "host:port")

    val grpcCli = Command("grpc-cli", help)(_ => grpcCliCommand) {
      (state, parsed) =>
        val extracted = Project extract state
        import extracted._
        println(getOpt(grpcServerAddress).get)
        parsed match {
          case ls @ LsCommand(method, _) =>
            ls.apply(getOpt(grpcServerAddress).get) match {
              case Nil =>
                println(s"Service or method $method not found.")
              case result => result.foreach(println)
            }
          case tpe @ TypeCommand(typeName) =>
            tpe.apply(getOpt(grpcServerAddress).get) match {
              case Nil =>
                println(s"Type $typeName not found.")
              case result => result.foreach(println)
            }
          case call @ CallCommand(_) =>
            call.apply(getOpt(grpcServerAddress).get)
        }
        state
    }
  }

  def grpcCliSettings = Seq(
    autoImport.grpcServerAddress := "localhost:50051",
    commands += autoImport.grpcCli
  )

  override def projectSettings: Seq[Def.Setting[_]] =
    grpcCliSettings
}
