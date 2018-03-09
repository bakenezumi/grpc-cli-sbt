import sbt._

val grpcJavaVersion = "1.10.0"

val unusedWarnings =
  "-Ywarn-unused" ::
  "-Ywarn-unused-import" ::
  Nil

val commonSettings = Seq(
  fork in Test := true,
  scalaVersion := "2.12.4",
  organization := "com.github.bakenezumi",
  version := "0.1.0-SNAPSHOT",
  scalacOptions ++= Seq(
    "-deprecation",
    "-feature",
    "-unchecked"
  )
)

lazy val root = (project in file(".")).settings(
  publish := {},
  publishLocal := {},
  skip in publish := true
) aggregate(core, plugin)

lazy val core = (project in file("core")).settings(
  name := "grpc-cli-core",
  commonSettings,
  libraryDependencies ++=
    "io.grpc" % "grpc-stub" % grpcJavaVersion ::
    "io.grpc" % "grpc-services" % grpcJavaVersion ::
    "io.grpc" % "grpc-netty" % grpcJavaVersion ::
    "com.github.os72" % "protoc-jar" % "3.5.1" ::
    "org.scalatest" %% "scalatest" % "3.0.5" % Test ::
    Nil,
)

lazy val plugin = (project in file("sbt-plugin")).settings(
  name := "grpc-cli-sbt",
  commonSettings,
  sbtPlugin := true,
  scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++
        Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
  scriptedBufferLog := false,
  moduleName := "grpc-cli-sbt",
  libraryDependencies ++=
    "org.scalatest" %% "scalatest" % "3.0.5" % Test ::
    Nil,
) dependsOn core
