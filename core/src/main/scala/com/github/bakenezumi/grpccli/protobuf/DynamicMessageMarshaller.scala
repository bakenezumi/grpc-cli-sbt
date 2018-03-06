package com.github.bakenezumi.grpccli.protobuf
import java.io.InputStream

import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.{DynamicMessage, ExtensionRegistryLite}
import io.grpc.MethodDescriptor.Marshaller

class DynamicMessageMarshaller(messageDescriptor: Descriptor)
    extends Marshaller[DynamicMessage] {

  def parse(inputStream: InputStream): DynamicMessage =
    DynamicMessage
      .newBuilder(messageDescriptor)
      .mergeFrom(inputStream, ExtensionRegistryLite.getEmptyRegistry)
      .build

  def stream(abstractMessage: DynamicMessage): InputStream =
    abstractMessage.toByteString.newInput
}
