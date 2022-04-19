#!/bin/bash

# Removes files that were created during file sharer test execution
# Assumes existence of workign directory

rm server/storage/*
rm client/storage/decrypted/*
rm log-server
