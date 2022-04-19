#!/bin/bash

# Creates the following directory structure:
#
# working-directory
#   client
#       config
#       storage
#           original
#           decrypted
#   server
#       config
#       storage

mkdir -p working-directory/{client/{config,storage/{original,decrypted}},server/{config,storage}}
