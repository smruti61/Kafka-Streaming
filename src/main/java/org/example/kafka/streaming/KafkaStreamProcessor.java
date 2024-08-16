package org.example.kafka.streaming;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.KafkaStreams.State;
import org.example.kafka.config.KafkaConfig;
import org.example.kafka.topology.StreamTopology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class KafkaStreamProcessor {
    private static final Logger logger = LoggerFactory.getLogger(KafkaStreamProcessor.class);

    public static void main(String[] args) {

        // Load Kafka Streams configuration from KafkaConfig
        Properties props = KafkaConfig.getStreamsConfig();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "my-join-stream-application"); // Ensure a unique application ID
        props.put(StreamsConfig.CLIENT_ID_CONFIG, "my-stream-client"); // Optional: Set client ID for monitoring

        // Create StreamsBuilder and build the topology
        StreamsBuilder builder = new StreamsBuilder();
        StreamTopology.buildTopology(builder);

        // Create and start Kafka Streams application
        KafkaStreams streams = new KafkaStreams(builder.build(), props);

        // Add a state change listener for better monitoring
        streams.setStateListener((newState, oldState) -> {
            logger.info("Kafka Streams state changed from {} to {}", oldState, newState);
            if (newState == State.ERROR) {
                logger.error("Kafka Streams encountered an error state.");
            }
        });

        // Start the application
        try {
            streams.start();
            logger.info("Kafka Streams application started successfully.");
        } catch (Exception e) {
            logger.error("Error starting Kafka Streams application", e);
            System.exit(1); // Ensure the application exits with a non-zero status on failure
        }

        // Add shutdown hook to close Kafka Streams on application exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down Kafka Streams application.");
            try {
                streams.close();
                logger.info("Kafka Streams application shut down successfully.");
            } catch (Exception e) {
                logger.error("Error during Kafka Streams application shutdown", e);
            }
        }));
    }
}