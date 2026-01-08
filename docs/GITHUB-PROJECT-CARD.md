# 🏥 Hamalog

> 헬스케어 복약 관리 백엔드 시스템

[![Java 21](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 3.4.5](https://img.shields.io/badge/Spring%20Boot-3.4.5-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![MySQL 8.0](https://img.shields.io/badge/MySQL-8.0-blue?logo=mysql)](https://www.mysql.com/)
[![Redis 7](https://img.shields.io/badge/Redis-7-red?logo=redis)](https://redis.io/)

복약 스케줄, 마음 일기, 부작용 추적을 통합 관리하는 Spring Boot REST API 서버입니다.  
**보안과 성능을 최우선으로 설계**하여 엔터프라이즈급 백엔드를 구현했습니다.

---

## ✨ 주요 기능

- 🔐 **JWT + CSRF 이중 보호** | Refresh Token Rotation, Rate Limiting
- 💊 **복약 관리** | 스케줄 CRUD, 이미지 업로드, 이행률 통계
- 📝 **마음 일기** | 템플릿/자유 형식, 기분 통계, 캘린더
- ⚠️ **부작용 추적** | 다중 기록, Redis 캐싱
- 🔔 **푸시 알림** | Redis Stream 메시지 큐, FCM, DLQ

---

## 🛠️ 기술 스택

| 영역 | 기술 |
|------|------|
| Backend | Java 21, Spring Boot 3.4.5, Spring Security, JPA |
| Database | MySQL 8.0, Redis 7, Flyway |
| Security | JWT, CSRF, AES-256-GCM 암호화 |
| DevOps | Docker, Cloudflare Tunnel, Nginx |
| Testing | JUnit 5, Mockito, ArchUnit (1,300+ 테스트) |

---

## 🎯 기술적 하이라이트

| 이슈 | 해결 |
|------|------|
| CSRF 공격 방어 | JWT + Redis CSRF 토큰 이중 보호 |
| 코드 중복 제거 | AOP `@RequireResourceOwnership` 어노테이션 |
| N+1 쿼리 문제 | @EntityGraph + DTO Projection (95% 쿼리 감소) |
| 민감 정보 보호 | AES-256-GCM 필드 단위 암호화 |
| 알림 응답 지연 | Redis Stream 비동기 메시지 큐 |

---

## 📊 프로젝트 규모

| 지표 | 수치 |
|------|------|
| API 엔드포인트 | **50+** |
| 테스트 케이스 | **1,300+** |
| 테스트 커버리지 | **80%+** |
| ADR 문서 | **7개** |

---

## 📚 문서

- [📄 상세 포트폴리오](./PORTFOLIO.md)
- [📋 API 명세서](./shared/API-specification.md)
- [🏛️ ADR (아키텍처 결정 기록)](./internal/adr/)
- [📐 패턴 가이드](./internal/patterns/)

---

> **개발 기간**: 2025.08 ~ 현재 (백엔드 전담 개발)

