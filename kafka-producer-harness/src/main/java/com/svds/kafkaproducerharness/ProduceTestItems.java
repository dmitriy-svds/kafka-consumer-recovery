package com.svds.kafkaproducerharness;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Accepts Kafka cluster configurations, number of iterations. Sends items to
 * the kafka cluster.
 * 
 *
 */
public class ProduceTestItems {

    private static final Logger LOG = LoggerFactory.getLogger(ProduceTestItems.class);

    public static void main(String[] args) throws MalformedURLException,
            IOException, InterruptedException, ExecutionException {

        long start = System.currentTimeMillis();
        Properties kafkaProducerProps = new Properties();

        kafkaProducerProps.load(new java.net.URL(args[0]).openStream());

        Producer<String, String> producer = new KafkaProducer<String, String>(
                kafkaProducerProps);

        Integer iterations = Integer.parseInt(args[2]);
        
        long prevMillis = start;

        String topic = args[1];

        int messagesSent = 0;
        LOG.info("Running " + iterations + " iterations for topic: "
                + topic);

        Integer i;
        for (i = 0; i < iterations; i++) {

        	//Messages will be in format: "Message_[no of message]"
            producer.send(new ProducerRecord<String, String>(topic, i
                    .toString(), "Message_" + i));
            messagesSent++;

            // Needed because otherwise we overwhelm the producer
            Thread.sleep(1);
            if (i % 10000 == 0) {
                long currMillis = System.currentTimeMillis();
                long setTime = currMillis - prevMillis;
                System.out.println("sent " + messagesSent
                        + " records so far in " + setTime);
                prevMillis = currMillis;
            }
        }

        //Let's actually send the messages
        producer.flush();

        long end = System.currentTimeMillis();

        long totalMillis = end - start;

        LOG.info("Finished sending " + i + " records in: "
                + totalMillis + " millis");
    }

}
