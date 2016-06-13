package com.svds.trains.kafkaconsumerharness;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import kafka.serializer.Decoder;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

public class RunConsumerHarness {

    // private static final int WRITE_BATCH_SIZE = 1;

    public static void main(String[] args) throws MalformedURLException,
    IOException {

        System.out.println(String.format(
                " Running with props %s, topic %s,outputDir %s", args[0],
                args[1], args[2]));

        final int WRITE_BATCH_SIZE = 1000;

        Properties kafkaConsumerProps = new Properties();
        kafkaConsumerProps.load(new java.net.URL(args[0]).openStream());

        String topic = args[1];

        KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(
                kafkaConsumerProps);

        consumer.subscribe(Arrays.asList(topic));
        TopicPartition partition0 = new TopicPartition(topic, 0);

        System.out.println("starting with offset: " + consumer.committed(partition0));
        Path outFile = Paths.get(args[2] + "/" + topic + ".out");

        ArrayList<String> outputBuffer = new ArrayList<>();

        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(1000);
                if (records.count() > 0) {
                    for (ConsumerRecord<String, String> record : records) {
                        outputBuffer.add("Partition: " + record.partition() + ",Offset: " + record.offset() + ",Key:" + record.key() + ",Value:"+ record.value());
                    }
                    if (outputBuffer.size() >= WRITE_BATCH_SIZE) {
                        writeBufferToFile(outFile, outputBuffer);
                        consumer.commitSync();
                        outputBuffer.clear();
                    }
                } else {
                    if (outputBuffer.size() > 0) {
                        System.out.println("clearing non-zero buffer");
                        writeBufferToFile(outFile, outputBuffer);
                        outputBuffer.clear();
                    }
                }
            }
        } finally {
            if (outputBuffer.size() > 0) {
                System.out.println("found non-zero buffer at exit, writing " + outputBuffer.size() + " records to file");
                writeBufferToFile(outFile, outputBuffer);
            }
            consumer.close();
        }

    }

    public static Decoder<String> getStringDecoder() {
        Decoder<String> decoder = new Decoder<String>() {
            public String fromBytes(byte[] bytes) {
                return bytes.toString();
            }
        };
        return decoder;
    }

    private static void writeBufferToFile(Path outFile,
            ArrayList<String> outputBuffer) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(outFile.toFile(), true));
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

        System.out.println("Writing buffer with length: " + outputBuffer.size()
                + " to file: " + outFile.toString());

        try {

            for (String outString : outputBuffer) {
                writer.append(outString + "\n");
            }
            writer.close();
        } catch (IOException e1) {
            System.err.println(e1.getMessage());
        }

        System.out.println("finished writing buffer");
    }

}
