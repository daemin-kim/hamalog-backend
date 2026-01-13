# Changelog

Hamalog 프로젝트의 모든 주요 변경사항을 기록합니다.

이 형식은 [Keep a Changelog](https://keepachangelog.com/ko/1.0.0/)를 기반으로 하며,
[Semantic Versioning](https://semver.org/lang/ko/)을 따릅니다.

## [Unreleased]

### ✅ 테스트 (Tests)

- JVMMetricsLogger 테스트 코드 추가
- JVMMetricsLogger 테스트 코드 추가
- RecentSideEffectResponse DTO 테스트 작성

### ✨ 추가됨 (Added)

- OAuth2 kakao login callback - RN app redirect implementation
- 회원 삭제 예약 스케줄러 및 마이그레이션 추가

### 🐛 수정됨 (Fixed)

- PowerShell에서 BOM 없는 UTF-8로 nginx 설정 저장
- 보안 소유권 검증 및 회원 탈퇴 예약
- H2 인덱스 이름 충돌 해결
- 복잡도 정당화 & 도메인 로직 취약점 강화
- 문서 통합 및 정리

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

---
> 이 CHANGELOG는 [git-cliff](https://github.com/orhun/git-cliff)로 자동 생성됩니다.
