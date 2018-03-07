package com.github.bakenezumi.grpccli

import java.io.{BufferedReader, IOException, InputStreamReader}
import java.nio.file.{Files, Path}

import com.google.common.base.Strings
import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.DynamicMessage
import com.google.protobuf.util.JsonFormat
import com.google.protobuf.util.JsonFormat.TypeRegistry

import scala.collection.mutable.ArrayBuffer

object MessageReader {

  /** Creates a [[com.github.bakenezumi.grpccli.MessageReader MessageReader]] which reads messages from stdin. */
  def forStdin(descriptor: Descriptor,
               registry: JsonFormat.TypeRegistry): MessageReader = {
    val reader = new BufferedReader(new InputStreamReader(System.in))
    new MessageReader(JsonFormat.parser.usingTypeRegistry(registry),
                      descriptor,
                      reader,
                      "STDIN")
  }

  /** Creates a [[com.github.bakenezumi.grpccli.MessageReader MessageReader]] which reads the messages from a file. */
  def forFile(path: Path, descriptor: Descriptor): MessageReader =
    forFile(path, descriptor, TypeRegistry.getEmptyTypeRegistry)

  def forFile(path: Path,
              descriptor: Descriptor,
              registry: JsonFormat.TypeRegistry): MessageReader =
    try new MessageReader(JsonFormat.parser.usingTypeRegistry(registry),
                          descriptor,
                          Files.newBufferedReader(path),
                          path.toString)
    catch {
      case e: IOException =>
        throw new IllegalArgumentException(
          "Unable to read file: " + path.toString,
          e)
    }

}

class MessageReader private (jsonParser: JsonFormat.Parser,
                             descriptor: Descriptor,
                             bufferedReader: BufferedReader,
                             source: String) {

  /** Parses all the messages and returns them in a list. */
  def read: List[DynamicMessage] = {
    val resultBuilder = ArrayBuffer.newBuilder[DynamicMessage]
    try {
      var line: String = null
      var wasLastLineEmpty = false
      println("reading request message from stdin...")
      while (true) {
        line = bufferedReader.readLine
        // Two consecutive empty lines mark the end of the stream.
        if (Strings.isNullOrEmpty(line)) {
          if (wasLastLineEmpty) return resultBuilder.result().toList
          wasLastLineEmpty = true
        } else {
          wasLastLineEmpty = false
          // Read the next full message.
          val stringBuilder = ArrayBuffer.newBuilder[String]
          while (!Strings.isNullOrEmpty(line)) {
            stringBuilder += removeLastComma(line)
            line = bufferedReader.readLine
          }
          wasLastLineEmpty = true
          val nextMessage = DynamicMessage.newBuilder(descriptor)
          jsonParser.merge(
            addBrackets(
              stringBuilder.result().filter(_.nonEmpty).mkString(",")),
            nextMessage)
          // Clean up and prepare for next message.
          resultBuilder += nextMessage.build
        }
      }
      throw new RuntimeException("never reach")
    } catch {
      case e: Exception =>
        throw new IllegalArgumentException(
          "Unable to read messages from: " + source,
          e)
    }
  }

  private def removeLastComma(line: String): String = {
    val trimmed = line.trim
    if (trimmed.nonEmpty && trimmed.last == ',') trimmed.init else trimmed
  }

  private def addBrackets(message: String): String = {
    if (message.head != '{' && message.last != '}') s"{$message}"
    else message
  }

}
