FROM eclipse-temurin:21-jre-alpine

# Install curl for healthcheck
RUN apk add --no-cache curl

WORKDIR /app

# Copy the built jar from Gradle build (expected to exist in CI before docker build)
COPY build/libs/*.jar app.jar

# Default runtime configuration (override in deployment)
ENV SPRING_PROFILES_ACTIVE=prod
ENV JWT_EXPIRY=3600000
ENV SPRING_DATASOURCE_URL=""
ENV SPRING_DATASOURCE_USERNAME=""
ENV SPRING_DATASOURCE_PASSWORD=""
ENV SPRING_DATA_REDIS_HOST=localhost
ENV SPRING_DATA_REDIS_PORT=6379

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "\
    echo 'Hamalog 애플리케이션을 시작합니다...' && \
    echo '활성 프로필: ${SPRING_PROFILES_ACTIVE}' && \
    echo '키 관리: HashiCorp Vault를 통해 모든 보안 키가 관리됩니다.' && \
    java -Djava.security.egd=file:/dev/./urandom \
         -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} \
         -jar app.jar"]
