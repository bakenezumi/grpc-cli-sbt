package com.github.bakenezumi.grpccli.testing

import java.io.File
import java.nio.file.{Path, Paths}

/** Utilities shared across tests.
  *
  *  [[https://github.com/grpc-ecosystem/polyglot/blob/v1.6.0/src/main/java/me/dinowernli/grpc/polyglot/testing/TestUtils.java]]
  */
object TestUtils {

  /** The root directory of the certificates we use for testing. */
  val TESTING_CERTS_DIR: Path = Paths.get(getWorkspaceRoot.toString,
                                          "mock-server",
                                          "src",
                                          "main",
                                          "scala",
                                          "com",
                                          "github",
                                          "bakenezumi",
                                          "grpccli",
                                          "testing",
                                          "certificates")

  /** Returns the root directory of the runtime workspace. */
  def getWorkspaceRoot
    : Path = { // Bazel runs binaries with the workspace root as working directory.
    Paths.get(".").toAbsolutePath
  }

  /** Returns a file containing a root CA certificate for use in tests. */
  def loadRootCaCert: File =
    Paths.get(TESTING_CERTS_DIR.toString, "ca.pem").toFile

  /** Returns a file containing a client certificate for use in tests. */
  def loadClientCert: File =
    Paths.get(TESTING_CERTS_DIR.toString, "client.pem").toFile

  /** Returns a file containing a client key for use in tests. */
  def loadClientKey: File =
    Paths.get(TESTING_CERTS_DIR.toString, "client.key").toFile

  /** Returns a file containing a certificate chain from our testing root CA to our server. */
  def loadServerChainCert: File =
    Paths.get(TESTING_CERTS_DIR.toString, "server.pem").toFile

  /** Returns a file containing the key pair of our server. */
  def loadServerKey: File =
    Paths.get(TESTING_CERTS_DIR.toString, "server.key").toFile
}
