package com.Hamalog.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

/**
 * Firebase 설정
 * FCM(Firebase Cloud Messaging) Push 알림을 위한 Firebase 초기화
 */
@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${firebase.credentials.path:}")
    private String firebaseCredentialsPath;

    @Value("${firebase.enabled:false}")
    private boolean firebaseEnabled;

    @PostConstruct
    public void initialize() {
        if (!firebaseEnabled) {
            log.info("Firebase is disabled. Push notifications will not be sent.");
            return;
        }

        if (FirebaseApp.getApps().isEmpty()) {
            try {
                InputStream serviceAccount = getServiceAccountStream();
                if (serviceAccount == null) {
                    log.warn("Firebase credentials not found. Push notifications will not work.");
                    return;
                }

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("Firebase application initialized successfully.");

            } catch (IOException e) {
                log.error("Failed to initialize Firebase: {}", e.getMessage());
            }
        } else {
            log.info("Firebase application already initialized.");
        }
    }

    /**
     * 서비스 계정 자격 증명 스트림 획득
     * 우선순위: 환경 변수 경로 > classpath 경로
     */
    private InputStream getServiceAccountStream() throws IOException {
        // 환경 변수 또는 설정 파일에서 지정한 경로
        if (firebaseCredentialsPath != null && !firebaseCredentialsPath.isEmpty()) {
            return new FileInputStream(firebaseCredentialsPath);
        }

        // classpath에서 기본 위치 확인
        try {
            ClassPathResource resource = new ClassPathResource("firebase-service-account.json");
            if (resource.exists()) {
                return resource.getInputStream();
            }
        } catch (IOException e) {
            log.debug("Firebase credentials not found in classpath: {}", e.getMessage());
        }

        return null;
    }
}
