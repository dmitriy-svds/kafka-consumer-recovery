#!/bin/bash

# set -e #fail on error
# set -x #debug

__dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
echo $__dir
echo $@

RUN=$1
ITERATIONS=$2
REBUILD=$3
MODE=$4

TOPIC=test_failure_$RUN
PARTITIONS=1
REPLICATION_FACTOR=2
HARNESS_VERSION=0.1


if [[ "$OSTYPE" == "linux-gnu" ]]; then
  DOCKER_IP=localhost
elif [[ "$OSTYPE" == "darwin"* ]]; then
  eval $(docker-machine env default)
  DOCKER_IP=$(docker-machine ip default)
fi

kafka_2.10-0.9.0.1/bin/kafka-topics.sh --create --zookeeper $DOCKER_IP:2181 --topic $TOPIC --partitions $PARTITIONS --replication-factor $REPLICATION_FACTOR

if [ "${MODE}" = "local" ]; then 
  echo "running in local mode"
  CONSUMER_PROPS=file://${__dir}/kafka-consumer-harness/src/main/resources/consumer_local.properties
  PRODUCER_PROPS=file://${__dir}/kafka-producer-harness/src/main/resources/producer_local.properties
else
  CONSUMER_PROPS=file://${__dir}/kafka-consumer-harness/src/main/resources/consumer.properties
  PRODUCER_PROPS=file://${__dir}/kafka-producer-harness/src/main/resources/producer.properties
fi

if [ "${REBUILD}" = "yes" ]; then
  #package jars projects 
  (cd kafka-consumer-harness; mvn package)
  (cd kafka-producer-harness; mvn package)
fi

if [ ! -d archive ]; then
  mkdir -p archive
fi

if [ -d output_$RUN ]; then
  seconds=`date +%s`
  mv output_$RUN archive/output_${RUN}_${seconds}
fi

mkdir -p output_$RUN

java -jar kafka-consumer-harness/target/kafka-consumer-harness-$HARNESS_VERSION.jar \
  $CONSUMER_PROPS $TOPIC $__dir/output_$RUN &
consumer_pid=$!

sleep 2

java -jar kafka-producer-harness/target/kafka-producer-harness-$HARNESS_VERSION.jar \
  $PRODUCER_PROPS $TOPIC $ITERATIONS &
producer_pid=$!

cleanup() {
  kill -1 $consumer_pid
  kill -1 $producer_pid
  exit 1
}

trap cleanup EXIT
trap cleanup INT

sleep 15
#kill consumer
kill -9 $consumer_pid

java -jar kafka-consumer-harness/target/kafka-consumer-harness-$HARNESS_VERSION.jar \
  $CONSUMER_PROPS $TOPIC $__dir/output_$RUN &
consumer_pid=$!

while true
do
  echo messages processed: $(wc -l output_${RUN}/${TOPIC}.out)
  sleep 10
done
