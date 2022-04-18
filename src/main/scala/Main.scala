package filesharer

import encryptor.Encryptor
import server.Server
import client.Client
import keymanager.KeyManager
import library.Utils

import java.io.{BufferedOutputStream, File, FileInputStream, FileOutputStream, IOException}
import java.net.Socket
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

object Main {
  def logInvalidArgs(): Unit ={
    Utils.logError("Invalid command line arguments:")
  }

  def runClient(args: Array[String], key: Array[Byte]): Unit = {
    val client = new Client("localhost", Utils.controlPort, Utils.dataPort, "/home/grejuca/IdeaProjects/FileSharer/keystore", "passphrase", key)
    client.connect()

    args(1).toLowerCase match {
      case "send" => client.sendFiles(args.drop(2))
      case _      => logInvalidArgs()
    }

    client.disconnect()
  }

  def main(args: Array[String]) = {
    println()
    val keyFile = new File("/home/grejuca/IdeaProjects/FileSharer/client/key/key")

    if (args.length == 1 && args(0).toLowerCase() == "server") {
      val server = new Server(Utils.controlPort, Utils.dataPort, "testfiles/server/", "/home/grejuca/IdeaProjects/FileSharer/keystore", "passphrase")
      server.run()
    }
    else if (args.length >= 3 && args(0).toLowerCase() == "client") {
      val keyOption = KeyManager.readKey(keyFile)

      keyOption match {
        case Some(key) => runClient(args, key)
        case _ => Utils.logError("Client cannot run without key. Terminating.")
      }
    }
    else if (args.length == 1 && args(0).toLowerCase() == "keygenerator") {
      KeyManager.generateAndStoreKey("AES", 128, keyFile)
    }
    else{
      logInvalidArgs()
    }

    println()
  }
}
