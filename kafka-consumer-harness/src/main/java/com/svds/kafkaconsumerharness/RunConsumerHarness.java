package com.svds.kafkaconsumerharness;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunConsumerHarness {

    private static final Logger LOG = LoggerFactory.getLogger(RunConsumerHarness.class);

    public static void main(String[] args) throws MalformedURLException,
    IOException {

        LOG.info(String.format(
                " Running with props %s, topic %s,outputDir %s", args[0],
                args[1], args[2]));

        //This could be tweaked according to your needs
        final int WRITE_BATCH_SIZE = 1000;

        Properties kafkaConsumerProps = new Properties();
        kafkaConsumerProps.load(new java.net.URL(args[0]).openStream());

        String topic = args[1];

        KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(
                kafkaConsumerProps);
        consumer.subscribe(Arrays.asList(topic));
        
        //This harness only uses 1 partition, but could be more than that in the future
        TopicPartition partition0 = new TopicPartition(topic, 0);
        LOG.debug("starting with offset: " + consumer.committed(partition0));
        Path outFile = Paths.get(args[2] + "/" + topic + ".out");

        //Buffering the disk I/O
        ArrayList<String> outputBuffer = new ArrayList<>();
        try {
            while (true) {
            	//New consumer API for in Kafka 0.9.*.*
                ConsumerRecords<String, String> records = consumer.poll(1000);
                if (records.count() > 0) {
                    for (ConsumerRecord<String, String> record : records) {
                        outputBuffer.add("Partition: " + record.partition() + ",Offset: " + record.offset() + ",Key:" + record.key() + ",Value:"+ record.value());
                    }
                    if (outputBuffer.size() >= WRITE_BATCH_SIZE) {
                    	//Make sure everything is written out to the file before committing the offset
                        writeBufferToFile(outFile, outputBuffer);
                        consumer.commitSync();
                        outputBuffer.clear();
                    }
                } else {
                	//This will happen when our buffer size is below WRITE_BATCH SIZE
                    if (outputBuffer.size() > 0) {
                        LOG.debug("clearing non-zero buffer");
                        writeBufferToFile(outFile, outputBuffer);
                        outputBuffer.clear();
                    }
                }
            }
        } finally {
            if (outputBuffer.size() > 0) {
            	//Write out everything left in the buffer before quitting
                LOG.debug("found non-zero buffer at exit, writing " + outputBuffer.size() + " records to file");
                writeBufferToFile(outFile, outputBuffer);
            }
            consumer.close();
        }

    }    

    private static void writeBufferToFile(Path outFile,
            ArrayList<String> outputBuffer) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(outFile.toFile(), true));
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

        LOG.debug("Writing buffer with length: " + outputBuffer.size()
                + " to file: " + outFile.toString());

        try {

            for (String outString : outputBuffer) {
                writer.append(outString + "\n");
            }
            writer.close();
        } catch (IOException e1) {
            LOG.error(e1.getMessage());
        }

        LOG.debug("finished writing buffer");
    }

}
