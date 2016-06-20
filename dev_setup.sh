#!/bin/bash

if [[ "$OSTYPE" == "linux-gnu" ]]; then
  command -v docker >/dev/null 2>&1 || { echo >&2 "Script requires docker but it's not installed.  Aborting."; exit 1; }
  DOCKER_IP="localhost"
elif [[ "$OSTYPE" == "darwin"* ]]; then
  command -v docker-machine >/dev/null 2>&1 || { echo >&2 "Script requires docker-machine but it's not installed.  Aborting."; exit 1; }
  command -v docker >/dev/null 2>&1 || { echo >&2 "Script requires docker but it's not installed.  Aborting."; exit 1; }
  docker-machine start default
  eval $(docker-machine env default)

  echo "docker machine running on IP:"
  echo `docker-machine ip default`
  eval $(docker-machine env default)
  DOCKER_IP=$(docker-machine ip default)
fi

[ -d "kafka-compose" ] || git clone git@github.com:dmitriy-svds/kafka-compose.git
[ -d "kafka_2.10-0.9.0.1" ] || curl -O http://apache.spinellicreations.com/kafka/0.9.0.1/kafka_2.10-0.9.0.1.tgz && tar -xzf kafka_2.10-0.9.0.1.tgz && rm kafka_2.10-0.9.0.1.tgz

(cd kafka-compose; ./start_environment.sh)

