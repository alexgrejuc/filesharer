#!/bin/bash

# An integration test of app functionality
# Does not return a pass/fail result
# Test output must be manually inspected for expected output
#
# If it is called with argument all, then it includes a large mp4 file
# e.g. ./test.sh all
# Note that this file is not committed to the repo and a substitute must be used if cloning
#
# Running ./clean.sh is recommended before running this script
# It is not run from this script for the sake of reproducing identical output
#
# This test should produce output similar to expected-test-output.txt
# The file paths will likely differ

# Start a server and redirect its output to a log file
java -jar filesharer.jar server &>> log-server & pid1="$!"

# Prevent client from starting before server
sleep 1

# Move the key so a new one can be created
mv client/config/key client/config/oldkey

java -jar filesharer.jar keygenerator

# Restore the old key
mv client/config/oldkey client/config/key

# List files before client does anything
java -jar filesharer.jar client list

# Send files to server
java -jar filesharer.jar client send client/storage/original/test.txt client/storage/original/test.png

if [ "$1" = all ]; then
    java -jar filesharer.jar client send client/storage/original/big.mp4
fi

# List to see the server storage containing those files
java -jar filesharer.jar client list

# Request the files back
java -jar filesharer.jar client request test.txt client/storage/decrypted/test.txt test.png client/storage/decrypted/test.png

if [ "$1" = all ]; then
    java -jar filesharer.jar client request big.mp4 client/storage/decrypted/big.mp4
fi

# Delete test.png, keep test.txt to verify files were actually encrypted
java -jar filesharer.jar client delete test.png

if [ "$1" = all ]; then
    java -jar filesharer.jar client delete big.mp4
fi

# List to see the updated server storage without those files
java -jar filesharer.jar client list

# Checks that the decrypted files match the original ones
cmp client/storage/decrypted/test.txt client/storage/original/test.txt
cmp client/storage/decrypted/test.png client/storage/original/test.png


if [ "$1" = all ]; then
    cmp client/storage/decrypted/big.mp4 client/storage/original/big.mp4
fi

echo "checked that files match"
echo

# Stop the server
kill $pid1

echo "testing done"
