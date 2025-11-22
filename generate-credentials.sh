#!/bin/bash

# Hamalog Credentials Generator
# This script generates secure credentials for production deployment

set -e

echo "========================================="
echo "Hamalog Credentials Generator"
echo "========================================="
echo ""

# Create .env.prod if it doesn't exist
if [ -f ".env.prod" ]; then
    echo "⚠️  .env.prod already exists. Backup will be created."
    cp .env.prod .env.prod.backup.$(date +%Y%m%d_%H%M%S)
fi

echo "Generating secure credentials..."
echo ""

# Generate JWT Secret (256-bit)
JWT_SECRET=$(openssl rand -base64 32)
echo "✅ JWT Secret generated"

# Generate Encryption Key (256-bit)
ENCRYPTION_KEY=$(openssl rand -base64 32)
echo "✅ Encryption Key generated"

# Generate DB Passwords
DB_PASSWORD=$(openssl rand -base64 32 | tr -d "=+/" | cut -c1-32)
MYSQL_ROOT_PASSWORD=$(openssl rand -base64 32 | tr -d "=+/" | cut -c1-32)
REDIS_PASSWORD=$(openssl rand -base64 32 | tr -d "=+/" | cut -c1-32)
echo "✅ Database passwords generated"

# Create .env.prod file
cat > .env.prod << EOF
# Hamalog Production Environment Variables
# Generated on: $(date)
# DO NOT commit this file to Git

# =============================================================================
# DATABASE CONFIGURATION
# =============================================================================
DB_NAME=Hamalog
DB_USERNAME=hamalog_user
DB_PASSWORD=${DB_PASSWORD}
MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}
SPRING_DATASOURCE_USERNAME=hamalog_user
SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
SPRING_DATASOURCE_URL=jdbc:mysql://mysql-hamalog:3306/Hamalog?useSSL=true&requireSSL=true&characterEncoding=UTF-8&serverTimezone=UTC

# =============================================================================
# JWT CONFIGURATION
# =============================================================================
JWT_SECRET=${JWT_SECRET}
JWT_EXPIRY=900000
JWT_REFRESH_TOKEN_EXPIRY=604800000

# =============================================================================
# DATA ENCRYPTION KEY (AES-256)
# =============================================================================
HAMALOG_ENCRYPTION_KEY=${ENCRYPTION_KEY}

# =============================================================================
# KAKAO OAUTH2 CONFIGURATION
# =============================================================================
# ⚠️  IMPORTANT: Set these values from Kakao Developers Console
KAKAO_CLIENT_ID=YOUR_KAKAO_CLIENT_ID_HERE
KAKAO_CLIENT_SECRET=YOUR_KAKAO_CLIENT_SECRET_HERE
KAKAO_REDIRECT_URI=http://your-domain.com/oauth2/auth/kakao/callback

# =============================================================================
# REDIS CONFIGURATION
# =============================================================================
SPRING_DATA_REDIS_HOST=redis
SPRING_DATA_REDIS_PORT=6379
SPRING_DATA_REDIS_PASSWORD=${REDIS_PASSWORD}

# =============================================================================
# SPRING CONFIGURATION
# =============================================================================
SPRING_PROFILES_ACTIVE=prod

# =============================================================================
# FRONTEND & CORS CONFIGURATION
# =============================================================================
FRONTEND_URL=http://your-domain.com
ALLOWED_ORIGINS=http://your-domain.com

# =============================================================================
# FILE UPLOAD CONFIGURATION
# =============================================================================
FILE_UPLOAD_DIR=/data/hamalog/uploads
FILE_UPLOAD_MAX_SIZE=5242880

# =============================================================================
# LOGGING CONFIGURATION
# =============================================================================
LOG_DIR=/var/log/hamalog
EOF

chmod 600 .env.prod

echo ""
echo "========================================="
echo "✅ Credentials generated successfully!"
echo "========================================="
echo ""
echo "📁 File created: .env.prod"
echo "🔒 File permissions: 600 (owner read/write only)"
echo ""
echo "⚠️  IMPORTANT: Next steps:"
echo "1. Edit .env.prod and set KAKAO_CLIENT_ID and KAKAO_CLIENT_SECRET"
echo "2. Update KAKAO_REDIRECT_URI with your actual domain"
echo "3. Update FRONTEND_URL and ALLOWED_ORIGINS"
echo "4. NEVER commit .env.prod to Git"
echo "5. Store a backup in a secure location (password manager, vault)"
echo ""
echo "🔐 Security reminders:"
echo "- Rotate these credentials every 3-6 months"
echo "- Use different credentials for different environments"
echo "- Keep backups in secure, encrypted storage"
echo ""

