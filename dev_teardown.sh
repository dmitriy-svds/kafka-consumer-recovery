#!/bin/bash

[[ "$OSTYPE" == "darwin"* ]] && eval $(docker-machine env default)

(cd kafka-compose; docker-compose stop)

[[ "$OSTYPE" == "darwin"* ]] && docker-machine stop default

rm -rf kafka-compose*
rm -rf kafka_2.10-0.9.0.1
rm -rf output*
rm -rf archive
rm -rf kafka-consumer-harness/target
rm -rf kafka-producer-harness/target
rm -rf log/*
