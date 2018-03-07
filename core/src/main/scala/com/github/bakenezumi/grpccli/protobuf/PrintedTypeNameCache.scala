package com.github.bakenezumi.grpccli.protobuf

import scala.collection.mutable

/** Cache a printed type name */
object PrintedTypeNameCache {

  private[this] val cache: mutable.HashSet[String] =
    collection.mutable.HashSet[String]()

  private[protobuf] def put(name: String): Boolean = cache.add(name)

  def getAll: List[String] = cache.toList

  def clear(): Unit = cache.clear()

}
