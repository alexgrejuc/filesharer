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
    client.connect()

    command match {
      case "send" => client.sendFiles(files)
      case "request" => client.requestFiles(files)
      case "delete" => files.map(f => client.delete(f))
      case "list" => client.list()
      case _      => logInvalidArgs()
    }

    client.disconnect()
  }

  // This function is a hack that handles a bizarre issue that occurs on my machine
  // When I compile and then run scala prog.jar, all arguments following prog.jar are duplicated
  // e.g. scala fileserver.jar client send file.txt results in args = [client, send, file.txt, client, send, file.txt]
  // This issue does not occur if I interpret the same file or use sbt run
  def processArgs(args: Array[String]): Array[String] = {
    // drop the name of the binary
    var programArgs = args.drop(1)

    if (programArgs.length % 2 == 0) {
      if (programArgs.slice(0, programArgs.length / 2).sameElements(programArgs.slice(programArgs.length / 2, programArgs.length))) {
        programArgs = programArgs.slice(0, programArgs.length / 2)
      }
    }

    programArgs(0) = programArgs(0).toLowerCase()
    programArgs
  }
  
  def main(args: Array[String]) = {
    val processedArgs = processArgs(args)

    if (processedArgs.length == 1 && processedArgs(0) == "server") {
      val optionServer = Configurator.configureServerWith(serverConfigFile)

      optionServer match {
        case Some(s) => s.run()
        case _ => Utils.logError("Cannot run server without proper configuration. Terminating execution.")
      }
    }
    else if (processedArgs.length >= 2 && processedArgs(0) == "client") {
      val optionClient = Configurator.configureClientWith(clientConfigFile)

      optionClient match {
        case Some(client) => runClient(processedArgs(1).toLowerCase(), processedArgs.drop(2), client)
        case _ => Utils.logError("Cannot run client without proper configuration. Terminating execution.")
      }
    }
    else if (processedArgs.length == 1 && processedArgs(0) == "keygenerator") {
      KeyManager.generateAndStoreKey("AES", 128, clientConfigFile)
    }
    else{
      logInvalidArgs()
    }

    println()
  }
}
