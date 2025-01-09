import json
from typing import Any, Dict, List

def bq_to_schema_registry(bq_schema: List[Dict[str, Any]]) -> Dict:
    """
    Convert BigQuery table schema to Schema Registry compatible JSON schema.
    """
    def map_bq_type(bq_type: str) -> str:
        """Map BigQuery types to JSON schema types."""
        mapping = {
            "STRING": "string",
            "INTEGER": "integer",
            "FLOAT": "number",
            "BOOLEAN": "boolean",
            "RECORD": "object",
            "TIMESTAMP": "string",
            "DATE": "string",
            "TIME": "string",
            "DATETIME": "string"
        }
        return mapping.get(bq_type, "string")

    def process_field(field: Dict[str, Any]) -> Dict:
        """Process a single field in the BigQuery schema."""
        field_type = map_bq_type(field["type"])
        schema_field = {
            "type": field_type,
            "description": field.get("description", f"The {field_type} type is used for {field['name']}.")
        }

        # Handle nested RECORD types
        if field["type"] == "RECORD" and field["fields"]:
            schema_field["properties"] = {
                sub_field["name"]: process_field(sub_field)
                for sub_field in field["fields"]
            }

        # Handle nullable fields using `oneOf`
        if field["mode"] == "NULLABLE":
            schema_field = {
                "oneOf": [
                    {"type": "null"},
                    schema_field
                ]
            }

        # Handle repeated fields as arrays
        if field["mode"] == "REPEATED":
            schema_field = {
                "type": "array",
                "items": schema_field
            }

        return schema_field

    # Build the JSON schema
    json_schema = {
        "$id": "http://example.com/myURI.schema.json",
        "$schema": "http://json-schema.org/draft-07/schema#",
        "title": "Generated Schema",
        "description": "Schema generated from BigQuery table.",
        "type": "object",
        "properties": {
            field["name"]: process_field(field)
            for field in bq_schema
        },
        "required": [field["name"] for field in bq_schema if field["mode"] == "REQUIRED"],
        "additionalProperties": False
    }

    return json_schema

# Read BigQuery schema from file
with open('payload_in.json', 'r') as infile:
    bq_schema = json.load(infile)

# Convert to Schema Registry compatible schema
schema_registry_payload = bq_to_schema_registry(bq_schema)

# Write Schema Registry schema to file
with open('payload_out.json', 'w') as outfile:
    json.dump(schema_registry_payload, outfile, indent=4)
