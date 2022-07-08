#!/bin/bash

# An integration test of app functionality with bad or atypical input
# Does not return a pass/fail result
# Test output must be manually inspected for expected output
#
# Running ./clean.sh is recommended before running this script
# It is not run from this script for the sake of reproducing identical output
#
# This test should produce output similar to expected-stress-test-output.txt
# The file paths will likely differ

# Try connecting to the server before it is running
java -jar filesharer.jar client list

# Start a server and redirect its output to a log file
java -jar filesharer.jar server &>> log-server & pid1="$!"

# Prevent client from starting before server
sleep 1

# Store a temporary key in case something changes in the next command
cp client/config/key client/config/tempkey

# Try to create key when it already exists (should fail)
java -jar filesharer.jar keygenerator

mv client/config/tempkey client/config/key

# Request files that don't exist to see how the server responds
java -jar filesharer.jar client request fakefile1 client/storage/decrypted/fakefile1 fakefile2 fakefile2

# Try to send files that don't exist to see how the client handles it
java -jar filesharer.jar client send fakefile1 fakefile2 fakefile3

java -jar filesharer.jar client list

# test some bad command line arguments
java -jar filesharer.jar
java -jar filesharer.jar client
java -jar filesharer.jar client request test.txt
java -jar filesharer.jar server send test.txt

# client commands without any files of file names
java -jar filesharer.jar client delete
java -jar filesharer.jar client send
java -jar filesharer.jar client request

# Stop the server
kill $pid1

echo "testing done"
