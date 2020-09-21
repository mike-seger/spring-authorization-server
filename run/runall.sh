#!/bin/bash

set -e

pid=$$
# shellcheck disable=SC2064
trap "kill -- -$pid" EXIT

# shellcheck disable=SC2046
cd $(dirname "$0")/..

rm -fR build/*
javac run/Wait4Net.java -d build

port=39001

function logTcp() {
  local host=$1
  local port=$2
  local intPort=$3
  local label=$4
  (
    cd run
    java -jar tcptunnel-1.2.0.jar "$port" "$host" "$intPort" \
      --logger console-string >"../build/$port-$label-tcp.log" &
  )
}

function runApp() {
  local type=$1
  local host=$2
  local port=$3
  intPort=$((port + 100))
  SERVER_PORT=$intPort ./gradlew -b \
    "samples/boot/oauth2-integration/$type/spring-security-samples-boot-oauth2-integrated-$type.gradle" \
    bootRun >"build/$type.log" &
  java -cp build Wait4Net "$host" $intPort 50000
  logTcp "$host" "$port" "$intPort" "$type"
}

runApp client localhost 39001
runApp resourceserver localhost 39002
runApp authorizationserver localhost 39003

echo "Application is ready."
echo ""
echo -n "press enter to terminate..."
read -r
