syntax = "proto3";

message AccountEvent {
    string name = 1;
}
======================================
protoc --python_out=. account_event.proto
======================================
from confluent_kafka import Producer
import account_event_pb2  # Import the generated protobuf class

# Kafka Configuration
KAFKA_BROKER = 'kafka.sbs-bld.oncp.dev:9092'
KAFKA_TOPIC = 'vault.core_api.v1.accounts.account_staging.events'
KAFKA_GROUP_ID = 'plain-python-group'
KAFKA_USERNAME = 'easy-access-user'
KAFKA_PASSWORD = 'easy-access-secret'

# Kafka producer configuration
kafka_config = {
    'bootstrap.servers': KAFKA_BROKER,
    'security.protocol': 'SASL_SSL',
    'sasl.mechanism': 'PLAIN',
    'sasl.username': KAFKA_USERNAME,
    'sasl.password': KAFKA_PASSWORD,
    'acks': 'all'
}

# Create a Kafka producer
producer = Producer(kafka_config)

# Create and serialize Protobuf message
def produce_protobuf_message():
    try:
        account_event = account_event_pb2.AccountEvent()
        account_event.name = "John Doe"  # Example name
        
        # Serialize the Protobuf message
        protobuf_message = account_event.SerializeToString()

        # Produce the message to Kafka topic
        producer.produce(KAFKA_TOPIC, value=protobuf_message)
        producer.flush()

        print(f"Protobuf message published successfully to topic: {KAFKA_TOPIC}")

    except Exception as e:
        print(f"Failed to publish message: {e}")

# Run the producer
if __name__ == "__main__":
    produce_protobuf_message()
