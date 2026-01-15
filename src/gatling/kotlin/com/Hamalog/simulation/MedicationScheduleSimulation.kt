package com.Hamalog.simulation

import io.gatling.javaapi.core.CoreDsl.*
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.*
import java.time.Duration

/**
 * 복약 스케줄 조회 성능 벤치마크 시뮬레이션
 *
 * Before/After 비교:
 * - Naive: N+1 문제가 발생하는 조회 (optimized=false)
 * - Optimized: @EntityGraph로 최적화된 조회 (optimized=true)
 *
 * 실행 방법:
 * ./gradlew gatlingRun-com.Hamalog.simulation.MedicationScheduleSimulation
 */
class MedicationScheduleSimulation : Simulation() {

    // 환경 설정 (시스템 프로퍼티로 오버라이드 가능)
    private val baseUrl = System.getProperty("baseUrl", "http://localhost:8080")
    private val testUserLoginId = System.getProperty("testUser", "benchmark@test.com")
    private val testUserPassword = System.getProperty("testPassword", "Benchmark1234!")

    // 벤치마크 API 인증용 (프로덕션 환경에서 필요)
    private val benchmarkApiKey = System.getProperty("benchmarkApiKey", "")

    private val httpProtocol = http
        .baseUrl(baseUrl)
        .acceptHeader("application/json")
        .contentTypeHeader("application/json")
        .userAgentHeader("Gatling/3.11 - Performance Benchmark")
        // 벤치마크 API Key가 설정된 경우 헤더 추가
        .let { protocol ->
            if (benchmarkApiKey.isNotEmpty()) {
                protocol.header("X-Benchmark-API-Key", benchmarkApiKey)
            } else {
                protocol
            }
        }

    // ========================================
    // 1. 인증 토큰 획득
    // ========================================
    private val login = exec(
        http("Login - Token Acquisition")
            .post("/auth/login")
            .body(StringBody("""{"loginId":"$testUserLoginId","password":"$testUserPassword"}"""))
            .check(status().`is`(200))
            .check(jsonPath("$.accessToken").saveAs("accessToken"))
            .check(jsonPath("$.memberId").saveAs("memberId")),
    ).exitHereIfFailed()

    // ========================================
    // 2. Before: N+1 문제가 있는 비최적화 조회
    // ========================================
    private val fetchSchedulesNaive = exec(
        http("Fetch Schedules (Naive - N+1 Problem)")
            .get("/api/v1/benchmark/medication-schedules/list/#{memberId}?optimized=false")
            .header("Authorization", "Bearer #{accessToken}")
            .check(status().`is`(200))
            .check(jsonPath("$.schedules").exists()),
    )

    // ========================================
    // 3. After: 최적화된 조회 (@EntityGraph)
    // ========================================
    private val fetchSchedulesOptimized = exec(
        http("Fetch Schedules (Optimized - @EntityGraph)")
            .get("/api/v1/benchmark/medication-schedules/list/#{memberId}?optimized=true")
            .header("Authorization", "Bearer #{accessToken}")
            .check(status().`is`(200))
            .check(jsonPath("$.schedules").exists()),
    )

    // ========================================
    // 시나리오 정의
    // ========================================

    // 시나리오 1: N+1 문제 재현 (Before)
    private val naiveScenario = scenario("Before: N+1 Problem Scenario")
        .exec(login)
        .pause(Duration.ofMillis(500))
        .repeat(20).on(
            exec(fetchSchedulesNaive)
                .pause(Duration.ofMillis(100), Duration.ofMillis(300)),
        )

    // 시나리오 2: 최적화된 조회 (After)
    private val optimizedScenario = scenario("After: Optimized Scenario")
        .exec(login)
        .pause(Duration.ofMillis(500))
        .repeat(20).on(
            exec(fetchSchedulesOptimized)
                .pause(Duration.ofMillis(100), Duration.ofMillis(300)),
        )

    init {
        setUp(
            // Before 시나리오: 50명 사용자, 30초에 걸쳐 램프업
            naiveScenario.injectOpen(
                rampUsers(50).during(Duration.ofSeconds(30)),
            ).protocols(httpProtocol),

            // After 시나리오: 50명 사용자, 30초에 걸쳐 램프업
            optimizedScenario.injectOpen(
                rampUsers(50).during(Duration.ofSeconds(30)),
            ).protocols(httpProtocol),
        ).assertions(
            // 전역 성능 기준
            global().responseTime().mean().lt(500), // 평균 응답 시간 500ms 미만
            global().responseTime().percentile(95.0).lt(1000), // P95 응답 시간 1초 미만
            global().successfulRequests().percent().gt(95.0), // 성공률 95% 이상

            // 개별 시나리오 성능 기준
            details("Fetch Schedules (Optimized - @EntityGraph)").responseTime().mean().lt(300),
            details("Fetch Schedules (Naive - N+1 Problem)").responseTime().mean().lt(1000),
        )
    }
}
