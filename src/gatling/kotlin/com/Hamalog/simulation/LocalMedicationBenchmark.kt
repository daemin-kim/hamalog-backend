package com.Hamalog.simulation

import io.gatling.javaapi.core.CoreDsl.*
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.*
import java.time.Duration

/**
 * 로컬 환경 N+1 vs Optimized 성능 벤치마크
 *
 * 목적: JPA @EntityGraph 최적화 효과 측정
 *
 * 시나리오:
 *   1. Before: N+1 문제가 있는 조회 (optimized=false)
 *   2. After: @EntityGraph로 최적화된 조회 (optimized=true)
 *
 * 실행 방법:
 *   ./scripts/benchmark/run-local-benchmark.sh
 *
 * 또는 직접 실행:
 *   docker-compose -f docker-compose-benchmark.yml up -d
 *   ./gradlew gatlingRun -Dgatling.simulationClass=com.Hamalog.simulation.LocalMedicationBenchmark
 */
class LocalMedicationBenchmark : Simulation() {

    // ============================================
    // 설정
    // ============================================
    private val baseUrl = System.getProperty("baseUrl") ?: "http://localhost:8080"
    private val memberId = 1 // 벤치마크 사용자 ID

    // 부하 설정
    private val usersPerSecond = 10.0 // 초당 사용자 수
    private val durationSeconds = 30L // 테스트 지속 시간

    init {
        println("╔════════════════════════════════════════════════════════════╗")
        println("║     Hamalog N+1 vs Optimized Performance Benchmark         ║")
        println("╠════════════════════════════════════════════════════════════╣")
        println("║  Base URL: $baseUrl")
        println("║  Users/sec: $usersPerSecond")
        println("║  Duration: ${durationSeconds}s")
        println("║  Member ID: $memberId")
        println("╚════════════════════════════════════════════════════════════╝")
    }

    // ============================================
    // HTTP 프로토콜
    // ============================================
    private val httpProtocol = http
        .baseUrl(baseUrl)
        .acceptHeader("application/json")
        .contentTypeHeader("application/json")
        .userAgentHeader("Gatling/3.11 - Local Benchmark")

    // ============================================
    // Before: N+1 문제 쿼리
    // ============================================
    private val naiveQuery = exec(
        http("Before: N+1 Query")
            .get("/api/v1/benchmark/medication-schedules/list/$memberId?optimized=false")
            .check(status().`is`(200))
            .check(jsonPath("$.schedules").exists())
            .check(jsonPath("$.schedules[*]").count().gte(1)),
    )

    // ============================================
    // After: 최적화된 쿼리 (@EntityGraph)
    // ============================================
    private val optimizedQuery = exec(
        http("After: Optimized Query (@EntityGraph)")
            .get("/api/v1/benchmark/medication-schedules/list/$memberId?optimized=true")
            .check(status().`is`(200))
            .check(jsonPath("$.schedules").exists())
            .check(jsonPath("$.schedules[*]").count().gte(1)),
    )

    // ============================================
    // 시나리오 정의
    // ============================================

    // Before 시나리오: N+1 문제 재현
    private val beforeScenario = scenario("Before: N+1 Problem")
        .exec(naiveQuery)
        .pause(Duration.ofMillis(50), Duration.ofMillis(100))

    // After 시나리오: 최적화된 조회
    private val afterScenario = scenario("After: Optimized (@EntityGraph)")
        .exec(optimizedQuery)
        .pause(Duration.ofMillis(50), Duration.ofMillis(100))

    // ============================================
    // 테스트 실행 설정
    // ============================================
    init {
        setUp(
            // Before 시나리오 먼저 실행
            beforeScenario.injectOpen(
                constantUsersPerSec(usersPerSecond).during(Duration.ofSeconds(durationSeconds)),
            ).protocols(httpProtocol),

            // After 시나리오
            afterScenario.injectOpen(
                constantUsersPerSec(usersPerSecond).during(Duration.ofSeconds(durationSeconds)),
            ).protocols(httpProtocol),
        ).assertions(
            // 전체 성공률 95% 이상
            global().successfulRequests().percent().gt(95.0),

            // Before (N+1): 평균 1초 미만 (N+1이라 느림)
            details("Before: N+1 Query").responseTime().mean().lt(1000),

            // After (Optimized): 평균 200ms 미만 (최적화로 빠름)
            details("After: Optimized Query (@EntityGraph)").responseTime().mean().lt(200),
        )
    }
}
