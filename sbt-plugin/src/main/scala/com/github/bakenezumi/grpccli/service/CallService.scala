package com.github.bakenezumi.grpccli.service

import com.github.bakenezumi.grpccli.protobuf.{ProtoMethodName, ServiceResolver}
import com.github.bakenezumi.grpccli.GrpcClient
import com.google.protobuf.DescriptorProtos.FileDescriptorSet
import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.DynamicMessage
import com.google.protobuf.util.JsonFormat
import com.google.protobuf.util.JsonFormat.TypeRegistry
import io.grpc.stub.StreamObserver
import sbt.util.Logger

import scala.concurrent.{Await, ExecutionContext, Promise}
import scala.concurrent.duration.{Duration, SECONDS}
import scala.collection.JavaConverters._
object CallService {

  def call(
      methodName: String,
      address: String,
      fileDescriptorSet: FileDescriptorSet,
      messageReader: (Descriptor, JsonFormat.TypeRegistry) => MessageReader,
      logger: Logger)(implicit executorContext: ExecutionContext): Unit = {
    val serviceResolver =
      ServiceResolver.fromFileDescriptorSet(fileDescriptorSet)
    val protoMethodName =
      ProtoMethodName.parseFullGrpcMethodName(formatMethodName(methodName))
    val methodDescriptor =
      serviceResolver.resolveServiceMethod(protoMethodName)
    val registry = TypeRegistry
      .newBuilder()
      .add(serviceResolver.listMessageTypes.asJava)
      .build()
    val requestMessages: Seq[DynamicMessage] =
      messageReader(methodDescriptor.getInputType, registry).read
    val promise = Promise[Unit]()
    val responseObserver = new StreamObserver[DynamicMessage] {
      override def onNext(v: DynamicMessage): Unit = {
        println(v.toString)
      }

      override def onError(throwable: Throwable): Unit = {
        logger.warn(throwable.getLocalizedMessage)
        promise.failure(throwable)
      }

      override def onCompleted(): Unit = {
        promise.success(())
      }

    }

    GrpcClient.using(address) { client =>
      Await.result(client.callDynamic(methodDescriptor,
                                      requestMessages,
                                      responseObserver,
                                      promise),
                   Duration(5, SECONDS))
    }
  }

  private def formatMethodName(methodName: String): String =
    if (methodName.contains('/')) methodName
    else {
      val words = methodName.split("\\.").toList
      if (words.isEmpty) methodName
      else words.init.mkString(".") + "/" + words.last
    }

}
