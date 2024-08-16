Flow:

KafkaStreamProcesor --- Kafka Config -- Topology calls -- Stream Processor  -- Validator -- Unique Record Processor

Project Java Version:

Java 17.0.10

Kafka console producer inputs:

kafka-console-producer --broker-list localhost:9092 --topic input-topic-account-update --property "parse.key=true" --property "key.separator=:"

unique-event-id-04:{"event_id": "unique-event-id-04", "timestamp": "2024-07-26T15:00:00Z", "vault_version": {"major": 1, "minor": 1, "patch": 0, "label": "v1.1.0"}, "change_id": "change-id-003", "account_update_created": {"account_update": {"id": "update-001", "account_id": "acc-002", "status": "pending", "activation_update": {}, "create_timestamp": "2024-07-26T15:00:00Z", "last_status_update_timestamp": "2024-07-26T15:00:00Z", "account_update_batch_id": "batch-001", "job_id": "job-001", "failure_reason": ""}}, "account_update_updated": {"account_update": {"id": "update-001", "account_id": "acc-002", "status": "completed", "activation_update": {}, "create_timestamp": "2024-07-26T15:00:00Z", "last_status_update_timestamp": "2024-07-26T15:30:00Z", "account_update_batch_id": "batch-001", "job_id": "job-001", "failure_reason": ""}, "update_mask": {"paths": ["status", "last_status_update_timestamp"]}}}

kafka-console-producer --broker-list localhost:9092 --topic input-topic-account-create --property "parse.key=true" --property "key.separator=:"

unique-event-id-04:{"event_id": "unique-event-id-04", "timestamp": "2024-07-26T14:30:00Z", "vault_version": {"major": 1, "minor": 1, "patch": 0, "label": "v1.1.0"}, "change_id": "change-id-002", "account_created": {"account": {"id": "acc-002", "name": "Account Name 2", "product_id": "prod-002", "product_version_id": "v2", "permitted_denominations": ["USD", "EUR"], "status": "active", "opening_timestamp": "2024-07-26T14:30:00Z", "closing_timestamp": "2024-07-26T14:30:00Z", "stakeholder_ids": ["stakeholder-002"], "instance_param_vals": {"account_indicators": "indicator-002", "account_legal_entity": "entity-002", "account_max_balance": "20000", "account_min_balance": "2000", "account_rate": "6%", "account_term": "24 months", "early_closure": "no", "manual_remit": "no", "manual_statement": "no", "maturity_date": "2026-07-26", "remittance_account": "remit-002"}, "derived_instance_param_vals": {}, "details": {"account_type": "current", "brand": "Brand Name 2", "default_currency": "USD"}, "accounting": {"tside": "debit"}}}}