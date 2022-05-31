#!/bin/bash

# An integration test of app functionality with bad or atypical input
# Does not return a pass/fail result
# Test output must be manually inspected for expected output

# Try connecting to the server before it is running
scala filesharer.jar client list

# Start a server and redirect its output to a log file
scala filesharer.jar server &>> log-server & pid1="$!"

# Prevent client from starting before server
sleep 1

# Store a temporary key in case something changes in the next command
cp client/config/key client/config/tempkey

# Try to create key when it already exists (should fail)
scala filesharer.jar keygenerator

mv client/config/tempkey client/config/key

# Request files that don't exist to see how the server responds
scala filesharer.jar client request fakefile1 client/storage/decrypted/fakefile1 fakefile2 fakefile2

# Try to send files that don't exist to see how the client handles it
scala filesharer.jar client send fakefile1 fakefile2 fakefile3

scala filesharer.jar client list

# test some bad command line arguments
scala filesharer.jar
scala filesharer.jar client
scala filesharer.jar client request test.txt
scala filesharer.jar server send test.txt

# client commands without any files of file names
scala filesharer.jar client delete
scala filesharer.jar client send
scala filesharer.jar client request

# Stop the server
kill $pid1

echo "testing done"
