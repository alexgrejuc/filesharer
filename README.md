# FileSharer

A command line interface application that enables clients to locally encrypt files, store them on a server, and later retrieve and decrypt them. It uses a custom application-level protocol on top of TCP/IP and depends only on the Java and Scala standard libraries.

The design borrows some ideas from FTP, with both the client and server using separate data and control ports. As in [FTP streaming mode](https://www.w3.org/Protocols/rfc959/3_DataTransfer.html), the data port is closed after a file is sent over it. Both the client and server make use of streams for their data transfer operations, so there are no file size limitations.

Note: I used Scala for the first time while working on this project and the code is likely far from idiomatic.

![An example run of the app](documentation/img/execution-example.png)

# Command Line Instructions

Note: these steps were only tested on a single machine running Ubuntu 20.04. They should work on other machines with JDK 11.0.014 and Scala 3.1.1, however.

A `.jar` is included in this repository, so the app can be used directly with `scala`, although `sbt` can be used to build it if desired.

Assumptions: you have cloned the repository and you are in the top-level `filesharer` directory.

### Building the application (optional)

    sbt clean compile package
    cp target/scala-3.1.1/filesharer_3-0.jar working-directory/filesharer.jar

Afterwards, to use the application, you must be in the working-directory (it contains configuration files and directories needed to run the app):

    cd working-directory

### Running the server

    scala filesharer.jar server

This should produce an output similar to:

    Server running with control port 9999, data port 9998
    Server storing files at server/storage, and keystore at server/config/keystore

Enter `CTRL+C` to stop the server.

### Using the client

The client accepts four commands: `send`, `list`, `request`, and `delete`. They all require the server to be running and are described below:

#### send

The syntax for sending files is: `scala filesharer.jar client send <file name>*`. For example:

    scala filesharer.jar client send client/storage/original/test.txt client/storage/original/test.png

With the the configuration in [working-directory/server/config/config](working-directory/server/config/config), the encrypted version of these files will end up in [working-directory/server/storage](working-directory/server/storage).

#### list

To list files stored on the server:

    scala filesharer.jar client list

Assuming the previous command sent test.txt and test.png, the output should be:

    Client connected to server at localhost with control port 9999 and data port 9998
    Using trust store at client/config/truststore

    Client requesting list of saved files.

    File Name | Size (bytes)
    -------------------------------
    test.png  | 277792
    test.txt  | 48

    Client disconnected

#### request

The syntax for requesting files back from the server is: `scala filesharer.jar client request (<file name> <path to save file to>)+`. For example:

    scala filesharer.jar client request test.txt client/storage/decrypted/test.txt test.png client/storage/decrypted/test.png

This will request the files that were sent with the previous `send` command, decrypt them, and store them at the specified path.

#### delete

The syntax for deleting files on the server side is: `scala filesharer.jar client delete <file name>*`. For example:

    scala filesharer.jar client delete test.txt test.png

## Generating a new key for the client (optional)

The working-directory already contains an AES-128 key at [working-directory/client/config/key](working-directory/client/config/key). However, if desired, a new one may be created.

First, delete or rename the old key:

    mv client/config/key client/config/oldkey

Then create a new one with:

    scala filesharer.jar keygenerator

# Configuration Details

The above instructions require running the application from `working-directory`, although this can be changed by modifying the configuration files. The only restriction is that the server must be run from a directory that contains [server/config/config](working-directory/server/config/config) and the client must be run from a directory that contains [client/config/config](working-directory/client/config/config). Look in [src/main/scala/configuration/Configurator.scala](src/main/scala/configuration/Configurator.scala) for the format of these files.

With the provided configuration, the server will store its files in [working-directory/server/storage](working-directory/server/storage).

Although the above examples send client files from [working-directory/client/storage/original](working-directory/client/storage/original) and store files in [working-directory/client/storage/decrypted](working-directory/client/storage/decrypted), this is only a convention. There is no configuration to these directories and clients may store and send files from any valid path.

# Listing of Scripts

The following bash scripts may be useful for evaluation the application, although they are not necessary for doing so:

1. [setup.sh](setup.sh) - creates the working-directory structure (without the gitkeep directory).
2. [working-directory/test.sh](working-directory/test.sh) - runs the app in typical workflows. 
3. [working-directory/bad-test.sh](working-directory/bad-test.sh) - runs some tests of the app in atypical/incorrect workflows.
4. [working-directory/clean.sh](working-directory/clean.sh) - removes server files, client's decrypted files, and server logs produced by the above tests.

# Design Overview

The image below shows the architecture of the application. It consists of a client and server that exchange commands and metadata over a control socket and encrypted files over a data socket. Both the client and server store and retrieve data from their file systems. The client uses AES-128 in CBC mode with a locally stored key to encrypt and decrypt its files as well as a SHA-256 hash to verify correct data transmission and integrity. The application also includes a key manager, which allows users to generate and store a key.


![File Sharer Architecture](documentation/img/file-sharer-architecture.png)

# Protocol

The image below shows a high level overview of the application-level protocol. Each operation begins with a TLS 1.3 handshake over the control socket. Once a client has authenticated the server, it sends encrypted commands and metadata over the control socket and locally-encrypted files over the data socket. Note that multiple command or metadata messages may be sent before or after the data transmission (shown in the send protocol diagram, which follows this one).

![High Level Protocol Overview](documentation/img/high-level-protocol.png)

The image below shows the protocol in more detail, for the send command. Over the control socket, the client notifies the server it has data to send and it tells it the name of the file it is sending. Then, it connects the data socket, sends the file over it, and closes that socket. To ensure that the server has received all data and to ensure that it has not been tampered with by a machine in the middle, the server sends a hash of the encrypted file over the control socket. The client then checks that this hash matches what it sent and notifices the user of the result before disconnecting.

![Client-Server Interaction for Sending a File](documentation/img/send-protocol.png)

# Design Choices

## Data Encryption

I chose AES 128 in CBC mode. Additionally, I chose to hash the encrypted data with SHA256 for data integrity.

I chose AES since it is an industry standard in symmetric key encryption, which I chose because only the client needs to encrypt and decrypt the data. I chose a 128 bit key because it is secure [(at least through 2030 according to NIST)](https://nvlpubs.nist.gov/nistpubs/SpecialPublications/NIST.SP.800-57pt1r5.pdf) and faster than a larger key size. Although CBC is susceptible to padding oracle attacks, this is not a problem in this instance since the client is the only one decrypting data and it does not share the result of this process with anyone. Since CBC does not ensure message integrity, the protocol requires a hash of the encrypted data to be sent over the secure control socket, which allows the receiver to compare hashes and ensure that all data has been transmitted and has not been tampered with.

GCM mode is a also viable choice and has the benefit of providing security as well as integrity. However, given that the files are sent over the unauthenticated data socket, a MAC would sent over the control socket would still be needed to avoid a machine in the middle attack. Additionally, GCM has a 64 GB file size limitation, which would require additional implementation overhead for splitting large files into 64 GB chunks.

For the control connection over TLS 1.3, I chose GCM mode to ensure the integrity of commands and metadata. GCM is a more appropriate choice here given that hte control connection is authenticated and considering that commands and metadata take up less than 1 KB.

## Two Sockets

I split communication over two sockets to get the authentication and encryption benefits of TLS without re-encrypting all of the client's data.

# Improvements

Additional improvements that could be made to the application:

1. Handle directories as well as files.
2. Implement more informative error handling. Currently, most functions catch `Exception` and provide a generic error message.
3. Use timeouts to prevent client or server from waiting indefinitely due to faulty behavior on the other side of the connection.
4. Rename files instead of overwriting them in the case of duplicate names.
5. Separate into two applications with the server in one and the client + key manager in another. I kept them together for rapid development, but they are separate pieces of software and should be decoupled.
6. Improve the command line argument infrastructure by adding a parser with error messages and a help command.
7. Send the length of the file before sending the file to avoid the overhead of opening and closing the data socket for each file.
