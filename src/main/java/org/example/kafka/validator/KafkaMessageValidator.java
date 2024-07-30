package org.example.kafka.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.ValidationMessage;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class KafkaMessageValidator {
    private static final Logger logger = LoggerFactory.getLogger(KafkaMessageValidator.class);
    private final KafkaConsumer<String, String> consumer;
    private final ObjectMapper mapper;
    private final JsonSchema schema;
    private final Map<String, Map<String, Object>> columnValidations;

    public KafkaMessageValidator(String bootstrapServers, String groupId, String schemaPath, String validationsPath) throws Exception {
        logger.info("Initializing KafkaMessageValidator...");
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        consumer = new KafkaConsumer<>(properties);
        mapper = new ObjectMapper(new YAMLFactory());
        schema = loadJsonSchema(schemaPath);
        columnValidations = loadYaml(validationsPath);
        logger.info("KafkaMessageValidator initialized successfully.");
    }

    private JsonSchema loadJsonSchema(String schemaPath) throws Exception {
        logger.info("Loading JSON Schema...");
        JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance();
        try (InputStream schemaStream = getClass().getClassLoader().getResourceAsStream(schemaPath)) {
            if (schemaStream == null) {
                throw new IllegalArgumentException("Schema file not found: " + schemaPath);
            }
            Yaml yaml = new Yaml();
            Map<String, Object> yamlSchema = yaml.load(schemaStream);
            String jsonSchema = new ObjectMapper().writeValueAsString(yamlSchema);
            logger.info("JSON Schema loaded successfully.");
            return schemaFactory.getSchema(jsonSchema);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T loadYaml(String path) throws Exception {
        logger.info("Loading YAML File...");
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(path)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("File not found: " + path);
            }
            logger.info("YAML File loaded successfully.");
            return (T) new Yaml().load(inputStream);
        }
    }

    public boolean validateMessage(String jsonData, String topic) throws Exception {
        logger.info("Started Validating message: "+jsonData);
        JsonNode jsonNode = mapper.readTree(jsonData);

        Set<ValidationMessage> errors = schema.validate(jsonNode);
        if (errors.isEmpty()) {
            if (columnValidations.containsKey(topic)) {
                boolean result = applyColumnValidations(jsonNode, columnValidations.get(topic));
                logger.info("Message validation result: {}", result);
                return result;
            }
            logger.info("Message validation passed.");
            return true;
        } else {
            logValidationErrors(errors);
            logger.info("Message validation failed.");
            return false;
        }
    }

    private void logValidationErrors(Set<ValidationMessage> errors) {
        for (ValidationMessage error : errors) {
            logger.error("Validation error: {}", error.getMessage());
        }
    }

    private boolean applyColumnValidations(JsonNode jsonNode, Map<String, Object> validations) {
        logger.info("Applying column validations...");
        boolean isValid = true;
        for (Map.Entry<String, Object> entry : validations.entrySet()) {
            String columnName = entry.getKey();
            Object validationRules = entry.getValue();
            JsonNode columnNode = jsonNode.path(columnName);

            if (validationRules instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> rules = (Map<String, Object>) validationRules;

                if (rules.containsKey("required") && columnNode.isMissingNode()) {
                    logger.error("Validation failed: Field {} is required but missing.", columnName);
                    isValid = false;
                }

                if (rules.containsKey("nullable") && columnNode.isNull()) {
                    continue;
                }
            }
        }
        logger.info("Column validations applied.");
        return isValid;
    }
}
