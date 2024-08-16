package org.example.kafka.streaming;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.JoinWindows;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.StreamJoined;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.time.Duration;
import java.util.Map;
import java.util.Properties;

public class KafkaJoinStreamingProcessor {
    private static final Logger log = LoggerFactory.getLogger(KafkaJoinStreamingProcessor.class);

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "join-example");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        StreamsBuilder builder = new StreamsBuilder();

        KStream<String, String> accountCreatedStream = builder.stream("output-topic-account-create");
        KStream<String, String> accountUpdatedStream = builder.stream("output-topic-account-update");

        ObjectMapper mapper = new ObjectMapper();

        KStream<String, JsonNode> accountCreatedParsedStream = accountCreatedStream
                .map((key, value) -> {
                    try {
                        log.info("Received account created message: key={}, value={}", key, value);
                        JsonNode jsonNode = mapper.readTree(value);
                        String accountId = jsonNode.path("account_created").path("account").path("id").asText();
                        log.info("Parsed account ID from created message: {}", accountId);
                        return KeyValue.pair(accountId, jsonNode);
                    } catch (Exception e) {
                        log.error("Error parsing account created JSON: {}", value, e);
                        return KeyValue.pair(null, null);
                    }
                })
                .filter((key, value) -> key != null && value != null);

        KStream<String, JsonNode> accountUpdatedParsedStream = accountUpdatedStream
                .map((key, value) -> {
                    try {
                        log.info("Received account updated message: key={}, value={}", key, value);
                        JsonNode jsonNode = mapper.readTree(value);
                        String accountId = jsonNode.path("account_update_created").path("account_update").path("account_id").asText();
                        log.info("Parsed account ID from updated message: {}", accountId);
                        return KeyValue.pair(accountId, jsonNode);
                    } catch (Exception e) {
                        log.error("Error parsing account updated JSON: {}", value, e);
                        return KeyValue.pair(null, null);
                    }
                })
                .filter((key, value) -> key != null && value != null);

        KStream<String, String> joinedStream = accountCreatedParsedStream.join(
                accountUpdatedParsedStream,
                (created, updated) -> {
                    log.info("Joining created: {} with updated: {}", created, updated);
                    try {
                        JsonNode accountCreated = created.path("account_created").path("account");
                        JsonNode accountUpdated = updated.path("account_update_updated").path("account_update");

//     Placeholder
                        Map<String, Object> resultMap = Map.of(
                                "account_id", accountCreated.path("id").asText(),
                                "created_name", accountCreated.path("name").asText(),
                                "created_status", accountCreated.path("status").asText(),
                                "job_id", accountUpdated.path("job_id").asText(),
                                "updated_status", accountUpdated.path("status").asText()
                        );

                        return mapper.writeValueAsString(resultMap);
                    } catch (Exception e) {
                        log.error("Error joining JSONs", e);
                        return null;
                    }
                },
                JoinWindows.of(Duration.ofSeconds(100)).grace(Duration.ZERO),
                StreamJoined.with(Serdes.String(), new JsonSerde<>(JsonNode.class), new JsonSerde<>(JsonNode.class))
        ).filter((key, value) -> value != null);

        joinedStream.foreach((key, value) -> log.info("Joined message: {}", value));

        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        streams.start();

        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));

//
    }
}