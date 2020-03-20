#!/usr/bin/env bash

if [[ "$TRAVIS_PULL_REQUEST" == "false" ]]; then
    openssl aes-256-cbc -K $encrypted_a0289d054342_key -iv $encrypted_a0289d054342_iv -in secrets.tar.enc -out secrets.tar -d
    tar xvf secrets.tar
fi
