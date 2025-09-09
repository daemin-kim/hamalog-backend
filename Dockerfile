FROM openjdk:21-jdk

# 작업 디렉토리 설정
WORKDIR /app

# JAR 파일 복사
COPY build/libs/*.jar app.jar

# 문서화 및 검증을 위한 환경 변수 선언
ENV SPRING_PROFILES_ACTIVE=prod
# 참고: JWT_SECRET은 런타임에 docker run -e JWT_SECRET=value를 통해 설정해야 함
# JWT_SECRET을 빈 값으로 선언하지 말 것 - 기본값을 덮어쓰게 됨
ENV JWT_EXPIRY=3600000
ENV SPRING_DATASOURCE_URL=""
ENV SPRING_DATASOURCE_USERNAME=""
ENV SPRING_DATASOURCE_PASSWORD=""
ENV SPRING_DATA_REDIS_HOST=localhost
ENV SPRING_DATA_REDIS_PORT=6379
# KAKAO_CLIENT_ID와 KAKAO_CLIENT_SECRET은 런타임에 설정해야 함
# OAuth2EmptyVariableHandler의 정상 작동을 위해 빈 값으로 선언하지 말 것
# 설정 방법: docker run -e KAKAO_CLIENT_ID=your_id -e KAKAO_CLIENT_SECRET=your_secret

# 애플리케이션이 실행되는 포트 노출
EXPOSE 8080

# 애플리케이션이 정상 실행되는지 확인하는 상태 검사
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# 환경 변수 검증과 함께 애플리케이션 실행
ENTRYPOINT ["sh", "-c", "\
    echo 'Hamalog 애플리케이션을 시작합니다...' && \
    echo '활성 프로필: ${SPRING_PROFILES_ACTIVE}' && \
    if [ -z \"$JWT_SECRET\" ]; then \
        echo '정보: JWT_SECRET 환경 변수가 설정되지 않았습니다. application properties의 기본값을 사용합니다.' && \
        echo '프로덕션 배포 시에는 다음과 같이 JWT_SECRET을 설정하세요: docker run -e JWT_SECRET=your-secret'; \
    elif [ \"$JWT_SECRET\" = \"\" ]; then \
        echo '경고: JWT_SECRET이 설정되었지만 비어있습니다. 시작 실패가 발생할 수 있습니다.' && \
        echo 'JWT_SECRET을 해제하거나 유효한 Base64 값을 제공하세요.'; \
    else \
        echo '정보: JWT_SECRET 환경 변수가 올바르게 설정되었습니다.'; \
    fi && \
    if [ -z \"$HAMALOG_ENCRYPTION_KEY\" ]; then \
        echo '정보: HAMALOG_ENCRYPTION_KEY 환경 변수가 설정되지 않았습니다. application properties의 기본값을 사용합니다.' && \
        echo '프로덕션 배포 시에는 다음과 같이 HAMALOG_ENCRYPTION_KEY를 설정하세요: docker run -e HAMALOG_ENCRYPTION_KEY=your-key'; \
    elif [ \"$HAMALOG_ENCRYPTION_KEY\" = \"\" ]; then \
        echo '경고: HAMALOG_ENCRYPTION_KEY가 설정되었지만 비어있습니다. 시작 실패가 발생할 수 있습니다.' && \
        echo 'HAMALOG_ENCRYPTION_KEY를 해제하거나 유효한 Base64 값을 제공하세요.'; \
    else \
        echo '정보: HAMALOG_ENCRYPTION_KEY 환경 변수가 올바르게 설정되었습니다.'; \
    fi && \
    java -Djava.security.egd=file:/dev/./urandom \
         -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} \
         -jar app.jar"]
