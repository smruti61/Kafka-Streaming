package org.example.kafka.processor;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.KStream;
import org.example.kafka.config.KafkaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
public class StreamProcessor {

    private static final Logger logger = LoggerFactory.getLogger(StreamProcessor.class);
    private static final Serdes.StringSerde valueSerde = new Serdes.StringSerde();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Method to Deserialize the Raw Stream
    public static KStream<Void, String> deserialize(KStream<byte[], byte[]> rawStream) {
        return rawStream.map(
                (key, value) -> {
                    try {
                        String deserializedValue = valueSerde.deserializer().deserialize(null, value);
                        return new KeyValue<>((Void) null, deserializedValue);  // Use null for the key
                    } catch (Exception e) {
                        logger.error("Deserialization error for value: {}", new String(value), e);
                        return null;
                    }
                }
        ).filter((key, value) -> value != null);
    }

    // Processing method for invalid streams, sending to dead-letter topic
    public static KStream<byte[], byte[]> processInvalidStream(KStream<Void, String> invalidStream) {
        return invalidStream.map(
                (key, value) -> {
                    try {
                        ObjectNode valueJson = objectMapper.readValue(value, ObjectNode.class);
                        String formattedJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(valueJson);
                        logger.warn("Sending invalid message to dead-letter topic: Value: {}", formattedJson);
                        return new KeyValue<>(new byte[0], formattedJson.getBytes());
                    } catch (Exception e) {
                        logger.error("Error converting to JSON for dead-letter topic. Value: {}", value, e);
                        return null;
                    }
                }
        ).filter((key, value) -> value != null);
    }

    // Processing method for valid streams, sending to output topic
    public static KStream<byte[], byte[]> processUniqueStream(KStream<Void, String> uniqueStream) {
        return uniqueStream.map(
                (key, value) -> {
                    try {
                        ObjectNode valueJson = objectMapper.readValue(value, ObjectNode.class);
                        String formattedJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(valueJson);
                        logger.info("Sending unique record to output topic: Value: {}", formattedJson);
                        return new KeyValue<>(new byte[0], formattedJson.getBytes());
                    } catch (Exception e) {
                        logger.error("Error converting to JSON for output topic. Value: {}", value, e);
                        return null;
                    }
                }
        ).filter((key, value) -> value != null);
    }
}