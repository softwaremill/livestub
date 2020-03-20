#!/bin/bash
echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USER" --password-stdin
sbt docker:publish
