package filesharer
package server

import java.io.{BufferedInputStream, BufferedOutputStream, DataInputStream, DataOutputStream, File, FileInputStream, FileOutputStream, InputStream, ObjectOutputStream, OutputStream, PrintStream}
import java.net.{ServerSocket, Socket}
import scala.io.BufferedSource
import library.Utils

object Server {
  // TODO: get from config file
  val storagePath = "testfiles/server/"

  def readInfo(in: InputStream): String = {
    val dis = new DataInputStream(in)
    dis.readUTF()
  }

  def receive(dataSS: ServerSocket, out: DataOutputStream): Unit = {
    val dataSocket = dataSS.accept()
    val in = new DataInputStream(new BufferedInputStream(dataSocket.getInputStream()))

    Utils.log("Server waiting to receive")
    Utils.wait(in)

    val name = readInfo(in)

    Utils.log(s"Server receiving file named $name")

    val receiveFile = new File(storagePath + name)
    val fos = new FileOutputStream(receiveFile)

    Utils.log("Server opened file")

    Utils.wait(in)

    Utils.log("Server received data")

    val lengthRead = in.transferTo(fos)

    fos.close()

    Utils.log("Server wrote to file")

    // tell client the data was properly received
    out.writeLong(lengthRead)
    out.flush()

    Utils.log("Server notified client of success")
    in.close()
    dataSocket.close()
  }

  def disconnect(s: Socket): Unit = {
    s.close()
    Utils.log("Client disconnected")
  }

  def run(): Unit = {
    val control = new ServerSocket(Utils.controlPort)
    val data = new ServerSocket(Utils.dataPort)

    Utils.log("Server running")

    val controlSocket = control.accept()

    val cis = new DataInputStream(new BufferedInputStream(controlSocket.getInputStream()))
    val cos = new DataOutputStream(new BufferedOutputStream(controlSocket.getOutputStream()))

    Utils.log("Server accepted a client")

    var clientConnected = true

    while (clientConnected) {
      Utils.log("\nServer waiting on client command")
      Utils.wait(cis)

      val mode = cis.readInt()

      Utils.log(s"Server received mode $mode from client")

      mode match {
        case 0 => receive(data, cos)
        case 3 => clientConnected = false
        case _ => Utils.logError("Unknown command received from client")
      }
    }

    controlSocket.close()
    control.close()
    data.close()
    Utils.log("Server stopping execution")
  }
}
