package com.svds.trains.kafkaproducerharness;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;

//import uk.co.jemos.podam.api.PodamFactory;
//import uk.co.jemos.podam.api.PodamFactoryImpl;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Accepts Kafka cluster configurations, number of iterations. Sends items to
 * the kafka cluster.
 * 
 *
 */
public class ProduceTestItems {

    // public static PodamFactory podamFactory = new PodamFactoryImpl();

    public static ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws MalformedURLException,
            IOException, InterruptedException, ExecutionException {

        long start = System.currentTimeMillis();
        Properties kafkaProducerProps = new Properties();

        kafkaProducerProps.load(new java.net.URL(args[0]).openStream());

        Producer<String, String> producer = new KafkaProducer<String, String>(
                kafkaProducerProps);

        Map<MetricName, ? extends Metric> metrics = producer.metrics();

        Integer iterations = Integer.parseInt(args[2]);

        Integer i;

        long prevMillis = start;

        String topic = args[1];

        int messagesSent = 0;
        System.out.println("Running " + iterations + " iterations for topic: "
                + topic);

        for (i = 0; i < iterations; i++) {

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

        producer.flush();

        long end = System.currentTimeMillis();

        long totalMillis = end - start;

        System.out.println("Finished sending " + i + " records in: "
                + totalMillis + " millis");
    }

}
