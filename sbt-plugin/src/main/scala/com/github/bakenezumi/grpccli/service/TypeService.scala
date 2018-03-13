package com.github.bakenezumi.grpccli.service

import com.github.bakenezumi.grpccli.protobuf.ProtobufFormat
import com.google.protobuf.DescriptorProtos.FileDescriptorSet

import scala.collection.JavaConverters._

object TypeService {
  def apply(fileDescriptorSet: FileDescriptorSet,
            typeName: String): Seq[String] = {
    fileDescriptorSet.getFileList.asScala.flatMap(
      file =>
        file.getMessageTypeList.asScala
          .map(descriptor => (file.getName, file.getPackage, descriptor))
          .collect {
            case (_, pkg, descriptor)
                if typeName == formatPackage(pkg) + descriptor.getName =>
              ProtobufFormat
                .print(descriptor)
        })
  }

  private def formatPackage(packageName: String): String =
    if (packageName.isEmpty) ""
    else packageName + "."
}
