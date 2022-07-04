package filesharer

import encryptor.Encryptor
import server.Server
import client.Client
import keymanager.KeyManager
import library.Utils
import configuration.Configurator

import java.io.{BufferedOutputStream, File, FileInputStream, FileOutputStream, IOException}
import java.net.Socket
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/*
 * The entry point for the FileSharer application
 * Processes command line arguments, reads from configuration files, and runs a Client, Server, or KeyManager
*/
object Main {
  val clientConfigFile = new File("client" + File.separator + "config" + File.separator + "config")
  val serverConfigFile = new File("server" + File.separator + "config" + File.separator + "config")

  def logInvalidArgs(): Unit = {
    Utils.logError("Invalid command line arguments")
  }

  def runClient(command: String, files: Array[String], client: Client): Unit = {
    val connected = client.connect()

    if (connected) {
      command match {
        case "send" => client.sendFiles(files)
        case "request" => client.requestFiles(files)
        case "delete" => files.map(f => client.delete(f))
        case "list" => client.list()
        case _ => logInvalidArgs()
      }

      client.disconnect()
    }
  }

  def main(args: Array[String]) = {
    if (args.length == 1 && args(0).toLowerCase == "server") {
      val optionServer = Configurator.configureServerWith(serverConfigFile)

      optionServer match {
        case Some(s) => s.run()
        case _ => Utils.logError("Cannot run server without proper configuration. Terminating execution.")
      }
    }
    else if (args.length >= 2 && args(0).toLowerCase == "client") {
      val optionClient = Configurator.configureClientWith(clientConfigFile)

      optionClient match {
        case Some(client) => runClient(args(1).toLowerCase, args.drop(2), client)
        case _ => Utils.logError("Cannot run client without proper configuration. Terminating execution.")
      }
    }
    else if (args.length == 1 && args(0).toLowerCase == "keygenerator") {
      KeyManager.generateAndStoreKey("AES", 128, clientConfigFile)
    }
    else{
      logInvalidArgs()
    }

    println()
  }
}
