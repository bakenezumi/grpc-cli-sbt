package com.github.bakenezumi.grpccli.service

import com.github.bakenezumi.grpccli.protobuf.ProtobufFormat
import com.google.protobuf.Descriptors.Descriptor
import sbt.internal.util.complete.DefaultParsers.token
import sbt.internal.util.complete.DefaultParsers._

import scala.collection.JavaConverters._

class TypeFieldParser(descriptor: Descriptor) {
  private val fields = descriptor.getFields.asScala

  val colon = token(":")

  lazy val field =
    (NotSpace.examples(fields.map(_.getName): _*) ~ colon).flatMap {
      case (fieldName, _) =>
        token(
          NotSpace,
          fields
            .find(_.getName == fieldName)
            .map(fld =>
              s"<${ProtobufFormat.typeName(fld.getType.toProto, fld.toProto.getTypeName)}>")
            .getOrElse("Invalid field name")
        )
    }
}
