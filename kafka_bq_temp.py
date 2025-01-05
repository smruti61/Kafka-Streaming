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
