addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.15")

libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.7.0"

libraryDependencies += "com.github.os72" % "protoc-jar" % "3.5.1"

libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
