package filesharer

import encryptor.Encryptor
import server.Server
import client.Client

object Main {
  def main(args: Array[String]) = {
    if (args.length == 1 && args(0).toLowerCase == "server") {
      Server.run()
    }
    else if (args.length == 3 && args(0).toLowerCase == "client") {
      val socket = Client.connect()

      val sendFileName = args(1).split('/').last
      val requestFileName = args(1).split('/').last

      Client.send(sendFileName, args(1), socket)
      Client.request(requestFileName, args(2), socket)

      Client.disconnect(socket)
    }
  }
}
