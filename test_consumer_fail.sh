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
  command -v docker >/dev/null 2>&1 || { echo >&2 "Script requires docker but it's not installed.  Aborting."; exit 1; }
elif [[ "$OSTYPE" == "darwin"* ]]; then
  command -v docker-machine >/dev/null 2>&1 || { echo >&2 "Script requires docker-machine but it's not installed.  Aborting."; exit 1; }
  command -v docker >/dev/null 2>&1 || { echo >&2 "Script requires docker but it's not installed.  Aborting."; exit 1; }
  docker-machine start default
  eval $(docker-machine env default)

  echo "docker machine running on IP:"
  echo `docker-machine ip default`
  eval $(docker-machine env default)
fi

cd $__dir
[ -d "kafka-compose" ] || git clone git@github.com:dmitriy-svds/kafka-compose.git
[ -d "kafka_2.10-0.9.0.1" ] || curl -O http://apache.spinellicreations.com/kafka/0.9.0.1/kafka_2.10-0.9.0.1.tgz && tar -xf kafka_2.10-0.9.0.1.tgz && rm kafka_2.10-0.9.0.1.tgz

(cd kafka-compose; ./start_environment.sh)

DOCKER_IP=$(docker-machine ip default)

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
  mkdir archive
fi

if [ -d output_$RUN ]; then
  seconds=`date +%s`
  mv output_$RUN archive/output_${RUN}_${seconds}
fi

mkdir output_$RUN

java -jar kafka-consumer-harness/target/kafka-consumer-harness-$HARNESS_VERSION.jar \
  $CONSUMER_PROPS $TOPIC $__dir/output_$RUN >> consumer.out 2>&1 &
consumer_pid=$!

sleep 2

java -jar kafka-producer-harness/target/kafka-producer-harness-$HARNESS_VERSION.jar \
  $PRODUCER_PROPS $TOPIC $ITERATIONS >> producer.out 2>&1 &
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
  $CONSUMER_PROPS $TOPIC $__dir/output_$RUN >> consumer.out 2>&1 &
consumer_pid=$!

while true
do
  echo messages processed: $(wc -l output_${RUN}/${TOPIC}.out)
  sleep 10
done
