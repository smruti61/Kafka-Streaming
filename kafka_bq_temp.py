import argparse
import json
from confluent_kafka import Consumer
from google.cloud import bigquery

def ensure_bigquery_table(dataset_id, table_id, schema, project_id):
    """Ensure the BigQuery table exists, create it dynamically if necessary."""
    print(f"Ensuring BigQuery table {table_id} in dataset {dataset_id} exists...")
    client = bigquery.Client(project=project_id)
    dataset_ref = client.dataset(dataset_id)

    # Check if dataset exists, create if not
    try:
        client.get_dataset(dataset_ref)
        print(f"Dataset {dataset_id} exists.")
    except Exception:
        print(f"Dataset {dataset_id} not found. Creating dataset...")
        dataset = bigquery.Dataset(dataset_ref)
        client.create_dataset(dataset)
        print(f"Dataset {dataset_id} created.")

    table_ref = dataset_ref.table(table_id)
    try:
        client.get_table(table_ref)
        print(f"Table {table_id} exists.")
    except Exception:
        print(f"Table {table_id} not found. Creating table...")
        try:
            table = bigquery.Table(table_ref, schema=schema)
            client.create_table(table)
            print(f"Table {table_id} created dynamically.")
        except Exception as e:
            print(f"Error creating table: {e}")
            raise


def consume_from_kafka(topic, kafka_broker, group_id, username, password):
    """Consume messages from Kafka."""
    print(f"Setting up Kafka consumer for topic {topic}...")
    consumer_config = {
        'bootstrap.servers': kafka_broker,
        'group.id': group_id,
        'security.protocol': 'SASL_SSL',
        'sasl.mechanisms': 'PLAIN',
        'sasl.username': username,
        'sasl.password': password,
        'auto.offset.reset': 'earliest',
    }

    consumer = Consumer(consumer_config)
    consumer.subscribe([topic])
    print(f"Subscribed to topic {topic}.")
    return consumer


def parse_message(message):
    """Parse a single Kafka message into JSON format."""
    print("Parsing message...")
    try:
        data = json.loads(message.value().decode('utf-8'))
        print(f"Message parsed successfully: {data}")
        return data
    except (json.JSONDecodeError, AttributeError) as e:
        print(f"Error parsing message: {e}")
        return None


def infer_schema_from_message(message):
    """Infer BigQuery schema from a Kafka message."""
    print("Inferring schema from message...")

    def infer_field(name, value):
        if isinstance(value, int):
            field_type = "INTEGER"
        elif isinstance(value, float):
            field_type = "FLOAT"
        elif isinstance(value, str):
            field_type = "STRING"
        elif isinstance(value, dict):
            if value:
                subfields = [infer_field(subkey, subvalue) for subkey, subvalue in value.items()]
                return bigquery.SchemaField(name, "RECORD", mode="NULLABLE", fields=subfields)
            else:
                return bigquery.SchemaField(name, "STRING", mode="NULLABLE")
        elif isinstance(value, list):
            if value and isinstance(value[0], dict):
                subfields = [infer_field(subkey, subvalue) for subkey, subvalue in value[0].items()]
                return bigquery.SchemaField(name, "RECORD", mode="REPEATED", fields=subfields)
            elif value:
                field_type = infer_field(name, value[0]).field_type if value else "STRING"
                return bigquery.SchemaField(name, field_type, mode="REPEATED")
            else:
                return bigquery.SchemaField(name, "STRING", mode="REPEATED")
        else:
            return bigquery.SchemaField(name, "STRING", mode="NULLABLE")

    fields = [infer_field(key, value) for key, value in message.items()]
    print(f"Inferred schema: {fields}")
    return fields


def main():
    # Argument parsing
    parser = argparse.ArgumentParser(description="Kafka to BigQuery Ingestion Script")
    parser.add_argument("--kafka-topic", required=True, help="Kafka topic name")
    parser.add_argument("--dataset-id", required=True, help="BigQuery dataset ID")
    parser.add_argument("--table-id", required=True, help="BigQuery table ID")
    parser.add_argument("--project-id", required=True, help="GCP project ID")
    args = parser.parse_args()

    # Kafka Configuration
    kafka_broker = "kafka.sbs-bld.oncp.dev:9092"
    group_id = "test-python-group"
    username = "easy-access-user"
    password = "easy-access-secret"

    # BigQuery Configuration
    dataset_id = args.dataset_id
    table_id = args.table_id
    project_id = args.project_id

    consumer = consume_from_kafka(args.kafka_topic, kafka_broker, group_id, username, password)

    schema_inferred = False
    schema = None

    try:
        while True:
            msg = consumer.poll(1.0)
            if msg is None:
                print("No message received. Polling again...")
                continue
            if msg.error():
                print(f"Kafka error: {msg.error()}")
                continue

            data = parse_message(msg)
            if data:
                if not schema_inferred:
                    schema = infer_schema_from_message(data)
                    ensure_bigquery_table(dataset_id, table_id, schema, project_id)
                    schema_inferred = True

                # Insert into BigQuery (function can be added here)
                print(f"Data: {data}")
    except KeyboardInterrupt:
        print("Stopping consumer...")
    finally:
        consumer.close()


if __name__ == "__main__":
    main()
