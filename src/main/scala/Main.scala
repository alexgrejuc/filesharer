package filesharer

import encryptor.Encryptor
import server.Server
import client.Client
import library.Utils

import java.io.{File, FileInputStream}
import java.net.Socket

object Main {
  def sendFiles(filePaths: Array[String], clientControl: Socket): Unit = {
    for(fp <- filePaths){
      val f = new File(fp)
      if (f.exists() && !f.isDirectory() && f.canRead()) {
        Client.send(f, clientControl, Utils.hostName, Utils.dataPort)
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
      Server.run()
    }
    else if (args.length >= 3 && args(0).toLowerCase() == "client") {
      val control = Client.connect(Utils.hostName, Utils.controlPort)

      args(1).toLowerCase match {
        case "send" => sendFiles(args.drop(2), control)
        case _      => logInvalidArgs()
      }

      Client.disconnect(control)
    }
    else{
      logInvalidArgs()
    }

    println()
  }
}
