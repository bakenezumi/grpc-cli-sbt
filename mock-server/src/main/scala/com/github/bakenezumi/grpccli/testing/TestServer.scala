/*
 * Copyright 2015, Google Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *    * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *
 *    * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.github.bakenezumi.grpccli.testing

import java.util.logging.Logger

import io.grpc.Server
import io.grpc.examples.helloworld.helloworld.{
  GreeterGrpc,
  HelloReply,
  HelloRequest
}
import io.grpc.netty.NettyServerBuilder
import io.grpc.protobuf.services.ProtoReflectionService
import io.netty.handler.ssl.SslContext

import scala.concurrent.{ExecutionContext, Future}

object TestServer {
  private val logger = Logger.getLogger(classOf[TestServer].getName)

  def main(args: Array[String]): Unit = {
    val server = new TestServer(port, ExecutionContext.global, None)
    server.start()
    server.blockUntilShutdown()
  }

  private val port = 50051
}

class TestServer(port: Int,
                 executionContext: ExecutionContext,
                 sslContext: Option[SslContext]) { self =>
  private[this] var server: Server = null

  def start(): Unit = {
    val builder = NettyServerBuilder
      .forPort(TestServer.port)
      .addService(GreeterGrpc.bindService(new GreeterImpl, executionContext))
      .addService(ProtoReflectionService.newInstance())
    sslContext.foreach(ctx => builder.sslContext(ctx))
    server = builder.build.start
    TestServer.logger.info("Server started, listening on " + TestServer.port)
    sys.addShutdownHook {
      System.err.println(
        "*** shutting down gRPC server since JVM is shutting down")
      self.stop()
      System.err.println("*** server shut down")
    }
  }

  def stop(): Unit = {
    if (server != null) {
      server.shutdown()
    }
  }

  private def blockUntilShutdown(): Unit = {
    if (server != null) {
      server.awaitTermination()
    }
  }

  private class GreeterImpl extends GreeterGrpc.Greeter {
    override def sayHello(req: HelloRequest) = {
      val reply = HelloReply(message = "Hello " + req.name)
      println("request: " + req.name)
      Future.successful(reply)
    }
  }

}
