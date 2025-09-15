# Hamalog Vault Configuration

This directory contains initialization scripts and configuration files for HashiCorp Vault integration in the Hamalog application.

## Overview

The Vault setup provides secure secret management for:
- JWT signing secrets
- Data encryption keys
- OAuth2 client credentials (Kakao)
- Other sensitive configuration data

## Development Mode

The current configuration runs Vault in development mode, which:
- Uses in-memory storage (data is lost when container restarts)
- Automatically unseals the vault
- Uses a static root token for simplicity
- **NOT suitable for production use**

### Development Configuration
- **Root Token**: `hamalog-dev-token`
- **Vault URL**: `http://localhost:8200`
- **KV Backend**: `secret` (KV v2)
- **Default Context**: `hamalog`

## Secret Structure

Secrets are stored in the KV v2 engine at path `secret/hamalog`:

```
secret/hamalog/
├── jwt-secret          # JWT signing key (Base64 encoded)
├── encryption-key      # AES encryption key for sensitive data
├── kakao-client-id     # Kakao OAuth2 client ID
└── kakao-client-secret # Kakao OAuth2 client secret
```

## Initialization Process

1. **Vault Service**: Starts in development mode with auto-unseal
2. **Vault-Init Service**: Waits for Vault to be healthy, then:
   - Enables KV v2 secrets engine at `secret/`
   - Creates initial secrets in `secret/hamalog` context
   - Uses environment variables as fallback values
   - Exits after successful initialization

## Application Integration

The Hamalog application:
- Uses `VaultKeyProvider` service to retrieve secrets
- Falls back to environment variables if Vault is unavailable
- Configurable via `hamalog.vault.enabled` property

### Environment Variables Mapping

| Environment Variable | Vault Secret Path | Purpose |
|---------------------|-------------------|---------|
| `JWT_SECRET` | `secret/hamalog:jwt-secret` | JWT token signing |
| `HAMALOG_ENCRYPTION_KEY` | `secret/hamalog:encryption-key` | Data encryption |
| `KAKAO_CLIENT_ID` | `secret/hamalog:kakao-client-id` | OAuth2 client ID |
| `KAKAO_CLIENT_SECRET` | `secret/hamalog:kakao-client-secret` | OAuth2 client secret |

## Production Considerations

For production deployment, consider:

1. **Persistent Storage**: Use external storage backend (Consul, etcd, etc.)
2. **Auto-Unsealing**: Configure cloud auto-unseal or implement proper unsealing process
3. **TLS Encryption**: Enable HTTPS for Vault API
4. **Access Control**: Implement proper authentication and authorization policies
5. **High Availability**: Deploy multiple Vault instances
6. **Backup Strategy**: Regular backups of Vault data
7. **Monitoring**: Health checks and audit logging

## Security Notes

⚠️ **Development Only**: The current setup is for development purposes only
⚠️ **Static Token**: Never use static tokens in production
⚠️ **Network Security**: Ensure Vault is not exposed to public networks
⚠️ **Audit Logging**: Enable audit logging for production deployments

## Troubleshooting

### Common Issues

1. **Vault Initialization Fails**
   - Check if Vault service is healthy
   - Verify network connectivity between services
   - Check logs: `docker-compose logs vault-init`

2. **Application Can't Connect to Vault**
   - Verify `HAMALOG_VAULT_URI` points to correct Vault instance
   - Check token validity and permissions
   - Ensure Vault is unsealed and responsive

3. **Secrets Not Found**
   - Verify secret path: `/v1/secret/data/hamalog`
   - Check if KV v2 engine is enabled
   - Confirm initialization completed successfully

### Useful Commands

```bash
# Check Vault status
docker-compose exec vault vault status

# List secrets
docker-compose exec vault vault kv list secret/

# Read specific secret
docker-compose exec vault vault kv get secret/hamalog

# Write new secret
docker-compose exec vault vault kv put secret/hamalog key=value
```

## File Structure

```
vault-init/
├── README.md           # This documentation
├── vault-config.hcl    # Production Vault configuration
└── init-secrets.sh     # Manual initialization script
```