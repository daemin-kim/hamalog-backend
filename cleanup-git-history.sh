#!/bin/bash

# Git History Cleanup Script for Hamalog
# This script removes sensitive files from Git history

set -e

echo "========================================="
echo "⚠️  Git History Cleanup - Hamalog"
echo "========================================="
echo ""
echo "This script will remove sensitive files from Git history."
echo "This operation will rewrite Git history and requires force push."
echo ""

# Warning prompt
read -p "Are you sure you want to continue? (yes/no): " confirm
if [ "$confirm" != "yes" ]; then
    echo "Operation cancelled."
    exit 0
fi

echo ""
echo "Creating backup branch..."
git branch backup-before-cleanup-$(date +%Y%m%d_%H%M%S)

echo ""
echo "Removing sensitive files from Git history..."

# List of sensitive files to remove
SENSITIVE_FILES=(
    ".env.prod"
    ".env.prod-local"
    "src/main/resources/application-prod.properties"
    "src/main/resources/application-local.properties"
)

# Build git filter-branch command
FILTER_CMD="git rm --cached --ignore-unmatch"
for file in "${SENSITIVE_FILES[@]}"; do
    FILTER_CMD="$FILTER_CMD $file"
done

# Execute filter-branch
git filter-branch --force --index-filter \
    "$FILTER_CMD" \
    --prune-empty --tag-name-filter cat -- --all

echo ""
echo "Cleaning up..."
rm -rf .git/refs/original/
git reflog expire --expire=now --all
git gc --prune=now --aggressive

echo ""
echo "========================================="
echo "✅ Git history cleaned successfully!"
echo "========================================="
echo ""
echo "📋 Next steps:"
echo ""
echo "1. Verify changes:"
echo "   git log --all --oneline | head -20"
echo ""
echo "2. Check that sensitive files are not tracked:"
echo "   git ls-files | grep -E '\\.env|application.*properties'"
echo ""
echo "3. Force push to remote (⚠️  WARNING: This is destructive!):"
echo "   git push origin --force --all"
echo "   git push origin --force --tags"
echo ""
echo "4. Notify all team members to re-clone the repository"
echo ""
echo "5. Rotate ALL exposed credentials immediately:"
echo "   - JWT Secret"
echo "   - Encryption Key"
echo "   - Kakao Client Secret"
echo "   - Database passwords"
echo "   - Redis password"
echo ""
echo "⚠️  IMPORTANT:"
echo "- All forks and clones will still contain old history"
echo "- Rotate credentials even after cleanup"
echo "- GitHub/GitLab caches may retain old commits for a while"
echo ""

