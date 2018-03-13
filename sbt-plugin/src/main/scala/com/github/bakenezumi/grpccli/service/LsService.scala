package com.github.bakenezumi.grpccli.service

import com.github.bakenezumi.grpccli.ServiceListFormat
import com.github.bakenezumi.grpccli.protobuf.{ProtobufFormat, ServiceResolver}
import com.google.protobuf.DescriptorProtos.FileDescriptorSet
import com.google.protobuf.Descriptors.{MethodDescriptor, ServiceDescriptor}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

object LsService {

  def getList(fileDescriptorSet: FileDescriptorSet,
              serviceNameParameter: String = "",
              format: ServiceListFormat = ServiceListFormat.SHORT)(
      implicit executorContext: ExecutionContext): Seq[String] = {
    val serviceResolver =
      ServiceResolver.fromFileDescriptorSet(fileDescriptorSet)

    format match {
      // ls
      case ServiceListFormat.SHORT if serviceNameParameter.isEmpty =>
        serviceResolver.listServices.map(_.getFullName).toList

      // ls service
      case ServiceListFormat.SHORT =>
        val handler = separateHandler(
          serviceNameParameter,
          (serviceDescriptor) =>
            serviceDescriptor.getMethods.asScala.toList
              .map(_.getFullName),
          method => Seq(method.getName))
        handleServiceResolver(
          serviceNameParameter,
          serviceResolver,
          handler
        )

      // ls -l [service]
      case ServiceListFormat.LONG =>
        val handler = separateHandler(
          serviceNameParameter,
          serviceDescriptor =>
            Seq(
              ProtobufFormat.print(serviceDescriptor.getFile.toProto,
                                   serviceDescriptor.toProto)),
          method =>
            Seq(
              "  " + ProtobufFormat
                .print(method.toProto) + System.lineSeparator)
        )
        serviceResolver.listServices
          .map(_.getFullName)
          .flatMap { serviceName =>
            handleServiceResolver(
              serviceName,
              serviceResolver,
              handler
            )
          }
          .toList
    }
  }

  private def formatPackage(packageName: String): String =
    if (packageName.isEmpty) ""
    else packageName + "."

  private def separateHandler(serviceNameParameter: String,
                              serviceHandler: ServiceDescriptor => Seq[String],
                              methodHandler: MethodDescriptor => Seq[String])
    : ServiceDescriptor => Seq[String] =
    (serviceDescriptor: ServiceDescriptor) =>
      // handle service
      if (serviceNameParameter.isEmpty ||
          serviceNameParameter
            .endsWith(serviceDescriptor.getFullName))
        serviceHandler(serviceDescriptor)
      // handle methods
      else {
        val pkg = serviceDescriptor.getFile.getPackage
        serviceDescriptor.getMethods.asScala
          .find(method =>
            serviceNameParameter == formatPackage(pkg) + serviceDescriptor.getName + "." + method.getName ||
              serviceNameParameter == formatPackage(pkg) + serviceDescriptor.getName + "/" + method.getName)
          .map(methodHandler)
          .getOrElse(Nil)
    }

  private def handleServiceResolver[T](
      serviceName: String,
      serviceResolver: ServiceResolver,
      handler: (ServiceDescriptor => Seq[T])): Seq[T] = {
    serviceResolver.listServices
      .filter { service =>
        serviceName.startsWith(service.getFullName)
      }
      .flatMap { serviceDescriptor =>
        handler(serviceDescriptor)
      }
  }.toList

}
