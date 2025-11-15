// React Native - 카카오 OAuth2 로그인 구현 예제
// 백엔드와 연동하는 방법

import { useEffect } from 'react';
import { useRoute, useNavigation } from '@react-navigation/native';
import { Linking } from 'react-native';
import * as SecureStore from 'expo-secure-store';

/**
 * OAuth2 로그인 흐름
 *
 * 1. 사용자가 "카카오 로그인" 버튼 클릭
 * 2. 웹뷰 또는 브라우저에서 백엔드의 /oauth2/auth/kakao 호출
 * 3. 백엔드가 카카오 인증 서버로 리다이렉트
 * 4. 사용자가 카카오에서 인증
 * 5. 카카오가 백엔드의 /oauth2/auth/kakao/callback으로 콜백
 * 6. 백엔드가 JWT 토큰을 생성하고 hamalog-rn://auth?token=... 으로 리다이렉트
 * 7. RN 앱이 딥링크를 받아서 처리
 */

// ==================== 딥링크 설정 ====================
// 1. app.json에 다음 설정 추가:
/*
{
  "expo": {
    "scheme": "hamalog-rn",
    "plugins": [
      [
        "expo-build-properties",
        {
          "android": {
            "usesCleartextTraffic": true
          }
        }
      ]
    ]
  }
}
*/

// 2. React Navigation 설정:
/*
const linking = {
  prefixes: ['hamalog-rn://', 'https://hamalog.com'],
  config: {
    screens: {
      OAuth2Callback: 'auth',
      // 다른 스크린들...
    },
  },
};
*/

// ==================== OAuth2 콜백 핸들러 ====================
export function OAuth2CallbackScreen() {
  const route = useRoute();
  const navigation = useNavigation();

  useEffect(() => {
    const handleOAuth2Callback = async () => {
      try {
        // 딥링크 파라미터에서 토큰 추출
        const { token, error } = route.params || {};

        if (error) {
          // 에러 처리
          console.error('OAuth2 로그인 실패:', error);
          // 에러 화면으로 이동 또는 토스트 메시지 표시
          navigation.replace('LoginError', { error });
          return;
        }

        if (token) {
          // 토큰 저장
          console.log('JWT 토큰 받음:', token.substring(0, 20) + '...');
          await SecureStore.setItemAsync('authToken', token);

          // 홈 화면으로 이동
          navigation.replace('Home');
        }
      } catch (error) {
        console.error('OAuth2 콜백 처리 중 에러:', error);
        navigation.replace('LoginError', { error: error.message });
      }
    };

    handleOAuth2Callback();
  }, [route.params]);

  return (
    <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
      <ActivityIndicator size="large" color="#0000ff" />
      <Text style={{ marginTop: 20 }}>로그인 처리 중...</Text>
    </View>
  );
}

// ==================== 로그인 스크린 ====================
export function LoginScreen() {
  const handleKakaoLogin = async () => {
    try {
      // 백엔드 주소 (환경에 따라 변경)
      const BACKEND_URL = 'http://49.142.154.182:8080'; // 프로덕션
      // const BACKEND_URL = 'http://localhost:8080'; // 로컬 테스트

      const oauthStartUrl = `${BACKEND_URL}/oauth2/auth/kakao`;

      // 웹뷰에서 열기 또는 브라우저에서 열기
      // 옵션 1: WebView 사용 (앱 내부)
      // navigation.navigate('OAuthWebView', { url: oauthStartUrl });

      // 옵션 2: 기본 브라우저에서 열기
      await Linking.openURL(oauthStartUrl);

    } catch (error) {
      console.error('카카오 로그인 시작 실패:', error);
      Alert.alert('오류', '로그인 시작 중 문제가 발생했습니다');
    }
  };

  return (
    <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
      <TouchableOpacity
        onPress={handleKakaoLogin}
        style={{
          backgroundColor: '#FFE812',
          paddingHorizontal: 20,
          paddingVertical: 12,
          borderRadius: 8,
        }}
      >
        <Text style={{ fontSize: 16, fontWeight: 'bold', color: '#000' }}>
          카카오로 로그인
        </Text>
      </TouchableOpacity>
    </View>
  );
}

// ==================== WebView를 통한 OAuth 처리 (선택사항) ====================
import { WebView } from 'react-native-webview';

export function OAuthWebViewScreen({ route }) {
  const { url } = route.params;

  const handleWebViewNavigationStateChange = async (newNavState) => {
    const { url: currentUrl } = newNavState;

    // hamalog-rn:// 스킴 확인
    if (currentUrl.startsWith('hamalog-rn://')) {
      try {
        // URL 파싱
        const urlParams = new URL(currentUrl);
        const token = urlParams.searchParams.get('token');
        const error = urlParams.searchParams.get('error');

        if (error) {
          console.error('OAuth2 에러:', error);
          // 에러 처리
          return;
        }

        if (token) {
          // 토큰 저장
          console.log('JWT 토큰 저장:', token.substring(0, 20) + '...');
          await SecureStore.setItemAsync('authToken', token);

          // 홈 화면으로 이동
          navigation.replace('Home');
        }
      } catch (error) {
        console.error('WebView OAuth 처리 중 에러:', error);
      }
    }
  };

  return (
    <WebView
      source={{ uri: url }}
      onNavigationStateChange={handleWebViewNavigationStateChange}
      startInLoadingState={true}
      scalesPageToFit={true}
    />
  );
}

// ==================== API 요청 시 토큰 사용 ====================
import axios from 'axios';

// Axios 인터셉터 설정
const apiClient = axios.create({
  baseURL: 'http://49.142.154.182:8080/api', // 프로덕션
  // baseURL: 'http://localhost:8080/api', // 로컬 테스트
});

apiClient.interceptors.request.use(async (config) => {
  try {
    // SecureStore에서 토큰 읽기
    const token = await SecureStore.getItemAsync('authToken');

    if (token) {
      // Authorization 헤더에 토큰 추가
      config.headers.Authorization = `Bearer ${token}`;
    }
  } catch (error) {
    console.error('토큰 읽기 실패:', error);
  }

  return config;
});

// 사용 예:
/*
try {
  const response = await apiClient.get('/medications'); // Authorization 헤더 자동 추가
  console.log('약물 목록:', response.data);
} catch (error) {
  console.error('API 요청 실패:', error);
}
*/

// ==================== 로그아웃 ====================
export async function handleLogout() {
  try {
    // 토큰 삭제
    await SecureStore.deleteItemAsync('authToken');

    // 로그인 화면으로 이동
    navigation.replace('Login');
  } catch (error) {
    console.error('로그아웃 실패:', error);
  }
}

// ==================== 토큰 검증 ====================
export async function validateToken() {
  try {
    const token = await SecureStore.getItemAsync('authToken');

    if (!token) {
      return false;
    }

    // 선택사항: 백엔드에서 토큰 검증
    // const response = await apiClient.post('/auth/validate-token');
    // return response.status === 200;

    return true;
  } catch (error) {
    console.error('토큰 검증 실패:', error);
    return false;
  }
}

