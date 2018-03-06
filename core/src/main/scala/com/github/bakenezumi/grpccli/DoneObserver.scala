package com.github.bakenezumi.grpccli

import io.grpc.stub.StreamObserver
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture

class DoneObserver[T] extends StreamObserver[T] {

  private[this] val doneFuture: SettableFuture[Void] =
    SettableFuture.create[Void]

  def onCompleted(): Unit = {
    doneFuture.set(null.asInstanceOf[Void])
  }

  def onError(t: Throwable): Unit = {
    doneFuture.setException(t)
  }

  def onNext(next: T): Unit = {
    // Do nothing.
  }

  /**
    * Returns a future which completes when the rpc finishes. The returned future fails if the rpc
    * fails.
    */
  def getCompletionFuture: ListenableFuture[Void] =
    doneFuture
}
