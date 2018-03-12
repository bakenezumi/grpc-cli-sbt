package com.github.bakenezumi.grpccli.protobuf

import com.google.protobuf.DescriptorProtos.{
  FileDescriptorProto,
  FileDescriptorSet
}
import com.google.protobuf.Descriptors.{
  Descriptor,
  FileDescriptor,
  MethodDescriptor,
  ServiceDescriptor
}

import scala.collection.JavaConverters._

/** [[https://github.com/grpc-ecosystem/polyglot/blob/master/src/main/java/me/dinowernli/grpc/polyglot/protobuf/ServiceResolver.java]] */
object ServiceResolver {

  /** Creates a resolver which searches the supplied `FileDescriptorSet`. */
  def fromFileDescriptorSet(
      descriptorSet: FileDescriptorSet): ServiceResolver = {
    val descriptorProtoIndex = computeDescriptorProtoIndex(descriptorSet)
    val descriptorCache = Map[String, FileDescriptor]()
    val result = descriptorSet.getFileList.asScala
      .map(
        descriptorProto =>
          descriptorFromProto(descriptorProto,
                              descriptorProtoIndex,
                              descriptorCache))
      .toList

    new ServiceResolver(result)
  }

  /**
    * Returns a map from descriptor proto name as found inside the descriptors to protos.
    */
  private def computeDescriptorProtoIndex(fileDescriptorSet: FileDescriptorSet)
    : Map[String, FileDescriptorProto] = {
    fileDescriptorSet.getFileList.asScala
      .map(descriptorProto => descriptorProto.getName -> descriptorProto)
      .toMap
  }

  /**
    * Recursively constructs file descriptors for all dependencies of the supplied proto and returns
    * a `FileDescriptor` for the supplied proto itself. For maximal efficiency, reuse the
    * descriptorCache argument across calls.
    */
  private def descriptorFromProto(
      descriptorProto: FileDescriptorProto,
      descriptorProtoIndex: Map[String, FileDescriptorProto],
      descriptorCache: Map[String, FileDescriptor])
    : FileDescriptor = { // First, check the cache.
    val descriptorName = descriptorProto.getName
    if (descriptorCache.contains(descriptorName))
      return descriptorCache(descriptorName)
    // Then, fetch all the required dependencies recursively.
    val dependencies = descriptorProto.getDependencyList.asScala.map(
      dependencyName =>
        if (!descriptorProtoIndex.contains(dependencyName))
          throw new IllegalArgumentException(
            "Could not find dependency: " + dependencyName)
        else {
          val dependencyProto = descriptorProtoIndex(dependencyName)
          descriptorFromProto(dependencyProto,
                              descriptorProtoIndex,
                              descriptorCache)
      })
    // Finally, construct the actual descriptor.
    FileDescriptor.buildFrom(descriptorProto, dependencies.toArray)
  }

}

class ServiceResolver private (fileDescriptors: List[FileDescriptor]) {

//  private val logger = Logger.getLogger(classOf[ServiceResolver].getName)

  /** Lists all of the services found in the file descriptors */
  def listServices: Iterable[ServiceDescriptor] = {
    fileDescriptors.flatMap(_.getServices.asScala)
  }

  /** Lists all the known message types. */
  def listMessageTypes: Set[Descriptor] = {
    fileDescriptors.flatMap(_.getMessageTypes.asScala).toSet
  }

  /**
    * Returns the descriptor of a protobuf method with the supplied grpc method name. If the method
    * cannot be found, this throws `java.lang.IllegalArgumentException`.
    */
  def resolveServiceMethod(method: ProtoMethodName): MethodDescriptor =
    resolveServiceMethod(method.serviceName,
                         method.methodName,
                         method.packageName)

  private def resolveServiceMethod(serviceName: String,
                                   methodName: String,
                                   packageName: String) = {
    val service = findService(serviceName, packageName)
    val method = service.findMethodByName(methodName)
    if (method == null)
      throw new IllegalArgumentException(
        "Unable to find method " + methodName + " in service " + serviceName)
    method
  }

  private def findService(serviceName: String, packageName: String)
    : ServiceDescriptor = { // TODO(dino): Consider creating an index.
    fileDescriptors
      .collectFirst {
        case fileDescriptor
            if fileDescriptor.getPackage.equals(packageName) => // Package does not match this file, ignore.
          fileDescriptor
            .findServiceByName(serviceName)
      }
      .filter(_ != null)
      .getOrElse(
        throw new IllegalArgumentException(
          "Unable to find service with name: " + serviceName)
      )
  }

}
