# Hamalog Production Vault Configuration
# This is a production-ready Vault configuration template
# Modify as needed for your deployment environment

# Storage backend - use appropriate backend for production
storage "file" {
  path = "/vault/data"
}

# Alternative storage backends for production:
# storage "consul" {
#   address = "consul:8500"
#   path    = "vault/"
# }

# storage "etcd" {
#   address     = "http://etcd:2379"
#   etcd_api    = "v3"
#   path        = "vault/"
# }

# Listener configuration
listener "tcp" {
  address       = "0.0.0.0:8200"
  tls_disable   = 1  # Set to 0 and configure TLS for production
  # tls_cert_file = "/vault/tls/vault.crt"
  # tls_key_file  = "/vault/tls/vault.key"
}

# API address
api_addr = "http://vault:8200"

# Cluster address (for HA deployments)
cluster_addr = "http://vault:8201"

# UI configuration
ui = true

# Logging
log_level = "INFO"
log_format = "json"

# Enable audit logging (recommended for production)
# audit {
#   file {
#     file_path = "/vault/logs/audit.log"
#   }
# }

# Disable mlock if running in containers without IPC_LOCK capability
disable_mlock = true

# Maximum lease TTL
max_lease_ttl = "768h"
default_lease_ttl = "24h"

# Plugin directory
plugin_directory = "/vault/plugins"

# Raw storage endpoint (disable for production)
raw_storage_endpoint = false

# Performance optimizations
cache_size = "32000"

# Seal configuration for production
# Use auto-unseal with cloud providers or hardware security modules
# seal "awskms" {
#   region     = "us-west-2"
#   kms_key_id = "alias/vault-unseal-key"
# }

# seal "gcpckms" {
#   project     = "vault-project"
#   region      = "global"
#   key_ring    = "vault-keyring"
#   crypto_key  = "vault-key"
# }

# seal "azurekeyvault" {
#   tenant_id      = "46646709-b63e-4747-be42-516edeaf1e14"
#   client_id      = "03dc33fc-16d9-4b77-8049-7d110d65c179"
#   client_secret  = "DUJDS3..."
#   vault_name     = "hc-vault"
#   key_name       = "vault_key"
# }

# Telemetry configuration (optional)
telemetry {
  prometheus_retention_time = "24h"
  disable_hostname = true
}

# Service discovery and load balancer health checks
service_registration "consul" {
  address = "consul:8500"
  service = "vault"
  tags    = ["hamalog", "vault"]
  # check_timeout = "5s"
  # service_tags  = "active"
}