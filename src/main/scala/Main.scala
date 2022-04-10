package filesharer

import encryptor.Encryptor
import server.Server
import client.Client

object Main {
  // assuming 3 command line arguments `f` `e` `d`:
  // saves encrypted version of file `f` to `e` and decrypted version to `d`
  def main(args: Array[String]) = {
    if (args.length == 1 && args(0).toLowerCase == "server") {
      Server.run()
    }
    else if (args.length == 2 && args(0).toLowerCase == "client") {
      Client.send(args(1))
    }
  }
}
