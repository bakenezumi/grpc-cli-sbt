package com.github.bakenezumi.grpccli

import io.grpc.stub.StreamObserver

case class CompositeStreamObserver[T](observers: StreamObserver[T]*)
    extends StreamObserver[T] {

  override def onCompleted(): Unit = {
    for (observer <- observers) {
      observer.onCompleted()
    }
  }

  override def onError(t: Throwable): Unit = {
    for (observer <- observers) {
      observer.onError(t)
    }
  }

  override def onNext(value: T): Unit = {
    for (observer <- observers) {
      observer.onNext(value)
    }
  }

}
