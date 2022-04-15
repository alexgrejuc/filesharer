package filesharer

import encryptor.Encryptor
import server.Server
import client.Client
import library.Utils

import java.io.{File, FileInputStream}
import java.net.Socket

object Main {
  def sendFiles(filePaths: Array[String], client: Client): Unit = {
    for(fp <- filePaths){
      val f = new File(fp)
      if (f.exists() && !f.isDirectory() && f.canRead()) {
        client.send(f)
      }
      else {
        Utils.logError(s"Could not access file $fp")
      }
      Utils.log("")
    }
  }

  def logInvalidArgs(): Unit ={
    Utils.logError("Invalid command line arguments:")
  }

  def main(args: Array[String]) = {
    println()

    if (args.length == 1 && args(0).toLowerCase() == "server") {
      val server = new Server(Utils.controlPort, Utils.dataPort, "testfiles/server/", "/home/grejuca/IdeaProjects/FileSharer/keystore", "passphrase")
      server.run()
    }
    else if (args.length >= 3 && args(0).toLowerCase() == "client") {
      val client = new Client("localhost", Utils.controlPort, Utils.dataPort, "/home/grejuca/IdeaProjects/FileSharer/keystore", "passphrase")
      client.connect()

      args(1).toLowerCase match {
        case "send" => sendFiles(args.drop(2), client)
        case _      => logInvalidArgs()
      }

      client.disconnect()
    }
    else{
      logInvalidArgs()
    }

    println()
  }
}
