# Hamalog 아키텍처 다이어그램 생성 프롬프트

> **대상 도구**: Nano Bananana Pro (AI 이미지 생성)  
> **목적**: Hamalog 프로젝트 전체 아키텍처를 하나의 다이어그램 이미지로 시각화  
> **스타일**: 아이콘 중심 다이어그램 + 하단 텍스트 라벨

---

## 🎨 이미지 생성 프롬프트 (한글)

```
헬스케어 백엔드 시스템 아키텍처 다이어그램을 생성해주세요.

전체 구성:
- 깔끔하고 전문적인 기술 다이어그램 스타일
- 밝은 파스텔 블루/그린 계열 배경 (신뢰감 있는 의료 시스템 느낌)
- 각 구성요소는 공식 로고 스타일 아이콘으로 표현
- 아이콘 하단에 간단한 텍스트 라벨 배치
- 구성요소 간 연결선은 얇은 회색 또는 파란색 점선/실선
- 가로 방향 레이아웃, 16:9 비율

계층 구조 (왼쪽에서 오른쪽으로):

[1단계: 사용자/클라이언트]
- 스마트폰 아이콘 (라벨: "Mobile App")
- 웹 브라우저 아이콘 (라벨: "Web Client")

[2단계: CDN/보안 계층]
- Cloudflare 주황색 로고 아이콘 (라벨: "Cloudflare")
  - 하위에 작은 아이콘들: 방패(DDoS), 자물쇠(SSL), 로봇차단(WAF)

[3단계: 리버스 프록시]
- Nginx 녹색 로고 아이콘 (라벨: "Nginx")
  - 하위에 작은 텍스트: Rate Limit, Bot Filter

[4단계: 애플리케이션 서버 (Docker 컨테이너 형태로 묶음)]
- 큰 Docker 고래 아이콘이 전체를 감싸는 형태
- 내부에:
  - Spring Boot 녹색 잎사귀 로고 (라벨: "Spring Boot 3.4.5")
  - Java 커피컵 로고 (라벨: "Java 21")
  - 보안 방패 아이콘 (라벨: "JWT + CSRF")
  - 캐시 번개 아이콘 (라벨: "Cache")

[5단계: 데이터 계층]
- Redis 빨간색 다이아몬드 로고 (라벨: "Redis 7")
  - 하위에 작은 텍스트: Cache, Session, Rate Limit
- MySQL 돌고래 로고 (라벨: "MySQL 8.0")
  - 하위에 작은 텍스트: Flyway Migration

[6단계: 외부 서비스 (오른쪽 상단)]
- Firebase 주황색 로고 (라벨: "FCM Push")
- Kakao 노란색 로고 (라벨: "OAuth2")
- Discord 보라색 로고 (라벨: "Alert Webhook")

[7단계: CI/CD (하단 별도 영역)]
- GitHub 고양이 로고 (라벨: "GitHub Actions")
- 화살표 → Docker 고래 아이콘 (라벨: "GHCR")
- 화살표 → 서버 아이콘 (라벨: "Self-hosted Runner")

색상 가이드:
- 배경: #F0F8FF (밝은 하늘색) 또는 흰색
- 아이콘 연결선: #4A90A4 (차분한 파란색)
- 텍스트 라벨: #333333 (진한 회색)
- 보안 관련 아이콘: 주황색/빨간색 계열
- 데이터 관련 아이콘: 빨간색(Redis), 파란색(MySQL)

스타일 참고:
- AWS 아키텍처 다이어그램과 유사한 깔끔한 스타일
- 각 아이콘은 둥근 사각형 또는 원형 배경 안에 배치
- 그림자 효과 최소화, 플랫 디자인
- 전체적으로 포트폴리오/기술 블로그에 적합한 전문적 느낌
```

---

## 🎨 이미지 생성 프롬프트 (영문)

```
Create a professional healthcare backend system architecture diagram.

Overall composition:
- Clean and professional technical diagram style
- Light pastel blue/green background representing trustworthy medical system
- Each component represented with official-style logo icons
- Simple text labels placed below each icon
- Thin gray or blue connection lines between components
- Horizontal layout, 16:9 aspect ratio

Layer structure (left to right):

[Layer 1: Client]
- Smartphone icon (label: "Mobile App")
- Web browser icon (label: "Web Client")

[Layer 2: CDN/Security]
- Cloudflare orange logo icon (label: "Cloudflare")
  - Small sub-icons: Shield(DDoS), Lock(SSL), Robot-block(WAF)

[Layer 3: Reverse Proxy]
- Nginx green logo icon (label: "Nginx")
  - Small text below: Rate Limit, Bot Filter

[Layer 4: Application Server (wrapped in Docker container shape)]
- Large Docker whale icon enclosing the section
- Inside:
  - Spring Boot green leaf logo (label: "Spring Boot 3.4.5")
  - Java coffee cup logo (label: "Java 21")
  - Security shield icon (label: "JWT + CSRF")
  - Lightning cache icon (label: "Cache")

[Layer 5: Data Layer]
- Redis red diamond logo (label: "Redis 7")
  - Small text: Cache, Session, Rate Limit
- MySQL dolphin logo (label: "MySQL 8.0")
  - Small text: Flyway Migration

[Layer 6: External Services (top right)]
- Firebase orange logo (label: "FCM Push")
- Kakao yellow logo (label: "OAuth2")
- Discord purple logo (label: "Alert Webhook")

[Layer 7: CI/CD (bottom separate section)]
- GitHub cat logo (label: "GitHub Actions")
- Arrow → Docker whale icon (label: "GHCR")
- Arrow → Server icon (label: "Self-hosted Runner")

Color guide:
- Background: #F0F8FF (light sky blue) or white
- Connection lines: #4A90A4 (calm blue)
- Text labels: #333333 (dark gray)
- Security icons: orange/red tones
- Data icons: red(Redis), blue(MySQL)

Style reference:
- Similar to AWS architecture diagram clean style
- Each icon placed in rounded rectangle or circle background
- Minimal shadow effects, flat design
- Professional look suitable for portfolio/tech blog
```

---

## 📐 다이어그램 구조 참조 (ASCII)

프롬프트 입력 시 이 구조를 참고하여 설명할 수 있습니다:

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                              HAMALOG ARCHITECTURE                                        │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                          │
│   ┌──────────┐     ┌──────────────┐     ┌─────────┐     ┌────────────────────────────┐  │
│   │ 📱       │     │ ☁️           │     │ 🟢      │     │  🐳 Docker                 │  │
│   │ Mobile   │────▶│ Cloudflare   │────▶│ Nginx   │────▶│  ┌────────┐ ┌────────┐    │  │
│   │ App      │     │              │     │         │     │  │🍃      │ │☕       │    │  │
│   └──────────┘     │ 🛡️DDoS       │     │ Rate    │     │  │Spring  │ │Java 21 │    │  │
│                    │ 🔒SSL        │     │ Limit   │     │  │Boot    │ │        │    │  │
│   ┌──────────┐     │ 🤖WAF        │     │ Bot     │     │  │3.4.5   │ └────────┘    │  │
│   │ 🌐       │     │              │     │ Filter  │     │  └────────┘               │  │
│   │ Web      │────▶│              │     │         │     │  ┌────────┐ ┌────────┐    │  │
│   │ Client   │     │              │     │         │     │  │🔐      │ │⚡       │    │  │
│   └──────────┘     └──────────────┘     └─────────┘     │  │JWT+    │ │Cache   │    │  │
│                                                          │  │CSRF    │ │        │    │  │
│                                                          │  └────────┘ └────────┘    │  │
│                                                          └────────────────────────────┘  │
│                                                                       │                  │
│                                                                       ▼                  │
│                          ┌─────────────────────────────────────────────────────────┐    │
│                          │              DATA LAYER                                  │    │
│                          │   ┌────────────┐              ┌────────────┐            │    │
│                          │   │ 🔴         │              │ 🐬         │            │    │
│                          │   │ Redis 7    │              │ MySQL 8.0  │            │    │
│                          │   │            │              │            │            │    │
│                          │   │ • Cache    │              │ • Flyway   │            │    │
│                          │   │ • Session  │              │ • JPA      │            │    │
│                          │   │ • RateLimit│              │            │            │    │
│                          │   └────────────┘              └────────────┘            │    │
│                          └─────────────────────────────────────────────────────────┘    │
│                                                                                          │
│  ┌─────────────────────────────────────────────────────────────────────────────────────┐│
│  │  EXTERNAL SERVICES                              CI/CD PIPELINE                       ││
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐        ┌──────────┐     ┌──────────┐        ││
│  │  │🔥        │ │💬        │ │🟣        │        │🐱        │────▶│🐳        │        ││
│  │  │Firebase  │ │Kakao     │ │Discord   │        │GitHub    │     │GHCR      │        ││
│  │  │FCM Push  │ │OAuth2    │ │Webhook   │        │Actions   │     │Registry  │        ││
│  │  └──────────┘ └──────────┘ └──────────┘        └──────────┘     └──────────┘        ││
│  └─────────────────────────────────────────────────────────────────────────────────────┘│
│                                                                                          │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 🏷️ 필수 포함 아이콘 및 라벨 목록

| 카테고리 | 아이콘 | 텍스트 라벨 | 색상 |
|----------|--------|-------------|------|
| **클라이언트** | 스마트폰 | Mobile App | 회색 |
| | 브라우저 | Web Client | 회색 |
| **보안/CDN** | Cloudflare 로고 | Cloudflare | 주황색 |
| | 방패 | DDoS Protection | 주황색 |
| | 자물쇠 | SSL/TLS | 주황색 |
| | 로봇 | WAF | 주황색 |
| **프록시** | Nginx 로고 | Nginx | 녹색 |
| **컨테이너** | Docker 고래 | Docker | 파란색 |
| **애플리케이션** | Spring Boot 잎사귀 | Spring Boot 3.4.5 | 녹색 |
| | Java 커피컵 | Java 21 | 빨간색 |
| | 방패+열쇠 | JWT + CSRF | 금색 |
| | 번개 | Cache | 노란색 |
| **데이터** | Redis 다이아몬드 | Redis 7 | 빨간색 |
| | MySQL 돌고래 | MySQL 8.0 | 파란색 |
| **외부 서비스** | Firebase 로고 | FCM Push | 주황색 |
| | Kakao 로고 | OAuth2 | 노란색 |
| | Discord 로고 | Alert Webhook | 보라색 |
| **CI/CD** | GitHub 고양이 | GitHub Actions | 검정색 |
| | 컨테이너 레지스트리 | GHCR | 파란색 |
| | 서버 | Self-hosted | 회색 |

---

## 💡 추가 팁

### Nano Bananana Pro 사용 시 권장사항

1. **프롬프트 분할**: 복잡한 아키텍처의 경우 여러 번 생성 후 합성 권장
2. **스타일 일관성**: 첫 생성 이미지의 스타일을 레퍼런스로 재사용
3. **텍스트 후처리**: AI 생성 텍스트가 불완전할 경우 Figma/Canva에서 수정
4. **해상도**: 최소 1920x1080 권장 (포트폴리오 용도)

### 생성 후 후처리 체크리스트

- [ ] 모든 아이콘 라벨 가독성 확인
- [ ] 연결선 방향 논리적 확인 (데이터 흐름)
- [ ] 색상 일관성 확인
- [ ] 포트폴리오 문서에 적용 테스트

---

## 📝 참고 문서

- [Project-Structure.md](../shared/Project-Structure.md) - 전체 프로젝트 구조
- [PORTFOLIO.md](../PORTFOLIO.md) - 메인 포트폴리오 문서
- [ADR 문서들](../internal/adr/) - 아키텍처 결정 기록

---

**문서 버전**: 1.0.0  
**작성일**: 2026-01-24  
**목적**: Nano Bananana Pro용 아키텍처 다이어그램 이미지 생성
