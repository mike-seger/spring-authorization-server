#!/bin/bash

set -e

pid=$$
trap "kill -- -$pid" EXIT

cd $(dirname "$0")/..

rm -fR build
javac run/Wait4Net.java -d build

port=39001

function logtcp() {
	local host=$1
	local port=$2
	local intport=$3
	local label=$4
	(cd run ; java -jar tcptunnel-1.2.0.jar $port $host $intport \
		--logger console-string >../build/$port-$label-tcp.log &)
}

function runapp() {
	local type=$1
	local host=$2
	local port=$3
	intport=$((port+100))
	SERVER_PORT=$intport ./gradlew -b samples/boot/oauth2-integration/$type/spring-security-samples-boot-oauth2-integrated-$type.gradle bootRun >build/$type.log &
	java -cp build Wait4Net $host $intport 50000
	logtcp $host $port $intport "$type"
}

runapp client localhost 39001
runapp resourceserver localhost 39002
runapp authorizationserver localhost 39003

echo "Application is ready."
echo ""
echo -n "press enter to terminate..."
read
