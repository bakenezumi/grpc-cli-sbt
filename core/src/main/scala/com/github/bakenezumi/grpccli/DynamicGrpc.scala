package com.github.bakenezumi.grpccli

import java.util.logging.Logger

import com.github.bakenezumi.grpccli.protobuf.DynamicMessageMarshaller
import com.google.common.base.Preconditions
import com.google.common.util.concurrent.ListenableFuture
import com.google.protobuf.Descriptors.MethodDescriptor
import com.google.protobuf.DynamicMessage
import io.grpc.MethodDescriptor.MethodType
import io.grpc.stub.{ClientCalls, StreamObserver}
import io.grpc.{CallOptions, Channel}

/** Copy from [[https://github.com/grpc-ecosystem/polyglot/blob/v1.6.0/src/main/java/me/dinowernli/grpc/polyglot/grpc/DynamicGrpcClient.java]]
  * */
object DynamicGrpc {
  def apply(protoMethodDescriptor: MethodDescriptor,
            channel: Channel): DynamicGrpc =
    new DynamicGrpc(protoMethodDescriptor, channel)

}

class DynamicGrpc private[grpccli] (protoMethodDescriptor: MethodDescriptor,
                                    channel: Channel) {

  private val logger =
    Logger.getLogger(classOf[GrpcClient].getName)

  /**
    * Makes an rpc to the remote endpoint and respects the supplied callback. Returns a future which
    * terminates once the call has ended. For calls which are single-request, this throws
    * `IllegalArgumentException` if the size of `requests` is not exactly 1.
    */
  def call(requests: List[DynamicMessage],
           responseObserver: StreamObserver[DynamicMessage],
           callOptions: CallOptions): ListenableFuture[Void] = {
    Preconditions.checkArgument(
      requests.nonEmpty,
      "Can't make call without any requests".asInstanceOf[Any])
    val methodType = getMethodType
    val numRequests = requests.size
    if (methodType eq MethodType.UNARY) {
      logger.info("Making unary call")
      Preconditions.checkArgument(
        numRequests == 1,
        ("Need exactly 1 request for unary call, but got: " + numRequests)
          .asInstanceOf[Any])
      callUnary(requests.head, responseObserver, callOptions)
    } else if (methodType eq MethodType.SERVER_STREAMING) {
      logger.info("Making server streaming call")
      Preconditions.checkArgument(
        numRequests == 1,
        ("Need exactly 1 request for server streaming call, but got: " + numRequests)
          .asInstanceOf[Any])
      callServerStreaming(requests.head, responseObserver, callOptions)
    } else if (methodType eq MethodType.CLIENT_STREAMING) {
      logger.info(
        "Making client streaming call with " + requests.size + " requests")
      callClientStreaming(requests, responseObserver, callOptions)
    } else { // Bidi streaming.
      logger.info(
        "Making bidi streaming call with " + requests.size + " requests")
      callBidiStreaming(requests, responseObserver, callOptions)
    }
  }

  private def callBidiStreaming(
      requests: List[DynamicMessage],
      responseObserver: StreamObserver[DynamicMessage],
      callOptions: CallOptions) = {
    val doneObserver = new DoneObserver[DynamicMessage]
    val requestObserver = ClientCalls.asyncBidiStreamingCall(
      createCall(callOptions),
      CompositeStreamObserver(responseObserver, doneObserver))
    requests.foreach(requestObserver.onNext)
    requestObserver.onCompleted()
    doneObserver.getCompletionFuture
  }

  private def callClientStreaming(
      requests: List[DynamicMessage],
      responseObserver: StreamObserver[DynamicMessage],
      callOptions: CallOptions) = {
    val doneObserver = new DoneObserver[DynamicMessage]
    val requestObserver = ClientCalls.asyncClientStreamingCall(
      createCall(callOptions),
      CompositeStreamObserver(responseObserver, doneObserver))
    requests.foreach(requestObserver.onNext)
    requestObserver.onCompleted()
    doneObserver.getCompletionFuture
  }

  private def callServerStreaming(
      request: DynamicMessage,
      responseObserver: StreamObserver[DynamicMessage],
      callOptions: CallOptions) = {
    val doneObserver = new DoneObserver[DynamicMessage]
    ClientCalls.asyncServerStreamingCall(
      createCall(callOptions),
      request,
      CompositeStreamObserver(responseObserver, doneObserver))
    doneObserver.getCompletionFuture
  }

  private def callUnary(request: DynamicMessage,
                        responseObserver: StreamObserver[DynamicMessage],
                        callOptions: CallOptions) = {
    val doneObserver = new DoneObserver[DynamicMessage]
    ClientCalls.asyncUnaryCall(
      createCall(callOptions),
      request,
      CompositeStreamObserver(responseObserver, doneObserver))
    doneObserver.getCompletionFuture
  }

  private def createCall(callOptions: CallOptions) =
    channel.newCall(createGrpcMethodDescriptor, callOptions)

  private def createGrpcMethodDescriptor = {
    io.grpc.MethodDescriptor
      .newBuilder(
        new DynamicMessageMarshaller(protoMethodDescriptor.getInputType),
        new DynamicMessageMarshaller(protoMethodDescriptor.getOutputType))
      .setType(getMethodType)
      .setFullMethodName(getFullMethodName)
      .build()
  }

  private def getFullMethodName = {
    val serviceName = protoMethodDescriptor.getService.getFullName
    val methodName = protoMethodDescriptor.getName
    io.grpc.MethodDescriptor.generateFullMethodName(serviceName, methodName)
  }

  /** Returns the appropriate method type based on whether the client or server expect streams. */
  private def getMethodType = {
    val clientStreaming = protoMethodDescriptor.toProto.getClientStreaming
    val serverStreaming = protoMethodDescriptor.toProto.getServerStreaming
    if (!clientStreaming && !serverStreaming) MethodType.UNARY
    else if (!clientStreaming && serverStreaming) MethodType.SERVER_STREAMING
    else if (clientStreaming && !serverStreaming) MethodType.CLIENT_STREAMING
    else MethodType.BIDI_STREAMING
  }
}
