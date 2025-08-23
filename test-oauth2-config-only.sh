#!/bin/bash

echo "=== Testing OAuth2 Configuration Fix (Context Loading Only) ==="
echo "This test verifies that OAuth2 configuration loads without 'client id must not be empty' errors"
echo

# Set minimal required environment variables
export SPRING_PROFILES_ACTIVE=local
export JWT_SECRET="xcrVqYlPMcLeEoEX+h8vjxZ97lS6AETwQJXJSLJ/h8g="

echo "Test 1: Testing with undefined KAKAO environment variables"
echo "Expected: OAuth2EmptyVariableHandler should provide fallback values"

# Unset KAKAO variables to test fallback behavior
unset KAKAO_CLIENT_ID
unset KAKAO_CLIENT_SECRET
unset KAKAO_REDIRECT_URI

echo "Building application..."
./gradlew clean build -x test -q

if [ $? -eq 0 ]; then
    echo "✅ Build successful"
else
    echo "❌ Build failed"
    exit 1
fi

echo "Testing application context loading..."

# Create a simple test file to check if Spring context loads with OAuth2 configuration
cat > src/test/java/com/Hamalog/OAuth2ConfigurationTest.java << 'EOF'
package com.Hamalog;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
    "spring.profiles.active=local",
    "spring.main.web-application-type=none"
})
public class OAuth2ConfigurationTest {
    
    @Test
    public void contextLoads() {
        // This test passes if the Spring context loads successfully
        // without any OAuth2 "client id must not be empty" errors
        System.out.println("[DEBUG_LOG] OAuth2 configuration test passed - no client ID errors");
    }
}
EOF

echo "Running OAuth2 configuration test..."
./gradlew test --tests "OAuth2ConfigurationTest" -q

if [ $? -eq 0 ]; then
    echo "✅ Test 1 PASSED: Application context loads without OAuth2 client ID errors"
else
    echo "❌ Test 1 FAILED: OAuth2 configuration issues detected"
    exit 1
fi

echo
echo "Test 2: Testing with empty KAKAO environment variables"
echo "Expected: OAuth2EmptyVariableHandler should handle empty strings properly"

# Set KAKAO variables to empty strings (simulating Docker issue)
export KAKAO_CLIENT_ID=""
export KAKAO_CLIENT_SECRET=""
export KAKAO_REDIRECT_URI=""

echo "Running OAuth2 configuration test with empty variables..."
./gradlew test --tests "OAuth2ConfigurationTest" -q

if [ $? -eq 0 ]; then
    echo "✅ Test 2 PASSED: Application handles empty OAuth2 environment variables correctly"
else
    echo "❌ Test 2 FAILED: Issues with empty OAuth2 environment variables"
    exit 1
fi

# Clean up test file
rm -f src/test/java/com/Hamalog/OAuth2ConfigurationTest.java

echo
echo "=== All OAuth2 Configuration Tests Passed! ==="
echo "✅ The fixes successfully resolved the 'client id must not be empty' issue"
echo "✅ OAuth2EmptyVariableHandler properly handles both undefined and empty environment variables"
echo "✅ Application can start without OAuth2 configuration errors"
echo
echo "Summary of fixes applied:"
echo "1. Removed conflicting OAuth2 property overrides from application-prod.properties"
echo "2. Removed empty KAKAO environment variable declarations from Dockerfile"
echo "3. OAuth2EmptyVariableHandler now properly manages environment variable mapping"