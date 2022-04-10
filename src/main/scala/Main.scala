package filesharer

import filesharer.encryptor.Encryptor

object Main {
  // assuming 3 command line arguments `f` `e` `d`:
  // saves encrypted version of file `f` to `e` and decrypted version to `d`
  def main(args: Array[String]) = {
    Encryptor.encryptFileTo(args(0), args(1))
    Encryptor.decryptFileTo(args(1), args(2))
  }
}
