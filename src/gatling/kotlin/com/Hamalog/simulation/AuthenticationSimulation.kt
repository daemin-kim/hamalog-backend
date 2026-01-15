package com.Hamalog.simulation

import io.gatling.javaapi.core.CoreDsl.*
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.*
import java.time.Duration

/**
 * 인증 플로우 성능 벤치마크 시뮬레이션
 *
 * 측정 항목:
 * - 로그인 API 응답 시간
 * - 토큰 갱신 API 응답 시간
 * - 동시 로그인 부하 처리 능력
 *
 * 실행 방법:
 * ./gradlew gatlingRun-com.Hamalog.simulation.AuthenticationSimulation
 */
class AuthenticationSimulation : Simulation() {

    private val baseUrl = System.getProperty("baseUrl", "http://localhost:8080")

    // 벤치마크 API 인증용 (프로덕션 환경에서 필요)
    private val benchmarkApiKey = System.getProperty("benchmarkApiKey", "")

    private val httpProtocol = http
        .baseUrl(baseUrl)
        .acceptHeader("application/json")
        .contentTypeHeader("application/json")
        .userAgentHeader("Gatling/3.11 - Auth Benchmark")
        // 벤치마크 API Key가 설정된 경우 헤더 추가
        .let { protocol ->
            if (benchmarkApiKey.isNotEmpty()) {
                protocol.header("X-Benchmark-API-Key", benchmarkApiKey)
            } else {
                protocol
            }
        }

    // ========================================
    // 1. 로그인 성능 측정
    // ========================================
    private val loginFlow = exec(
        http("Login")
            .post("/auth/login")
            .body(StringBody("""{"loginId":"benchmark@test.com","password":"Benchmark1234!"}"""))
            .check(status().`in`(200, 401))
            .check(jsonPath("$.access_token").optional().saveAs("accessToken"))
            .check(jsonPath("$.refresh_token").optional().saveAs("refreshToken")),
    )

    // ========================================
    // 2. 토큰 갱신 성능 측정
    // ========================================
    private val refreshTokenFlow = exec(
        http("Refresh Token")
            .post("/auth/refresh")
            .body(StringBody("""{"refreshToken":"#{refreshToken}"}"""))
            .check(status().`in`(200, 401))
            .check(jsonPath("$.access_token").optional().saveAs("newAccessToken")),
    )

    // ========================================
    // 3. 로그아웃 성능 측정
    // ========================================
    private val logoutFlow = exec(
        http("Logout")
            .post("/auth/logout")
            .header("Authorization", "Bearer #{accessToken}")
            .check(status().`in`(200, 401)),
    )

    // ========================================
    // 시나리오 정의
    // ========================================

    // 시나리오 1: 단순 로그인 부하
    private val loginLoadScenario = scenario("Login Load Test")
        .exec(loginFlow)
        .pause(Duration.ofMillis(100), Duration.ofMillis(500))

    // 시나리오 2: 전체 인증 플로우 (로그인 -> 토큰 갱신 -> 로그아웃)
    private val fullAuthFlowScenario = scenario("Full Auth Flow")
        .exec(
            http("Login for Full Flow")
                .post("/auth/login")
                .body(StringBody("""{"loginId":"benchmark@test.com","password":"Benchmark1234!"}"""))
                .check(status().`is`(200))
                .check(jsonPath("$.access_token").saveAs("accessToken"))
                .check(jsonPath("$.refresh_token").saveAs("refreshToken")),
        )
        .pause(Duration.ofSeconds(1))
        .exec(refreshTokenFlow)
        .pause(Duration.ofMillis(500))
        .exec(logoutFlow)

    // 시나리오 3: 동시 로그인 스파이크
    private val loginSpikeScenario = scenario("Login Spike Test")
        .exec(
            http("Spike Login")
                .post("/auth/login")
                .body(StringBody("""{"loginId":"benchmark@test.com","password":"Benchmark1234!"}"""))
                .check(status().`in`(200, 429)), // 429는 Rate Limiting
        )

    init {
        setUp(
            // 정상 로그인 부하: 100명이 1분에 걸쳐 로그인
            loginLoadScenario.injectOpen(
                rampUsers(100).during(Duration.ofSeconds(60)),
            ).protocols(httpProtocol),

            // 전체 인증 플로우: 20명이 30초에 걸쳐 진행
            fullAuthFlowScenario.injectOpen(
                rampUsers(20).during(Duration.ofSeconds(30)),
            ).protocols(httpProtocol),

            // 스파이크 테스트: 50명이 동시에 로그인 시도
            loginSpikeScenario.injectOpen(
                atOnceUsers(50),
            ).protocols(httpProtocol),
        ).assertions(
            global().responseTime().mean().lt(1000), // 평균 응답 시간 1초 미만
            global().responseTime().percentile(99.0).lt(3000), // P99 응답 시간 3초 미만
            global().successfulRequests().percent().gt(90.0), // 성공률 90% 이상
            details("Login").responseTime().mean().lt(500), // 로그인 평균 500ms 미만
        )
    }
}
