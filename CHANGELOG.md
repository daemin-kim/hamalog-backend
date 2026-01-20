# Changelog

Hamalog 프로젝트의 모든 주요 변경사항을 기록합니다.

이 형식은 [Keep a Changelog](https://keepachangelog.com/ko/1.0.0/)를 기반으로 하며,
[Semantic Versioning](https://semver.org/lang/ko/)을 따릅니다.

## [Unreleased]

### ♻️ 리팩토링 (Refactored)

- TODO를 NOTE로 변경 - 향후 개선사항 문서 참조

### ✅ 테스트 (Tests)

- JVMMetricsLogger 테스트 코드 추가
- JVMMetricsLogger 테스트 코드 추가
- RecentSideEffectResponse DTO 테스트 작성

### ✨ 추가됨 (Added)

- OAuth2 kakao login callback - RN app redirect implementation
- 회원 삭제 예약 스케줄러 및 마이그레이션 추가
- **benchmark**: 서버 시작 시 벤치마크 데이터 자동 생성 및 시나리오 수정
- Redis 캐시 성능 벤치마크 계획, 테스트 결과 정리
- ExportController, MedicationScheduleGroupController 구현 및 보안 설정 개선

### 🐛 수정됨 (Fixed)

- PowerShell에서 BOM 없는 UTF-8로 nginx 설정 저장
- 보안 소유권 검증 및 회원 탈퇴 예약
- H2 인덱스 이름 충돌 해결
- 복잡도 정당화 & 도메인 로직 취약점 강화
- 문서 통합 및 정리
- **security**: /api/v1/auth/* 경로를 SecurityConfig permitAll에 추가
- **auth**: 로그인 시 트랜잭션 read-only 오류 수정
- **benchmark**: JSON 필드명 스네이크 케이스로 통일
- **benchmark**: Gatling 실행 방식 및 JSON 필드명 수정
- **benchmark**: RateLimitingFilter에서 벤치마크 요청 Rate Limiting 우회
- **benchmark**: Gatling JVM에 시스템 프로퍼티 전달 설정 추가
- **benchmark**: Gatling 환경변수 전달 및 Rate Limiting 우회 개선

### 📚 문서화 (Documentation)

- README를 간략한 버전으로 업데이트
- Api 명세서 수정
- 문서 삭제
- Add comprehensive deployment status and resolution documentation
- Add final SSH timeout resolution report with complete analysis and outcomes
- 공개용 환경 변수 가이드 정리
- 컨벤션 문서 실무 기준 점검 및 일관성 수정

### 🔧 기타 (Chore)

- 메시지 인코딩 UTF-8 및 탈퇴 예약 안내 반영
- Spotless 포맷 수정
- 테스트 및 설정 동기화
- 오버엔지니어링 제거 및 설정 정리
- JVM 메트릭 로그 가독성 개선
- Vault 설정 잔여 제거
- JPA 성능 최적화 및 QueryDSL 적용
- 포맷팅 및 테스트 안정화
- 로컬 실행 기본 설정 보강
- Git-cliff로 CHANGELOG.md 자동 생성
- CI 재실행
- Spotless 적용
- 워크플로우에서 MySQL DELIMITER 지원을 위한 실행 방식 변경

---
> 이 CHANGELOG는 [git-cliff](https://github.com/orhun/git-cliff)로 자동 생성됩니다.
