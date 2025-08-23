# Docker Environment Variables Configuration Guide

## Overview

This document explains how environment variables are configured and prioritized in the Hamalog Docker container deployment, with specific focus on Windows container deployment scenarios.

## Environment Variable Priority

The Hamalog application follows Spring Boot's standard environment variable resolution order:

### Priority Order (Highest to Lowest)
1. **🥇 Runtime Environment Variables** - Variables passed to `docker run` command using `-e` flag
2. **🥈 Dockerfile ENV Variables** - Variables declared in Dockerfile with `ENV` statements  
3. **🥉 Application Properties Fallbacks** - Default values specified in `application.properties` files

### Example Priority Resolution
```properties
# In application.properties
jwt.secret=${JWT_SECRET:xcrVqYlPMcLeEoEX+h8vjxZ97lS6AETwQJXJSLJ/h8g=}
```

- If `JWT_SECRET` environment variable is set → **Uses environment variable value**
- If `JWT_SECRET` is not set → **Uses fallback value** `xcrVqYlPMcLeEoEX+h8vjxZ97lS6AETwQJXJSLJ/h8g=`

## Dockerfile Environment Variable Configuration

### Current Dockerfile Environment Variables

```dockerfile
# Declare environment variables for documentation and validation
ENV SPRING_PROFILES_ACTIVE=prod
ENV JWT_SECRET=""
ENV JWT_EXPIRY=3600000
ENV SPRING_DATASOURCE_URL=""
ENV SPRING_DATASOURCE_USERNAME=""
ENV SPRING_DATASOURCE_PASSWORD=""
ENV SPRING_DATA_REDIS_HOST=localhost
ENV SPRING_DATA_REDIS_PORT=6379
ENV KAKAO_CLIENT_ID=""
ENV KAKAO_CLIENT_SECRET=""
```

### Environment Variable Validation

The Dockerfile includes validation logic in the ENTRYPOINT:

```dockerfile
ENTRYPOINT ["sh", "-c", "\
    echo 'Starting Hamalog application...' && \
    echo 'Active Profile: ${SPRING_PROFILES_ACTIVE}' && \
    if [ -z \"$JWT_SECRET\" ]; then \
        echo 'WARNING: JWT_SECRET environment variable is not set. Using fallback value.' && \
        echo 'For production deployment, please set JWT_SECRET environment variable.'; \
    else \
        echo 'JWT_SECRET environment variable is properly configured.'; \
    fi && \
    java -Djava.security.egd=file:/dev/./urandom \
         -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} \
         -jar app.jar"]
```

## Windows Container Deployment

### PowerShell Commands for Windows

#### Basic Container Run
```powershell
# Run container with environment variables on Windows
docker run -d `
  --name hamalog-container `
  -p 8080:8080 `
  -e JWT_SECRET=$env:JWT_SECRET `
  -e JWT_EXPIRY=3600000 `
  -e SPRING_DATASOURCE_URL="jdbc:mysql://mysql-host:3306/hamalog" `
  -e SPRING_DATASOURCE_USERNAME="hamalog_user" `
  -e SPRING_DATASOURCE_PASSWORD=$env:DB_PASSWORD `
  -e KAKAO_CLIENT_ID=$env:KAKAO_CLIENT_ID `
  -e KAKAO_CLIENT_SECRET=$env:KAKAO_CLIENT_SECRET `
  hamalog-app:latest
```

#### Windows Environment Variable Setup
```powershell
# Set environment variables in PowerShell session
$env:JWT_SECRET = "your-base64-encoded-jwt-secret"
$env:DB_PASSWORD = "your-database-password"
$env:KAKAO_CLIENT_ID = "your-kakao-client-id"
$env:KAKAO_CLIENT_SECRET = "your-kakao-client-secret"

# Verify environment variables
echo "JWT_SECRET: $env:JWT_SECRET"
echo "DB_PASSWORD: $env:DB_PASSWORD"
```

#### Persistent Environment Variables (Windows)
```powershell
# Set system-wide environment variables (requires admin privileges)
[System.Environment]::SetEnvironmentVariable("JWT_SECRET", "your-jwt-secret", "Machine")
[System.Environment]::SetEnvironmentVariable("DB_PASSWORD", "your-db-password", "Machine")

# Set user-level environment variables
[System.Environment]::SetEnvironmentVariable("JWT_SECRET", "your-jwt-secret", "User")
```

## Docker Compose Configuration

### Environment Variable Passing in docker-compose.yml

```yaml
services:
  hamalog-app:
    build: .
    ports:
      - "8080:8080"
    environment:
      # Spring Profile Configuration
      - SPRING_PROFILES_ACTIVE=prod
      
      # Database Configuration
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql-hamalog:3306/${DB_NAME:-Hamalog}
      - SPRING_DATASOURCE_USERNAME=${SPRING_DATASOURCE_USERNAME:-hamalog_user}
      - SPRING_DATASOURCE_PASSWORD=${SPRING_DATASOURCE_PASSWORD:-hamalog_password}
      
      # JWT Configuration (reads from host environment)
      - JWT_SECRET=${JWT_SECRET:-EzUuJwKK4vLnvk5r7yAgdNP/sa1dL87febZhlayPGjI=}
      - JWT_EXPIRY=${JWT_EXPIRY:-3600000}
      
      # OAuth2 Configuration
      - KAKAO_CLIENT_ID=${KAKAO_CLIENT_ID:-dummy-client-id}
      - KAKAO_CLIENT_SECRET=${KAKAO_CLIENT_SECRET:-dummy-client-secret}
```

### Running Docker Compose on Windows
```powershell
# Set environment variables before running docker-compose
$env:JWT_SECRET = "your-jwt-secret"
$env:DB_PASSWORD = "your-db-password"
$env:KAKAO_CLIENT_ID = "your-kakao-client-id"
$env:KAKAO_CLIENT_SECRET = "your-kakao-client-secret"

# Run docker-compose
docker compose up -d --build
```

## GitHub Actions Integration

### Windows Self-Hosted Runner Configuration

The `.github/workflows/docker-build.yml` is configured for Windows deployment:

```yaml
jobs:
  build-and-push:
    runs-on: self-hosted  # Windows self-hosted runner
    
    steps:
      - name: 서버에 배포
        shell: powershell  # Uses PowerShell for Windows compatibility
        run: |
          # App container execution with environment variables
          docker run -d `
            --name hamalog-container `
            --network hamalog-network `
            -p 8080:8080 `
            -e SPRING_DATASOURCE_URL=jdbc:mysql://mysql-hamalog:3306/Hamalog `
            -e SPRING_DATASOURCE_USERNAME=user `
            -e SPRING_DATASOURCE_PASSWORD=password `
            -e SPRING_DATA_REDIS_HOST=redis-hamalog `
            -e SPRING_DATA_REDIS_PORT=6379 `
            -e JWT_SECRET=${{ secrets.JWT_SECRET }} `
            -e JWT_EXPIRY=3600000 `
            -e KAKAO_CLIENT_ID=${{ secrets.KAKAO_CLIENT_ID }} `
            -e KAKAO_CLIENT_SECRET=${{ secrets.KAKAO_CLIENT_SECRET }} `
            ghcr.io/daemin-kim/hamalog-backend/hamalog-app:latest
```

### Required GitHub Secrets

Set these secrets in your GitHub repository settings:

- `JWT_SECRET` - Base64-encoded 256-bit JWT signing key
- `KAKAO_CLIENT_ID` - Kakao OAuth2 client ID
- `KAKAO_CLIENT_SECRET` - Kakao OAuth2 client secret

## Application Properties Configuration

### Environment Variable Integration

All critical configuration values use environment variables with fallbacks:

```properties
# JWT Configuration - Environment variable prioritized
jwt.secret=${JWT_SECRET:xcrVqYlPMcLeEoEX+h8vjxZ97lS6AETwQJXJSLJ/h8g=}
jwt.expiry=${JWT_EXPIRY:3600000}

# Database Configuration - Environment variable prioritized
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:h2:mem:hamalog;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:sa}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:}

# Redis Configuration - Environment variable prioritized
spring.data.redis.host=${SPRING_DATA_REDIS_HOST:localhost}
spring.data.redis.port=${SPRING_DATA_REDIS_PORT:6379}

# OAuth2 Configuration - Environment variable prioritized
spring.security.oauth2.client.registration.kakao.client-id=${KAKAO_CLIENT_ID:dummy-client-id-for-production}
spring.security.oauth2.client.registration.kakao.client-secret=${KAKAO_CLIENT_SECRET:dummy-client-secret-for-production}
```

## Security Best Practices

### JWT Secret Generation
```bash
# Generate secure 256-bit JWT secret
openssl rand -base64 32

# Example output: dGVzdC1qd3Qtc2VjcmV0LWZvci1kb2NrZXItdGVzdGluZw==
```

### Windows Environment Variable Security
```powershell
# Use Windows Credential Manager for sensitive data
cmdkey /generic:hamalog-jwt /user:jwt /pass:"your-jwt-secret"

# Or use encrypted strings in PowerShell
$secureString = ConvertTo-SecureString "your-jwt-secret" -AsPlainText -Force
```

## Troubleshooting

### Common Issues on Windows

1. **PowerShell Execution Policy**
   ```powershell
   # Allow script execution
   Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
   ```

2. **Environment Variable Not Found**
   ```powershell
   # Check if environment variable is set
   echo $env:JWT_SECRET
   
   # List all environment variables
   Get-ChildItem Env:
   ```

3. **Docker Network Issues**
   ```powershell
   # Create Docker network if not exists
   docker network create hamalog-network
   
   # List networks
   docker network ls
   ```

### Container Logs
```powershell
# View container logs
docker logs hamalog-container

# Follow logs in real-time
docker logs -f hamalog-container

# View last 100 lines
docker logs --tail 100 hamalog-container
```

## Testing

Use the provided test script to verify environment variable configuration:

```bash
# Run the environment variable test script
./test-docker-env-vars.sh
```

The test script validates:
- ✅ Environment variables are properly declared in Dockerfile
- ✅ Container accepts and uses custom environment variables
- ✅ Environment variable priority works correctly
- ✅ Application starts successfully with environment variables

## Summary

The Hamalog Docker configuration properly handles environment variables with the following characteristics:

- **Environment Variable Priority**: Runtime env vars override Dockerfile env vars override property fallbacks
- **Windows Compatibility**: PowerShell syntax supported in GitHub Actions and deployment scripts
- **Security**: Sensitive values (JWT_SECRET, OAuth2 secrets) are passed via environment variables
- **Flexibility**: Fallback values ensure application starts even without all environment variables
- **Validation**: Container startup validates critical environment variables and provides helpful warnings

The configuration ensures that environment variables take priority over configuration files, making it suitable for secure production deployment on Windows containers.