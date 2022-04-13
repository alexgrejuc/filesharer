package filesharer
package server

import java.io.{BufferedInputStream, BufferedOutputStream, Closeable, DataInputStream, DataOutputStream, File, FileInputStream, FileOutputStream, IOException, InputStream, ObjectOutputStream, OutputStream, PrintStream}
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
    val in = new DataInputStream(new BufferedInputStream(dataSocket.getInputStream))

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

  def run(): Unit = {
    var control: ServerSocket = null
    var data: ServerSocket = null
    var controlSocket: Socket = null

    try {
      control = new ServerSocket(Utils.controlPort)
      data = new ServerSocket(Utils.dataPort)

      Utils.log("Server running")

      controlSocket = control.accept()

      val cis = new DataInputStream(new BufferedInputStream(controlSocket.getInputStream))
      val cos = new DataOutputStream(new BufferedOutputStream(controlSocket.getOutputStream))

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
    } catch {
      case io: IOException =>
        io.printStackTrace
        Utils.logError(s"Error connecting: ${io.getMessage}")
      case er: Exception =>
        er.printStackTrace
        Utils.logError(s"Unknown error: ${er.getMessage}")
    }
    finally {
      List(controlSocket, control, data).map(r => Utils.tryClose(r))
    }

    Utils.log("Server stopping execution")
  }
}
