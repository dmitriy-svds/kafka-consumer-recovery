#!/bin/bash

eval $(docker-machine env default)
(cd kafka-compose; docker-compose stop)
docker-machine stop default

rm -rf kafka-compose*
rm -rf kafka_2.10-0.9.0.1

