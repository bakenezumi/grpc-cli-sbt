package com.github.bakenezumi.grpccli.protobuf

import com.google.protobuf.DescriptorProtos._

import scala.collection.JavaConverters._

// TODO: WIP
object ProtobufFormat {
  def print(message: DescriptorProto): String = {
    val fields: List[FieldDescriptorProto] =
      message.getFieldList.asScala.toList

    val oneofDeclList = message.getOneofDeclList.asScala
    val groupedFields = fields
      .groupBy(
        field =>
          if (field.hasOneofIndex) Some(oneofDeclList(field.getOneofIndex))
          else None) // group by maybe OneofDecl
      .toSeq
      .sortBy(_._2.map(_.getNumber).max) // sort by field number
    val fieldsString =
      groupedFields
        .map {
          case (Some(oneof), _fields) =>
            s"""|  oneof ${oneof.getName} {
                |    ${_fields
              .map(print)
              .mkString(System.lineSeparator + "    ")}
                |  }"""
          case (None, _fields) =>
            s"""  ${_fields
              .map(print)
              .mkString(System.lineSeparator + "  ")}"""
        }
        .mkString(System.lineSeparator + "  ")

    s"""|message ${message.getName} {
        |$fieldsString
        |}
        |""".stripMargin
  }

  def print(field: FieldDescriptorProto): String = {
    val label = field.getLabel match {
      case FieldDescriptorProto.Label.LABEL_OPTIONAL => ""
      case FieldDescriptorProto.Label.LABEL_REQUIRED => "required "
      case FieldDescriptorProto.Label.LABEL_REPEATED => "repeated "
    }
    val typeName = field.getType match {
      case FieldDescriptorProto.Type.TYPE_DOUBLE   => "double"
      case FieldDescriptorProto.Type.TYPE_FLOAT    => "float"
      case FieldDescriptorProto.Type.TYPE_INT64    => "int64"
      case FieldDescriptorProto.Type.TYPE_UINT64   => "uint64"
      case FieldDescriptorProto.Type.TYPE_INT32    => "int32"
      case FieldDescriptorProto.Type.TYPE_FIXED64  => "fixed64"
      case FieldDescriptorProto.Type.TYPE_FIXED32  => "fixed32"
      case FieldDescriptorProto.Type.TYPE_BOOL     => "bool"
      case FieldDescriptorProto.Type.TYPE_STRING   => "string"
      case FieldDescriptorProto.Type.TYPE_BYTES    => "bytes"
      case FieldDescriptorProto.Type.TYPE_UINT32   => "uint32"
      case FieldDescriptorProto.Type.TYPE_SFIXED32 => "sfixed32"
      case FieldDescriptorProto.Type.TYPE_SFIXED64 => "sfixed64"
      case FieldDescriptorProto.Type.TYPE_SINT32   => "sint32"
      case FieldDescriptorProto.Type.TYPE_SINT64   => "sint64"

      case FieldDescriptorProto.Type.TYPE_ENUM    => field.getTypeName
      case FieldDescriptorProto.Type.TYPE_GROUP   => field.getTypeName
      case FieldDescriptorProto.Type.TYPE_MESSAGE => field.getTypeName

      case _ => field.getTypeName
    }
    s"""$label$typeName ${field.getName} = ${field.getNumber}${if (field.hasJsonName)
         s"""[json_name = "${field.getJsonName}"]"""
       else ""};""".stripMargin

  }
}
