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
            # Handle nested RECORD types with proper subfield inference
            if value:  # Check if the dict is not empty
                subfields = [infer_field(subkey, subvalue) for subkey, subvalue in value.items()]
                return bigquery.SchemaField(name, "RECORD", mode="NULLABLE", fields=subfields)
            else:
                # Handle empty dicts (default to a STRING field)
                return bigquery.SchemaField(name, "STRING", mode="NULLABLE")
        elif isinstance(value, list):
            # Handle arrays of basic types or RECORDs
            if value and isinstance(value[0], dict):
                # Array of RECORDs
                subfields = [infer_field(subkey, subvalue) for subkey, subvalue in value[0].items()]
                return bigquery.SchemaField(name, "RECORD", mode="REPEATED", fields=subfields)
            else:
                return bigquery.SchemaField(name, "STRING", mode="REPEATED")
        else:
            # Default to STRING for unrecognized types
            field_type = "STRING"
        return bigquery.SchemaField(name, field_type, mode="NULLABLE")

    fields = [infer_field(key, value) for key, value in message.items()]
    print(f"Inferred schema: {fields}")
    print("Schema inferred successfully.")
    return fields
===================




def ensure_bigquery_table(schema):
    """Ensure the BigQuery table exists, create it dynamically if necessary."""
    print("Ensuring BigQuery table exists...")
    client = bigquery.Client(project=PROJECT_ID)
    dataset_ref = client.dataset(DATASET_ID)

    # Check if dataset exists, create if not
    try:
        client.get_dataset(dataset_ref)
        print(f"Dataset {DATASET_ID} exists.")
    except Exception:
        print(f"Dataset {DATASET_ID} not found. Creating dataset...")
        dataset = bigquery.Dataset(dataset_ref)
        client.create_dataset(dataset)
        print(f"Dataset {DATASET_ID} created.")

    table_ref = dataset_ref.table(TABLE_ID)
    try:
        client.get_table(table_ref)
        print(f"Table {TABLE_ID} exists.")
    except Exception:
        print(f"Table {TABLE_ID} not found. Creating table...")
        try:
            print(f"Inferred schema: {schema}")
            table = bigquery.Table(table_ref, schema=schema)
            client.create_table(table)
            print(f"Table {TABLE_ID} created dynamically.")
        except Exception as e:
            print(f"Error creating table: {e}")
            raise


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
            # Handle nested RECORD types
            subfields = [infer_field(subkey, subvalue) for subkey, subvalue in value.items()]
            return bigquery.SchemaField(name, "RECORD", mode="NULLABLE", fields=subfields)
        elif isinstance(value, list):
            # Handle REPEATED fields
            if value and isinstance(value[0], dict):
                subfields = [infer_field(subkey, subvalue) for subkey, subvalue in value[0].items()]
                return bigquery.SchemaField(name, "RECORD", mode="REPEATED", fields=subfields)
            else:
                return bigquery.SchemaField(name, "STRING", mode="REPEATED")
        else:
            field_type = "STRING"  # Default to STRING for unknown types
        return bigquery.SchemaField(name, field_type, mode="NULLABLE")

    fields = [infer_field(key, value) for key, value in message.items()]
    print("Schema inferred successfully.")
    return fields


def consume_from_kafka():
    """Consume messages from Kafka."""
    print("Setting up Kafka consumer...")
    consumer_config = {
        'bootstrap.servers': KAFKA_BROKER,
        'group.id': KAFKA_GROUP_ID,
        'security.protocol': 'SASL_SSL',
        'sasl.mechanisms': 'PLAIN',
        'sasl.username': KAFKA_USERNAME,
        'sasl.password': KAFKA_PASSWORD,
        'auto.offset.reset': 'earliest',
    }

    consumer = Consumer(consumer_config)
    consumer.subscribe([KAFKA_TOPIC])
    print(f"Subscribed to topic {KAFKA_TOPIC}.")
    return consumer


def insert_into_bigquery(rows):
    """Insert rows into BigQuery."""
    print(f"Inserting {len(rows)} rows into BigQuery...")
    client = bigquery.Client(project=PROJECT_ID)
    table_ref = f"{PROJECT_ID}.{DATASET_ID}.{TABLE_ID}"

    errors = client.insert_rows_json(table_ref, rows)
    if errors:
        print(f"Error inserting rows: {errors}")
    else:
        print(f"Successfully inserted {len(rows)} rows into BigQuery.")


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


def main():
    consumer = consume_from_kafka()
    print("Starting to consume messages from Kafka...")

    schema_inferred = False
    schema = None

    try:
        while True:
            print("Polling for new messages...")
            msg = consumer.poll(1.0)  # Timeout in seconds
            if msg is None:
                print("No message received. Polling again...")
                continue
            if msg.error():
                print(f"Kafka error: {msg.error()}")
                continue

            print("Message received. Parsing...")
            data = parse_message(msg)
            if data:
                if not schema_inferred:
                    print("Inferring schema for the first time...")
                    schema = infer_schema_from_message(data)
                    ensure_bigquery_table(schema)
                    schema_inferred = True

                print("Inserting data into BigQuery...")
                insert_into_bigquery([data])  # Insert each message as a row
    except KeyboardInterrupt:
        print("Stopping consumer...")
    finally:
        consumer.close()
        print("Consumer closed.")


if __name__ == "__main__":
    main()
