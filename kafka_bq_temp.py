from confluent_kafka import Producer
from google.protobuf.message import Message
from google.protobuf.descriptor import Descriptor, FieldDescriptor
from google.protobuf.message_factory import GetPrototype

# Kafka Configuration
KAFKA_BROKER = 'kafka.sbs-bld.oncp.dev:9092'
KAFKA_TOPIC = 'vault.core_api.v1.accounts.account_staging.events'
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

# Define Protobuf message dynamically
account_event_descriptor = Descriptor(
    name='AccountEvent',
    full_name='AccountEvent',
    fields=[
        FieldDescriptor(
            name='name',
            full_name='AccountEvent.name',
            index=0,
            number=1,
            type=FieldDescriptor.TYPE_STRING,
            cpp_type=FieldDescriptor.CPPTYPE_STRING,
            label=FieldDescriptor.LABEL_OPTIONAL,
            has_default_value=False,
            default_value=None,
            message_type=None,
            enum_type=None,
            containing_type=None,
            is_extension=False,
            extension_scope=None,
            options=None
        )
    ],
    nested_types=[],
    enum_types=[],
    options=None,
    is_extendable=False,
    extensions=[]
)

# Create a Protobuf message class dynamically
AccountEvent = GetPrototype(account_event_descriptor)

# Kafka Producer
producer = Producer(kafka_config)

def produce_protobuf_message():
    try:
        # Create a new message instance and set the 'name' field
        account_event = AccountEvent()
        account_event.name = "John Doe"  # Setting the name value

        # Serialize Protobuf message to binary format
        protobuf_message = account_event.SerializeToString()

        # Produce the serialized message to Kafka topic
        producer.produce(KAFKA_TOPIC, value=protobuf_message)
        producer.flush()

        print(f"Protobuf message with name='{account_event.name}' published successfully to topic: {KAFKA_TOPIC}")

    except Exception as e:
        print(f"Failed to publish message: {e}")

# Run the producer
if __name__ == "__main__":
    produce_protobuf_message()
