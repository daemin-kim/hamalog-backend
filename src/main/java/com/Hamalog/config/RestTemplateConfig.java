package com.Hamalog.config;

import java.io.IOException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RestTemplateConfig {

    @Value("${oauth2.rest.connect-timeout:3s}")
    private Duration connectTimeout;

    @Value("${oauth2.rest.read-timeout:5s}")
    private Duration readTimeout;

    @Value("${oauth2.rest.connection-request-timeout:2s}")
    private Duration connectionRequestTimeout;

    @Value("${oauth2.rest.slow-threshold:1s}")
    private Duration slowLogThreshold;

    @Value("${oauth2.rest.pool.max-total:100}")
    private int maxConnectionsTotal;

    @Value("${oauth2.rest.pool.max-per-route:20}")
    private int maxConnectionsPerRoute;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .requestFactory(this::httpRequestFactory)
                .additionalInterceptors(new TimingLoggingInterceptor(slowLogThreshold))
                .errorHandler(new OAuth2ResponseErrorHandler())
                .build();
    }

    private ClientHttpRequestFactory httpRequestFactory() {
        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnTotal(maxConnectionsTotal)
                .setMaxConnPerRoute(maxConnectionsPerRoute)
                .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(connectionRequestTimeout.toMillis()))
                .setConnectTimeout(Timeout.ofMilliseconds(connectTimeout.toMillis()))
                .setResponseTimeout(Timeout.ofMilliseconds(readTimeout.toMillis()))
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .disableAutomaticRetries()
                .build();

        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }

    static class TimingLoggingInterceptor implements ClientHttpRequestInterceptor {
        private final Duration slowThreshold;

        TimingLoggingInterceptor(Duration slowThreshold) {
            this.slowThreshold = slowThreshold;
        }

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
            long start = System.nanoTime();
            try {
                ClientHttpResponse response = execution.execute(request, body);
                logIfSlow(request, response, System.nanoTime() - start);
                return response;
            } catch (IOException ex) {
                log.warn("[OAUTH2_HTTP] Request to {} failed: {}", request.getURI(), ex.getMessage());
                throw ex;
            }
        }

        private void logIfSlow(HttpRequest request, ClientHttpResponse response, long nanosElapsed) throws IOException {
            Duration elapsed = Duration.ofNanos(nanosElapsed);
            if (elapsed.compareTo(slowThreshold) >= 0) {
                log.warn("[OAUTH2_HTTP] Slow request detected method={} uri={} status={} elapsedMs={}",
                        request.getMethod(), request.getURI(), response.getStatusCode(), elapsed.toMillis());
            } else if (log.isDebugEnabled()) {
                log.debug("[OAUTH2_HTTP] method={} uri={} status={} elapsedMs={}",
                        request.getMethod(), request.getURI(), response.getStatusCode(), elapsed.toMillis());
            }
        }
    }

    static class OAuth2ResponseErrorHandler extends DefaultResponseErrorHandler {
        @Override
        public void handleError(ClientHttpResponse response) throws IOException {
            log.error("[OAUTH2_HTTP] Upstream error status={} message={}", response.getStatusCode(), response.getStatusText());
            super.handleError(response);
        }
    }
}