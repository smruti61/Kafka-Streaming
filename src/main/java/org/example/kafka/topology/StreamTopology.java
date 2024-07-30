package org.example.kafka.topology;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.example.kafka.config.KafkaConfig;
import org.example.kafka.processor.StreamProcessor;
import org.example.kafka.processor.UniqueRecordProcessor;
import org.example.kafka.validator.KafkaMessageValidator;

public class StreamTopology {
    private static final Logger logger = LoggerFactory.getLogger(StreamTopology.class);

    public static void buildTopology(StreamsBuilder builder) {
        String stateStoreName = KafkaConfig.getAccountCreateStateStoreName();
        StoreBuilder<KeyValueStore<String, String>> storeBuilder = KafkaConfig.getStateStoreBuilder(stateStoreName);
        builder.addStateStore(storeBuilder);

        configureAccountCreateTopic(builder, stateStoreName);
    }

    private static void configureAccountCreateTopic(StreamsBuilder builder, String stateStoreName) {
        String inputTopic = KafkaConfig.getAccountCreateInputTopic();
        String deadLetterTopic = KafkaConfig.getAccountCreateDeadLetterTopic();
        String outputTopic = KafkaConfig.getAccountCreateOutputTopic();
        String rawOutputTopic = KafkaConfig.getAccountCreateRawOutputTopic();
        String groupId = KafkaConfig.getAccountCreateGroupId();
        String schemaPath = KafkaConfig.getAccountCreateSchemaPath();
        String validationsPath = KafkaConfig.getAccountCreateValidationsPath();

        KStream<byte[], byte[]> rawStream = builder.stream(inputTopic, Consumed.with(Serdes.ByteArray(), Serdes.ByteArray()));
        KStream<Void, String> deserializedStream = StreamProcessor.deserialize(rawStream);

        KafkaMessageValidator validator;
        try {
            validator = new KafkaMessageValidator("localhost:9092", groupId, schemaPath, validationsPath);
        } catch (Exception e) {
            logger.error("Failed to initialize KafkaMessageValidator", e);
            throw new RuntimeException(e);
        }

        KStream<Void, String>[] branches = deserializedStream.branch(
                (key, value) -> {
                    try {
                        logger.info("*******************************************************************************");
                        return validator.validateMessage(value, inputTopic);
                    } catch (Exception e) {
                        logger.error("Validation error for key: {}, value: {}", key, value, e);
                        return false;
                    }
                },
                (key, value) -> true
        );

        KStream<Void, String> validStream = branches[0];
        KStream<byte[], byte[]> invalidStream = StreamProcessor.processInvalidStream(branches[1]);
        invalidStream.to(deadLetterTopic, Produced.with(Serdes.ByteArray(), Serdes.ByteArray()));

        KStream<Void, String> uniqueStream = validStream.transform(() -> new UniqueRecordProcessor(stateStoreName), stateStoreName);

        KStream<byte[], byte[]> outputStream = StreamProcessor.processUniqueStream(uniqueStream);
        outputStream.to(outputTopic, Produced.with(Serdes.ByteArray(), Serdes.ByteArray()));
    }
}
