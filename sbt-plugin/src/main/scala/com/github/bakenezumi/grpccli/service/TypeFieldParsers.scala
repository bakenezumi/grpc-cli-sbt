package com.github.bakenezumi.grpccli.service

import com.github.bakenezumi.grpccli.protobuf.ProtobufFormat
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto
import com.google.protobuf.Descriptors.Descriptor
import sbt.internal.util.complete.DefaultParsers.{token, _}
import sbt.internal.util.complete.Parser

import scala.collection.JavaConverters._

case class TypeFieldParsers private (descriptor: Descriptor,
                                     entered: List[String]) {

  import TypeFieldParsers._

  private[this] val fields = descriptor.getFields.asScala

  private[this] val enteredFieldNames =
    entered
      .map { s =>
        Parser.parse(s, FieldName)
      }
      .collect {
        case Right(s) => s
      }

  private[this] val remainedFieldNames =
    fields.map(_.getName).filter(s => !enteredFieldNames.contains(s))

  /** [field name]:[field value] */
  lazy val Field: Parser[Parsed.Field] =
    (ID.examples(remainedFieldNames: _*) ~ Colon).flatMap {
      case (name, _) =>
        val maybeField = fields.find(_.getName == name)
        maybeField match {
          case Some(fieldDesc) if fieldDesc.toProto.getTypeName.nonEmpty =>
            val parser = TypeFieldParsers(fieldDesc.getMessageType, Nil)
            (OptSpace ~> token("{") ~>
              parser.Field)
              .flatMap { field =>
                (OptSpace ~> Comma ~> OptSpace ~> parser
                  .copy(entered = field.name + ":dummy" :: parser.entered)
                  .Field).* <~ token("}")
              }
              .map { fields =>
                Parsed.Field(name, Seq(Parsed.Message(fields)), false)
              }

          case _ =>
            val valueParser =
              if (maybeField.exists(
                    _.getType.toProto == FieldDescriptorProto.Type.TYPE_STRING)) {
                StringEscapable
              } else
                NotComma

            val typeDescription = maybeField
              .map(field =>
                s"<${ProtobufFormat.typeName(field.getType.toProto, field.toProto.getTypeName)}>")
              .getOrElse("Invalid field name")

            token(
              OptSpace ~>
                valueParser.?,
              typeDescription
            ).map { v =>
              Parsed.Field(name, Seq(Parsed.Value(v)), false)
            }
        }
    }

}

object TypeFieldParsers {
  val Colon = token(":")
  val Comma = token(",")
  val NotComma = charClass(c => c != ',' && !c.isWhitespace).+.string

  lazy val FieldName =
    ID <~ Colon <~ OptSpace <~ OptNotSpace
}

object Parsed {
  sealed trait Descriptor

  case class Message(fields: Seq[Parsed.Field]) extends Descriptor

  case class Field(name: String, values: Seq[Descriptor], isRepeated: Boolean)

  case class Value(value: Option[String]) extends Descriptor

}
