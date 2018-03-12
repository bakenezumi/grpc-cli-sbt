package com.github.bakenezumi.grpccli.protobuf

import java.io.{ByteArrayOutputStream, IOException, PrintStream}
import java.nio.file._
import java.util.logging.Logger

import com.github.bakenezumi.grpccli.GrpcClient
import com.github.os72.protocjar.Protoc
import com.google.protobuf.DescriptorProtos
import com.google.protobuf.DescriptorProtos.FileDescriptorSet

import scala.collection.mutable.ListBuffer

// Copy from[[https://github.com/grpc-ecosystem/polyglot/blob/v1.6.0/src/main/java/me/dinowernli/grpc/polyglot/protobuf/ProtocInvoker.java]]
class ProtocInvoker(discoveryRoot: Path, protocIncludePaths: Seq[Path]) {

  private val logger =
    Logger.getLogger(classOf[GrpcClient].getName)
  private val PROTO_MATCHER =
    FileSystems.getDefault.getPathMatcher("glob:**/*.proto")

  /**
    * Executes protoc on all .proto files in the subtree rooted at the supplied path and returns a
    * `FileDescriptorSet` which describes all the protos.
    */
  @throws[ProtocInvocationException]
  def invoke: FileDescriptorSet = {
    val wellKnownTypesInclude = try setupWellKnownTypes
    catch {
      case e: IOException =>
        throw new ProtocInvocationException(
          "Unable to extract well known types",
          e)
    }
    val descriptorPath =
      try Files.createTempFile("descriptor", ".pb.bin")
      catch {
        case e: IOException =>
          throw new ProtocInvocationException("Unable to create temporary file",
                                              e)
      }
    val builder = ListBuffer.newBuilder[String]
    builder ++= scanProtoFiles(discoveryRoot)
    builder ++= includePathArgs(wellKnownTypesInclude)
    builder += "--descriptor_set_out=" + descriptorPath.toAbsolutePath
    builder += "--include_imports"
    invokeBinary(builder.result())

    try FileDescriptorSet.parseFrom(Files.readAllBytes(descriptorPath))
    catch {
      case e: IOException =>
        throw new ProtocInvocationException(
          "Unable to parse the generated descriptors",
          e)
    }
  }

  private def includePathArgs(wellKnownTypesInclude: Path): Seq[String] = {
    val resultBuilder = ListBuffer.newBuilder[String]
    protocIncludePaths.foreach(path => resultBuilder += "-I" + path.toString)
    // Add the include path which makes sure that protoc finds the well known types. Note that we
    // add this *after* the user types above in case users want to provide their own well known
    // types.
    resultBuilder += "-I" + wellKnownTypesInclude.toString
    // Protoc requires that all files being compiled are present in the subtree rooted at one of
    // the import paths (or the proto_root argument, which we don't use). Therefore, the safest
    // thing to do is to add the discovery path itself as the *last* include.
    resultBuilder += "-I" + discoveryRoot.toAbsolutePath.toString
    resultBuilder.result().toList
  }

  @throws[ProtocInvocationException]
  private def invokeBinary(protocArgs: Seq[String]): Unit = {
    var status = 0

    // The "protoc" library unconditionally writes to stdout. So, we replace stdout right before
    // calling into the library in order to gather its output.
    val stdoutBackup = System.out
    val protocLogLines = try {
      val protocStdout = new ByteArrayOutputStream
      System.setOut(new PrintStream(protocStdout))
      status = Protoc.runProtoc(protocArgs.toArray)
      protocStdout.toString.split("\n")
    } catch {
      case e @ (_: IOException | _: InterruptedException) =>
        throw new ProtocInvocationException("Unable to execute protoc binary",
                                            e)
    } finally {
      // Restore stdout.
      System.setOut(stdoutBackup)
    }
    if (status != 0) { // If protoc failed, we dump its output as a warning.
      logger.warning("Protoc invocation failed with status: " + status)
      for (line <- protocLogLines) {
        logger.warning("[Protoc log] " + line)
      }
      throw new ProtocInvocationException(
        s"Got exit code [$status] from protoc with args [$protocArgs]")
    }
  }

  @throws[ProtocInvocationException]
  private def scanProtoFiles(protoRoot: Path): Seq[String] =
    try {
      val builder = ListBuffer.newBuilder[String]
      Files
        .walk(protoRoot)
        .filter((path) => PROTO_MATCHER.matches(path))
        .forEach((path) => builder += path.toAbsolutePath.toString)
      builder.result()
    } catch {
      case e: IOException =>
        throw new ProtocInvocationException(
          "Unable to scan proto tree for files",
          e)
    }

  /**
    * Extracts the .proto files for the well-known-types into a directory and returns a proto
    * include path which can be used to point protoc to the files.
    */
  @throws[IOException]
  private def setupWellKnownTypes: Path = {
    val tmpdir = Files.createTempDirectory("grpccli-sbt-well-known-types")
    val protoDir =
      Files.createDirectories(Paths.get(tmpdir.toString, "google", "protobuf"))
    for (file <- WellKnownTypes.fileNames) {
      Files.copy(
        classOf[ProtocInvoker].getResourceAsStream("/google/protobuf/" + file),
        Paths.get(protoDir.toString, file))
    }
    tmpdir
  }

}

/** An error indicating that something went wrong while invoking protoc. */
@SerialVersionUID(1L)
class ProtocInvocationException(message: String) extends Exception(message) {
  def this(message: String, cause: Throwable) = {
    this(message)
    initCause(cause)
  }
}

import com.google.protobuf.DescriptorProtos.DescriptorProto
import com.google.protobuf.{
  AnyProto,
  ApiProto,
  DurationProto,
  EmptyProto,
  FieldMaskProto,
  SourceContextProto,
  StructProto,
  TimestampProto,
  TypeProto,
  WrappersProto
}

object WellKnownTypes {
  private val DESCRIPTORS = Set(
    AnyProto.getDescriptor.getFile.toProto,
    ApiProto.getDescriptor.getFile.toProto,
    DescriptorProto.getDescriptor.getFile.toProto,
    DurationProto.getDescriptor.getFile.toProto,
    EmptyProto.getDescriptor.getFile.toProto,
    FieldMaskProto.getDescriptor.getFile.toProto,
    SourceContextProto.getDescriptor.getFile.toProto,
    StructProto.getDescriptor.getFile.toProto,
    TimestampProto.getDescriptor.getFile.toProto,
    TypeProto.getDescriptor.getFile.toProto,
    WrappersProto.getDescriptor.getFile.toProto
  )
  private val FILES = Set(
    "any.proto",
    "api.proto",
    "descriptor.proto",
    "duration.proto",
    "empty.proto",
    "field_mask.proto",
    "source_context.proto",
    "struct.proto",
    "timestamp.proto",
    "type.proto",
    "wrappers.proto"
  )

  def descriptors: Set[DescriptorProtos.FileDescriptorProto] =
    DESCRIPTORS

  def fileNames: Set[String] = FILES
}
