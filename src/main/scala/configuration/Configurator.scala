package filesharer
package configuration

import server.Server
import client.Client
import library.Utils

import keymanager.KeyManager

import java.io.File
import scala.io.{BufferedSource, Source}

/*
 * Quick and dirty methods for reading configuration information from a file
 * and creating Client or Server objects with it
 */
object Configurator {
  // Configures a server using a file that has the following info separated by newlines
  // control port
  // data port
  // storage path
  // key store path (for TLS control socket)
  // key store password (for TLS control socket)
  // <POSIX terminating newline>
  def configureServerWith(configurationFile: File): Option[Server] = {
    var file: BufferedSource = null

    try {
      file = Source.fromFile(configurationFile)
      val configs = file.getLines.toList

      // 5 lines of info + POSIX newline
      val serverConfigLength = 6

      if (configs.length == serverConfigLength) {
        val controlPort = configs(0).toInt
        val dataPort = configs(1).toInt
        val storagePath = configs(2)
        val keyStorePath = configs(3)
        val keyStorePassword = configs(4)

        Some(Server(controlPort, dataPort, storagePath, keyStorePath, keyStorePassword))
      }
      else {
        Utils.log(s"Server configuration file must contain exactly ${serverConfigLength - 1} lines followed by a newline.")
        None
      }
    } catch {
      case ex: Exception => Utils.logError(s"Error reading server configuration: ${ex.getMessage}")
        None
    } finally {
      Utils.tryClose(file)
    }
  }

  // Configures a client using a file that has the following info separated by newlines
  // key path (for locally encrypting client's files)
  // host name
  // control port
  // data port
  // trust store path (for TLS control socket)
  // trust store password (for TLS control socket)
  // <POSIX terminating newline>
  def configureClientWith(configurationFile: File): Option[Client] = {
    var file: BufferedSource = null

    try {
      file = Source.fromFile(configurationFile)
      val configs = file.getLines.toList

      // 6 lines of info + POSIX newline
      val clientConfigLength = 7
      if (configs.length == clientConfigLength) {
        val keyPath = configs(0)
        val hostName = configs(1)
        val controlPort = configs(2).toInt
        val dataPort = configs(3).toInt
        val trustStorePath = configs(4)
        val trustStorePassword = configs(5)

        val optionKey = KeyManager.readKey(new File(keyPath))

        optionKey match {
          case Some(key) => Some(Client(hostName, controlPort, dataPort, trustStorePath, trustStorePassword, key))
          case _ =>
            Utils.logError("Cannot configure client without key.")
            None
        }

      }
      else {
        Utils.log(s"Client configuration file must contain exactly ${clientConfigLength - 1} lines followed by a newline.")
        None
      }
    } catch {
      case ex: Exception => Utils.logError(s"Error reading server configuration: ${ex.getMessage}")
        None
    } finally {
      Utils.tryClose(file)
    }
  }
}
