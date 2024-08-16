package org.example.kafka.config;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

public class KafkaConfig {
    private static final Logger logger = LoggerFactory.getLogger(KafkaConfig.class);
    private static Properties properties = new Properties();

    static {
        try (InputStream input = KafkaConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new RuntimeException("Unable to find application.properties");
            }
            properties.load(input);
        } catch (Exception ex) {
            logger.error("Error loading properties file", ex);
            throw new RuntimeException("Error loading properties file", ex);
        }
    }

    public static Properties getStreamsConfig() {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, getRequiredProperty("stream.application-id"));
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, getRequiredProperty("kafka.bootstrap-servers"));
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, getRequiredProperty("stream.default.key.serde.class"));
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, getRequiredProperty("stream.default.value.serde.class"));
        props.put(StreamsConfig.APPLICATION_SERVER_CONFIG, getRequiredProperty("stream.application.server"));
        props.put(StreamsConfig.STATE_DIR_CONFIG, getRequiredProperty("stream.state.dir"));

        return props;
    }

    private static String getRequiredProperty(String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new RuntimeException("Property " + key + " is not defined in application.properties");
        }
        return value;
    }

    public static String getAccountCreateInputTopic() {
        return getRequiredProperty("kafka.streams.account_create.input");
    }

    public static String getAccountCreateDeadLetterTopic() {
        return getRequiredProperty("kafka.streams.account_create.dead-letter-topic");
    }

    public static String getAccountCreateOutputTopic() {
        return getRequiredProperty("kafka.streams.account_create.output");
    }

    public static String getAccountCreateRawOutputTopic() {
        return getRequiredProperty("kafka.streams.account_create.raw-output");
    }

    public static String getAccountCreateGroupId() {
        return getRequiredProperty("kafka.streams.account_create.group-id");
    }

    public static String getAccountCreateSchemaPath() {
        return getRequiredProperty("kafka.streams.account_create.schema-path");
    }

    public static String getAccountCreateValidationsPath() {
        return getRequiredProperty("kafka.streams.account_create.validations-path");
    }

    public static String getAccountCreateStateStoreName() {
        return getRequiredProperty("kafka.streams.account_create.state-store");
    }

    public static String getAccountUpdateInputTopic() {
        return getRequiredProperty("kafka.streams.account_update.input");
    }

    public static String getAccountUpdateDeadLetterTopic() {
        return getRequiredProperty("kafka.streams.account_update.dead-letter-topic");
    }

    public static String getAccountUpdateOutputTopic() {
        return getRequiredProperty("kafka.streams.account_update.output");
    }

    public static String getAccountUpdateRawOutputTopic() {
        return getRequiredProperty("kafka.streams.account_update.raw-output");
    }

    public static String getAccountUpdateGroupId() {
        return getRequiredProperty("kafka.streams.account_update.group-id");
    }

    public static String getAccountUpdateSchemaPath() {
        return getRequiredProperty("kafka.streams.account_update.schema-path");
    }

    public static String getAccountUpdateValidationsPath() {
        return getRequiredProperty("kafka.streams.account_update.validations-path");
    }

    public static String getAccountUpdateStateStoreName() {
        return getRequiredProperty("kafka.streams.account_update.state-store");
    }

    public static StoreBuilder<KeyValueStore<String, String>> getStateStoreBuilder(String storeName) {
        return Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(storeName),
                Serdes.String(),
                Serdes.String()
        );
    }
}
