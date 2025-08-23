FROM openjdk:21-jdk

# Set working directory
WORKDIR /app

# Copy the JAR file
COPY build/libs/*.jar app.jar

# Declare environment variables for documentation and validation
ENV SPRING_PROFILES_ACTIVE=prod
# Note: JWT_SECRET should be set at runtime via docker run -e JWT_SECRET=value
# Do not declare JWT_SECRET with empty value here as it overrides fallback values
ENV JWT_EXPIRY=3600000
ENV SPRING_DATASOURCE_URL=""
ENV SPRING_DATASOURCE_USERNAME=""
ENV SPRING_DATASOURCE_PASSWORD=""
ENV SPRING_DATA_REDIS_HOST=localhost
ENV SPRING_DATA_REDIS_PORT=6379
ENV KAKAO_CLIENT_ID=""
ENV KAKAO_CLIENT_SECRET=""

# Expose the port the application runs on
EXPOSE 8080

# Health check to ensure the application is running properly
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application with environment variable validation
ENTRYPOINT ["sh", "-c", "\
    echo 'Starting Hamalog application...' && \
    echo 'Active Profile: ${SPRING_PROFILES_ACTIVE}' && \
    if [ -z \"$JWT_SECRET\" ]; then \
        echo 'INFO: JWT_SECRET environment variable is not set. Using fallback value from application properties.' && \
        echo 'For production deployment, set JWT_SECRET via: docker run -e JWT_SECRET=your-secret'; \
    elif [ \"$JWT_SECRET\" = \"\" ]; then \
        echo 'WARNING: JWT_SECRET is set but empty. This may cause startup failures.' && \
        echo 'Either unset JWT_SECRET or provide a valid Base64 value.'; \
    else \
        echo 'INFO: JWT_SECRET environment variable is properly configured.'; \
    fi && \
    java -Djava.security.egd=file:/dev/./urandom \
         -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} \
         -jar app.jar"]
