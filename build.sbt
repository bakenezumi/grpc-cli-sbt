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
  version := "0.2.0-SNAPSHOT",
  scalacOptions ++= Seq(
    "-deprecation",
    "-feature",
    "-unchecked"
  ),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  publishTo := Some(
    if (isSnapshot.value)
      Opts.resolver.sonatypeSnapshots
    else
      Opts.resolver.sonatypeStaging
    ),
  licenses := _licenses,
  homepage := _homepage,
  scmInfo := _scmInfo,
  developers := _developers  
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
) dependsOn(mockServer % "test->compile")

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
) dependsOn (core, mockServer % "test->compile")

import scalapb.compiler.Version.{grpcJavaVersion, scalapbVersion}

lazy val mockServer = (project in file("mock-server")).settings(
  name :="grpc-mock-server",
  PB.targets in Compile := Seq(
    scalapb.gen() -> (sourceManaged in Compile).value
  ),
  libraryDependencies ++=
    "io.grpc" % "grpc-netty" % grpcJavaVersion ::
    "io.grpc" % "grpc-services" % grpcJavaVersion ::
    "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapbVersion ::
    Nil,
)


lazy val _licenses = Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))
lazy val _homepage = Some(url("https://github.com/bakenezumi"))

lazy val _scmInfo = Some(
  ScmInfo(
    url("https://github.com/bakenezumi/grpc-cli-sbt"),
    "scm:git@github.com:/bakenezumi/grpc-cli-sbt.git"
  )
)

lazy val _developers = List(
  Developer(
    id    = "bakenezumi",
    name  = "Nobuhiko Hosonishi",
    email = "hosonioshi@gmail.com",
    url   = url("https://github.com/bakenezumi")
  )
)