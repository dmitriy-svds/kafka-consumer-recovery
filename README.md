# kafka-consumer-recovery
A project to test at-least-once recovery of a Kafka consumer.

## Requirements:
OSX: [Docker Machine](https://docs.docker.com/machine/install-machine/) - with a 'default' machine already created
Linux: Docker

## Set-up the dev environment
`./dev_setup.sh`

## Running the harness:
`./test_consumer_fail.sh [run name] [#of iterations] [rebuild(yes/no)]`
*During every run, the 'run name' should be changed to avoid collision with previously-created topics

Watching the output:
`tail -f consumer.out producer.out`

## Tear-down dev environment
`./dev_teardown.sh`
