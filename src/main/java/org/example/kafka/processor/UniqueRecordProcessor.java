package org.example.kafka.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UniqueRecordProcessor implements Transformer<Void, String, KeyValue<Void, String>> {
    private static final Logger logger = LoggerFactory.getLogger(UniqueRecordProcessor.class);
    private KeyValueStore<String, String> stateStore;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String stateStoreName;

    // Constructor to initialize stateStoreName
    public UniqueRecordProcessor(String stateStoreName) {
        this.stateStoreName = stateStoreName;
    }

    @Override
    public void init(ProcessorContext context) {
        stateStore = (KeyValueStore<String, String>) context.getStateStore(stateStoreName);
        logger.info("State store {} initialized.", stateStoreName);
    }

    @Override
    public KeyValue<Void, String> transform(Void key, String value) {
        try {
            JsonNode jsonNode = objectMapper.readTree(value);
            String eventId = jsonNode.get("event_id").asText();

            String storedValue = stateStore.get(eventId);

            if (storedValue != null) {
                logger.info("Event ID {} already exists in the state store. Value: {}", eventId, storedValue);
                return null;
            } else {
                stateStore.put(eventId, value);
                logger.info("Event ID {} added to the state store. Value: {}", eventId, value);
                return new KeyValue<>(null, value);
            }
        } catch (Exception e) {
            logger.error("Error processing message with value: {}", value, e);
            return null;
        }
    }

    @Override
    public void close() {
        logger.info("Closing the transformer.");
    }
}
