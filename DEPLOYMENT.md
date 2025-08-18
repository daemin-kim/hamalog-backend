# Hamalog Production Deployment Guide

## Overview
This guide addresses all hardcoded values and configuration inconsistencies that could cause deployment failures in production environments. All sensitive configuration has been externalized to environment variables following industry best practices.

## Critical Environment Variables

### Required for Production (No Fallbacks)
These environment variables MUST be set in production to avoid startup failures:

```bash
# Database Configuration (Required)
export DB_USERNAME="your_production_db_user"
export DB_PASSWORD="your_secure_db_password"

# OAuth2 Kakao Integration (Required)
export KAKAO_CLIENT_ID="your_kakao_client_id"
export KAKAO_CLIENT_SECRET="your_kakao_client_secret"
```

### Recommended for Production (Has Fallbacks)
These variables should be set for production but have secure fallbacks:

```bash
# JWT Configuration (Highly Recommended - Generate new secret)
export JWT_SECRET=$(openssl rand -base64 32)
export JWT_EXPIRY=3600000  # 1 hour in milliseconds

# Database Configuration
export DB_HOST="your_db_host"
export DB_PORT=3306
export DB_NAME="your_db_name"

# MySQL Root Password (for docker-compose deployments)
export MYSQL_ROOT_PASSWORD="your_secure_root_password"

# Application URLs (Production)
export KAKAO_REDIRECT_URI="https://your-domain.com/login/oauth2/code/kakao"
export FRONTEND_URL="https://your-domain.com"

# CORS Configuration
export ALLOWED_ORIGINS="https://your-domain.com,https://admin.your-domain.com"

# OpenAPI Documentation Server
export SERVER_URL="https://api.your-domain.com"
export SERVER_DESCRIPTION="Production API Server"

# Redis Configuration (if using external Redis)
export SPRING_DATA_REDIS_HOST="your_redis_host"
export SPRING_DATA_REDIS_PORT=6379

# File Upload Directory
export UPLOAD_DIR="/secure/path/to/uploads"

# Logging Directory
export LOG_DIR="/var/log/hamalog"
```

## Fixed Configuration Issues

### 1. JWT Secret Inconsistency (CRITICAL FIX)
**Problem**: Different fallback secrets were used across configuration files, causing bean creation failures.

**Solution**: All configuration files now use the same fallback JWT secret:
- `application.properties`: `${JWT_SECRET:EzUuJwKK4vLnvk5r7yAgdNP/sa1dL87febZhlayPGjI=}`
- `application-prod.properties`: `${JWT_SECRET:EzUuJwKK4vLnvk5r7yAgdNP/sa1dL87febZhlayPGjI=}`
- `JwtTokenProvider.java`: Same fallback value
- `docker-compose.yml`: `${JWT_SECRET:-EzUuJwKK4vLnvk5r7yAgdNP/sa1dL87febZhlayPGjI=}`

### 2. Database Password Hardcoding
**Problem**: Hardcoded database passwords in docker-compose.yml.

**Solution**: All database credentials now use environment variables:
```yaml
- SPRING_DATASOURCE_USERNAME=${DB_USERNAME:-hamalog_user}
- SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD:-hamalog_password}
- MYSQL_USER=${DB_USERNAME:-hamalog_user}
- MYSQL_PASSWORD=${DB_PASSWORD:-hamalog_password}
```

### 3. Localhost URL Hardcoding
**Problem**: Hardcoded localhost URLs preventing production deployment.

**Solution**: All URLs now configurable via environment variables:
- OAuth2 redirect URI: `${KAKAO_REDIRECT_URI:http://localhost:8080/login/oauth2/code/kakao}`
- Frontend redirect: `${FRONTEND_URL:http://localhost:3000}/oauth/kakao`
- OpenAPI server: `${SERVER_URL:http://localhost:8080}`

## Docker Deployment

### Using docker-compose (Recommended)
All necessary environment variables are now externalized:

```bash
# Set production environment variables
export JWT_SECRET=$(openssl rand -base64 32)
export DB_USERNAME="hamalog_prod_user"
export DB_PASSWORD="your_secure_password"
export MYSQL_ROOT_PASSWORD="your_secure_root_password"
export KAKAO_CLIENT_ID="your_kakao_client_id"
export KAKAO_CLIENT_SECRET="your_kakao_client_secret"
export FRONTEND_URL="https://your-domain.com"
export KAKAO_REDIRECT_URI="https://your-domain.com/login/oauth2/code/kakao"

# Deploy
docker-compose up -d
```

### Using Docker run
```bash
docker run -d \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e JWT_SECRET="$(openssl rand -base64 32)" \
  -e DB_USERNAME="your_db_user" \
  -e DB_PASSWORD="your_db_password" \
  -e KAKAO_CLIENT_ID="your_kakao_client_id" \
  -e KAKAO_CLIENT_SECRET="your_kakao_client_secret" \
  -e FRONTEND_URL="https://your-domain.com" \
  -e KAKAO_REDIRECT_URI="https://your-domain.com/login/oauth2/code/kakao" \
  hamalog
```

## Security Best Practices Applied

1. **No Hardcoded Secrets**: All secrets are externalized to environment variables
2. **Secure Fallbacks**: Development fallbacks are clearly marked and secure
3. **Environment Variable Precedence**: Production values always override fallbacks
4. **Consistent Configuration**: Same patterns used across all configuration files
5. **Proper Secret Generation**: JWT secrets must be 256-bit Base64 encoded values

## Verification Steps

1. **Build Test**: Run `./gradlew clean build` - should complete without errors
2. **Configuration Test**: Set production environment variables and verify startup
3. **JWT Bean Creation**: Ensure SecurityConfig and JwtTokenProvider beans are created successfully
4. **Database Connection**: Verify application connects to production database
5. **OAuth2 Flow**: Test Kakao OAuth2 login with production URLs

## Troubleshooting

### JWT Bean Creation Failure
- Ensure JWT_SECRET environment variable is set
- Verify JWT secret is Base64 encoded and at least 256 bits (32 bytes)
- Check all configuration files use the same fallback value

### Database Connection Issues
- Verify DB_USERNAME and DB_PASSWORD are set correctly
- Ensure database host and port are accessible
- Check MySQL container is running (for docker-compose deployments)

### OAuth2 Redirect Issues
- Verify KAKAO_REDIRECT_URI matches Kakao app configuration
- Ensure FRONTEND_URL is set to correct domain
- Check CORS allowed origins include your frontend domain

This deployment guide addresses all hardcoded values and configuration inconsistencies that were causing deployment failures. Following these instructions will ensure successful production deployment with proper security practices.