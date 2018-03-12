package com.github.bakenezumi.grpccli.protobuf

class ProtoMethodName private (val packageName: String,
                               val serviceName: String,
                               val methodName: String) {

  import com.google.common.base.Joiner

  def getFullServiceName: String = Joiner.on(".").join(packageName, serviceName)
}

object ProtoMethodName {

  def parseFullGrpcMethodName(fullMethodName: String): ProtoMethodName = {
    // Ask grpc for the service name.
    val fullServiceName =
      io.grpc.MethodDescriptor.extractFullServiceName(fullMethodName)
    if (fullServiceName == null)
      throw new IllegalArgumentException(
        "Could not extract full service from " + fullMethodName)
    // Make sure there is a '/' and use the rest as the method name.
    val serviceLength = fullServiceName.length
    if (serviceLength + 1 >= fullMethodName.length || fullMethodName.charAt(
          serviceLength) != '/')
      throw new IllegalArgumentException(
        "Could not extract method name from " + fullMethodName)
    val methodName = fullMethodName.substring(fullServiceName.length + 1)
    // Extract the leading package from the service name.
    val index = fullServiceName.lastIndexOf('.')
    if (index == -1)
      throw new IllegalArgumentException(
        "Could not extract package name from " + fullServiceName)
    val packageName = fullServiceName.substring(0, index)
    // Make sure there is a '.' and use the rest as the service name.
    if (index + 1 >= fullServiceName.length || fullServiceName.charAt(index) != '.')
      throw new IllegalArgumentException(
        "Could not extract service from " + fullServiceName)
    val serviceName = fullServiceName.substring(index + 1)
    new ProtoMethodName(packageName, serviceName, methodName)
  }

}
