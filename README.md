gRPC CLI for SBT
======================

### Setup

```sh
$ sbt publishLocal
```

edit `your-project/project/plugins.sbt`

```scala
addSbtPlugin("com.github.bakenezumi" % "grpc-cli-sbt" % "0.1.0-SNAPSHOT")
```

edit `your-project/build.sbt`

```scala
enablePlugins(com.github.bakenezumi.grpccli.GrpcCliPlugin)
```

### Usage

After enabling server reflection in a server application, you can use gRPC CLI to get information about its available services.

```sh
$ sbt
sbt:your-project> set grpcServerAddress  := "localhost:50051"
sbt:your-project> grpc-cli ls
```

### List services
```
sbt:your-project> grpc-cli ls
```

### List methods
```
sbt:your-project> grpc-cli ls helloworld.Greeter -l
```

### Inspect message types
```
sbt:your-project> grpc-cli type helloworld.HelloRequest
```

### Call a remote method

```
sbt:your-project> grpc-cli call helloworld.Greeter.SayHello
reading request message from stdin...
{name:"gRPC CLI"}

```
output: `message: "Hello gRPC CLI"`


License
--------
Apache License, Version 2.0
