gRPC command line tool on SBT
======================

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.bakenezumi/grpc-cli-sbt/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.bakenezumi/grpc-cli-sbt)

## Overview

This is [gRPC](https://grpc.io/) command line tool to use on SBT.

This is implemented to follow to the [original specifications](https://github.com/grpc/grpc/blob/master/doc/command_line_tool.md). and it refer to [Polyglot](https://github.com/grpc-ecosystem/polyglot).

This tool assists the input with tab completion when entering a command.

## Core functionality

The command line tool can do the following things:

- Send unary rpc.
- ~~Attach metadata and~~(yet) display received metadata.
- ~~Handle common authentication to server.~~(yet)
- Infer request/response types from server reflection result.
- Find the request/response types from a given proto file.
- Read proto request in text form.
- ~~Read request in wire form (for protobuf messages, this means serialized binary form).~~(yet)
- Display proto response in text form.
- ~~(not yet)Write response in wire form to a file.~~(yet)

### Setup

edit `your-project/project/plugins.sbt`

```scala
addSbtPlugin("com.github.bakenezumi" % "grpc-cli-sbt" % "0.1.0")
```

## Prerequisites

Most `grpc-cli` commands need the server to support server reflection. See
guides for
[Java](https://github.com/grpc/grpc-java/blob/master/documentation/server-reflection-tutorial.md#enable-server-reflection)
, [C++](https://github.com/grpc/grpc/blob/master/doc/server_reflection_tutorial.md)
and [Go](https://github.com/grpc/grpc-go/blob/master/Documentation/server-reflection-tutorial.md)

### Usage

After enabling server reflection in a server application, you can use gRPC CLI to get information about its available services.

```sh
$ sbt
sbt:your-project> set gRPCEndpoint := "localhost:50051"
sbt:your-project> grpc-cli ls
```

The localhost:50051 part indicates the server you are connecting to.

### List services

`grpc-cli ls` command lists services and methods exposed at a setting port

- List all the services exposed at a given port

  ```
  sbt:your-project> grpc-cli ls
  ```

  output:

  ```
  grpc.reflection.v1alpha.ServerReflection
  helloworld.Greeter
  ```
- List one service with details

  `grpc-cli ls` command inspects a service given its full name (in the format
      of \<package\>.\<service\>). It can print information with a long listing
      format when `-l` flag is set. This flag can be used to get more details
      about a service.
  
  ```
  sbt:your-project> grpc-cli ls helloworld.Greeter -l
  ```

  `helloworld.Greeter` is full name of the service.

  output:

  ```proto
  filename: helloworld.proto
  package: helloworld;
  service Greeter {
    rpc SayHello(helloworld.HelloRequest) returns (helloworld.HelloReply) {}
  }
  ```

### List methods

- List one method with details
  
  `grpc-cli ls` command also inspects a method given its full name (in the format of \<package\>.\<service\>.\<method\>).

  ```
  sbt:your-project> grpc-cli ls helloworld.Greeter.SayHello -l
  ```

  helloworld.Greeter.SayHello is full name of the method.

  output:

  ```proto
    rpc SayHello(helloworld.HelloRequest) returns (helloworld.HelloReply) {}
  ```

### Inspect message types

We can use `grpc-cli type`  command to inspect request/response types given the
full name of the type (in the format of \<package\>.\<type\>).

- Get information about the request type
  
  ```
  sbt:your-project> grpc-cli type helloworld.HelloRequest
  ```

  `helloworld.HelloRequest` is the full name of the request type.

  output:

  ```proto
  message HelloRequest {
    string name = 1[json_name = "name"];
  }
  ```

### Call a remote method

We can send RPCs to a server and get responses using `grpc-cli call` command.

- Call a unary method Send a rpc to a helloworld

  ```
  sbt:your-project> grpc-cli call helloworld.Greeter.SayHello
  reading request message from stdin...
  {name:"gRPC CLI SBT"}

  ```
  
  output: `message: "Hello gRPC CLI SBT"`

- Use local proto files
  
    If the server does not have the server reflection service, you will need to
    provide local proto files containing the service definition.

    The plugin assumes your `proto` files are under `src/main/protobuf`,
    however this is configurable using the `gRPCProtoSources` setting.

License
--------
Apache License, Version 2.0
