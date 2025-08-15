#!/usr/bin/env python3
"""
Test script for Kakao OAuth2 endpoint /oauth2/auth/kakao
This script tests if the endpoint correctly redirects to Kakao authorization server.
"""

import requests
import sys
from urllib.parse import urlparse, parse_qs

def test_kakao_oauth_endpoint():
    """Test the /oauth2/auth/kakao endpoint"""
    
    # Test endpoint URL
    base_url = "http://localhost:8080"
    endpoint = "/oauth2/auth/kakao"
    test_url = f"{base_url}{endpoint}"
    
    print(f"Testing Kakao OAuth2 endpoint: {test_url}")
    print("-" * 50)
    
    try:
        # Make request with allow_redirects=False to capture the redirect
        response = requests.get(test_url, allow_redirects=False)
        
        print(f"Response Status Code: {response.status_code}")
        print(f"Response Headers: {dict(response.headers)}")
        
        # Check if it's a redirect response
        if response.status_code == 302:
            location = response.headers.get('Location')
            if location:
                print(f"Redirect Location: {location}")
                
                # Parse the redirect URL to validate parameters
                parsed_url = urlparse(location)
                query_params = parse_qs(parsed_url.query)
                
                print("\nRedirect URL Analysis:")
                print(f"Scheme: {parsed_url.scheme}")
                print(f"Host: {parsed_url.netloc}")
                print(f"Path: {parsed_url.path}")
                print(f"Query Parameters:")
                
                expected_params = ['response_type', 'client_id', 'scope', 'state', 'redirect_uri']
                
                for param in expected_params:
                    if param in query_params:
                        print(f"  ✓ {param}: {query_params[param][0]}")
                    else:
                        print(f"  ✗ {param}: MISSING")
                
                # Validate if it's redirecting to Kakao
                if 'kauth.kakao.com' in parsed_url.netloc:
                    print("\n✓ SUCCESS: Correctly redirecting to Kakao authorization server")
                    return True
                else:
                    print(f"\n✗ ERROR: Not redirecting to Kakao (redirecting to {parsed_url.netloc})")
                    return False
            else:
                print("\n✗ ERROR: Redirect response but no Location header")
                return False
        else:
            print(f"\n✗ ERROR: Expected 302 redirect, got {response.status_code}")
            if response.text:
                print(f"Response body: {response.text}")
            return False
            
    except requests.exceptions.ConnectionError:
        print("✗ ERROR: Cannot connect to the application. Make sure it's running on localhost:8080")
        return False
    except Exception as e:
        print(f"✗ ERROR: {str(e)}")
        return False

if __name__ == "__main__":
    print("Kakao OAuth2 Endpoint Test")
    print("=" * 50)
    
    success = test_kakao_oauth_endpoint()
    
    if success:
        print("\n🎉 Test PASSED: Kakao OAuth2 endpoint is working correctly!")
        sys.exit(0)
    else:
        print("\n❌ Test FAILED: There are issues with the Kakao OAuth2 endpoint")
        sys.exit(1)