#!/bin/bash

# Removes files that were created during file sharer test execution
# Assumes existence of testfiles/server and testfiles/client directories

rm testfiles/server/encrypted*
rm testfiles/client/decrypted*
