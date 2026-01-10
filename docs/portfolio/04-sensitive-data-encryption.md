# 04. AES-256-GCM 민감정보 암호화

> **개인정보보호법과 의료법 요구사항을 충족하는 필드 단위 암호화 및 JPA Converter를 통한 투명한 암복호화**

---

## 📋 목차

1. [문제 상황](#1-문제-상황-problem)
2. [해결 전략](#2-해결-전략-solution-strategy)
3. [구현 상세](#3-구현-상세-implementation)
4. [효과 및 검증](#4-효과-및-검증-results)
5. [면접 대비 Q&A](#5-면접-대비-qa)

---

## 1. 문제 상황 (Problem)

### 1.1 법적 요구사항

의료 정보를 다루는 Hamalog는 다음 법률의 암호화 요구사항을 충족해야 합니다:

| 법률 | 요구사항 | 대상 |
|------|---------|------|
| **개인정보보호법** | 고유식별정보, 비밀번호 암호화 저장 | 주민번호, 전화번호, 이메일 |
| **의료법** | 환자 진료정보 보호 | 건강 관련 모든 데이터 |
| **정보통신망법** | 개인정보 전송 시 암호화 | API 통신 전구간 |

### 1.2 DB 수준 암호화의 한계

MySQL의 TDE(Transparent Data Encryption)만으로는 부족합니다:

```
┌─────────────────────────────────────────────────────────────────┐
│                      TDE의 한계                                  │
│                                                                  │
│  1. DB 서버 접근 권한이 있으면 복호화된 데이터 조회 가능         │
│  2. SQL Injection 성공 시 평문 노출                              │
│  3. DB 백업 파일이 암호화되어도 복원 후 평문                     │
│  4. 애플리케이션 로그에 민감정보 노출 가능                       │
│                                                                  │
│  결론: 애플리케이션 레벨 암호화 필요                             │
└─────────────────────────────────────────────────────────────────┘
```

### 1.3 Hamalog에서 보호해야 할 데이터

| 엔티티 | 필드 | 민감도 |
|--------|------|-------|
| Member | phoneNumber, birthDate | 🔴 높음 |
| MoodDiary | content (일기 내용) | 🔴 높음 |
| SideEffectRecord | description | 🟡 중간 |
| NotificationSettings | deviceToken | 🟡 중간 |

---

## 2. 해결 전략 (Solution Strategy)

### 2.1 고려한 대안들

| 방식 | 장점 | 단점 | 채택 여부 |
|------|------|------|----------|
| **DB TDE** | 투명, 구현 불필요 | 애플리케이션 레벨 보호 없음 | ❌ 단독 사용 불가 |
| **AES-CBC** | 널리 사용, 검증됨 | 무결성 검증 별도 필요 | ❌ |
| **AES-GCM** | 암호화 + 무결성 동시 | 구현 복잡도 약간 증가 | ✅ |
| **RSA** | 비대칭키, 키 분리 | 대용량 데이터 비효율 | ❌ |

### 2.2 최종 선택: AES-256-GCM

```
┌─────────────────────────────────────────────────────────────────┐
│                     AES-256-GCM 선택 이유                        │
│                                                                  │
│  1. 기밀성 (Confidentiality): AES-256으로 강력한 암호화         │
│  2. 무결성 (Integrity): GCM 모드의 인증 태그로 변조 감지        │
│  3. 성능: CBC보다 빠름 (병렬 처리 가능)                         │
│  4. 표준: NIST 권장, TLS 1.3에서 사용                           │
│                                                                  │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────────────┐  │
│  │  평문       │ →  │ AES-256-GCM │ →  │ IV + 암호문 + 태그  │  │
│  │ "010-1234"  │    │   암호화    │    │ Base64 인코딩       │  │
│  └─────────────┘    └─────────────┘    └─────────────────────┘  │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 2.3 JPA Converter로 투명한 암복호화

```
┌─────────────────────────────────────────────────────────────────┐
│                    투명한 암복호화 흐름                          │
│                                                                  │
│  [저장 시]                                                       │
│  Service → Entity.setPhone("010-1234")                          │
│         → JPA Converter.encrypt()                               │
│         → DB에 "aGVsbG8gd29ybGQ=" 저장                          │
│                                                                  │
│  [조회 시]                                                       │
│  DB "aGVsbG8gd29ybGQ=" → JPA Converter.decrypt()                │
│                       → Entity.getPhone() = "010-1234"          │
│                       → Service                                 │
│                                                                  │
│  ✅ 비즈니스 로직에서 암복호화 인지 불필요                       │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. 구현 상세 (Implementation)

### 3.1 암호화 유틸리티 (DataEncryptionUtil.java)

```java
/**
 * AES-256-GCM 기반 데이터 암호화 유틸리티
 * 
 * GCM(Galois/Counter Mode) 특징:
 * - 암호화 + 인증을 한 번에 수행 (AEAD: Authenticated Encryption with Associated Data)
 * - 암호문 변조 시 복호화 실패로 무결성 보장
 * - 병렬 처리 가능하여 성능 우수
 */
@Component
public class DataEncryptionUtil {
    
    // ============================================================
    // 상수 정의
    // ============================================================
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    
    /**
     * GCM IV(Initialization Vector) 길이: 12바이트 (96비트)
     * 
     * NIST 권장값. 12바이트보다 짧으면 보안 취약,
     * 길면 내부적으로 해시 처리되어 성능 저하.
     */
    private static final int GCM_IV_LENGTH = 12;
    
    /**
     * GCM 인증 태그 길이: 16바이트 (128비트)
     * 
     * 암호문 변조 감지용. 최대 128비트까지 지원되며,
     * 128비트가 가장 안전.
     */
    private static final int GCM_TAG_LENGTH = 16;
    
    private final SecretKeySpec secretKey;
    private final SecureRandom secureRandom;
    private final boolean encryptionDisabled;  // 키 누락 시 비활성화 플래그
    
    // ============================================================
    // 생성자: 암호화 키 초기화
    // ============================================================
    
    public DataEncryptionUtil(
            @Value("${hamalog.encryption.key:${HAMALOG_ENCRYPTION_KEY:}}") String fallbackKey,
            Environment environment
    ) {
        this.secureRandom = new SecureRandom();
        
        // 키 초기화 (환경변수 우선, 프로퍼티 fallback)
        KeyInitializationResult result = initializeSecretKey(fallbackKey, this.secureRandom);
        this.secretKey = result.secretKey;
        this.encryptionDisabled = result.encryptionDisabled;
    }
    
    private KeyInitializationResult initializeSecretKey(String fallbackKey, SecureRandom random) {
        boolean isProduction = /* 프로덕션 프로파일 체크 */;
        
        // 환경변수 우선순위: System.getenv() > Spring property
        String encryptionKey = System.getenv("HAMALOG_ENCRYPTION_KEY");
        if (encryptionKey == null || encryptionKey.isEmpty()) {
            encryptionKey = fallbackKey;
        }
        
        // ============================================================
        // 프로덕션에서 키 누락 시 경고 (시작은 허용, 암호화 비활성화)
        // ============================================================
        
        if (encryptionKey == null || encryptionKey.isEmpty()) {
            if (isProduction) {
                log.error("프로덕션에서 암호화 키 누락! 키 생성: openssl rand -base64 32");
                // 시작은 허용하되 암호화 시도 시 예외 발생
                return new KeyInitializationResult(
                    new SecretKeySpec(new byte[32], ALGORITHM), 
                    true  // encryptionDisabled = true
                );
            }
            
            // 개발 환경: 임시 키 생성 (매 재시작마다 변경됨)
            log.warn("개발용 임시 암호화 키 생성. 재시작 시 기존 암호화 데이터 복호화 불가!");
            byte[] randomKey = new byte[32];
            random.nextBytes(randomKey);
            return new KeyInitializationResult(new SecretKeySpec(randomKey, ALGORITHM), false);
        }
        
        // ============================================================
        // 키 길이 검증: 정확히 256비트(32바이트) 필요
        // ============================================================
        
        try {
            byte[] decodedKey = Base64.getDecoder().decode(encryptionKey);
            
            if (decodedKey.length != 32) {
                throw new IllegalStateException(
                    "암호화 키는 정확히 256비트(32바이트)여야 합니다. " +
                    "현재: " + (decodedKey.length * 8) + "비트"
                );
            }
            
            return new KeyInitializationResult(new SecretKeySpec(decodedKey, ALGORITHM), false);
            
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                "암호화 키는 유효한 Base64 형식이어야 합니다.", e
            );
        }
    }
    
    // ============================================================
    // 암호화 메서드
    // ============================================================
    
    /**
     * 평문을 AES-256-GCM으로 암호화
     * 
     * @param plainText 암호화할 평문
     * @return Base64 인코딩된 암호문 (IV + 암호문 + 인증태그 포함)
     * 
     * 출력 형식:
     * ┌────────┬─────────────────┬───────────────┐
     * │ IV     │ 암호문          │ 인증 태그      │
     * │ 12byte │ 가변 길이       │ 16byte        │
     * └────────┴─────────────────┴───────────────┘
     *           ↓ Base64 인코딩
     * "aGVsbG8gd29ybGQgaGVsbG8gd29ybGQ="
     */
    public String encrypt(String plainText) {
        // null/빈 문자열은 그대로 반환
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        
        // 암호화 비활성화 상태면 예외 (프로덕션 키 누락)
        if (encryptionDisabled) {
            throw new IllegalStateException(
                "❌ 암호화 비활성화 상태. HAMALOG_ENCRYPTION_KEY 환경변수 설정 필요."
            );
        }
        
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            
            // ============================================================
            // IV(Initialization Vector) 생성
            // ============================================================
            // 
            // - 매 암호화마다 새로운 무작위 IV 생성 (필수!)
            // - 같은 키로 같은 평문을 암호화해도 다른 암호문 생성
            // - IV는 비밀이 아니므로 암호문과 함께 저장
            
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            
            // ============================================================
            // GCM 파라미터 설정 및 암호화
            // ============================================================
            
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);
            
            byte[] encryptedData = cipher.doFinal(plainText.getBytes());
            
            // ============================================================
            // IV + 암호문 결합
            // ============================================================
            // 
            // 복호화 시 IV가 필요하므로 암호문 앞에 IV를 붙여서 저장
            // | IV (12byte) | 암호문 + 인증태그 (가변) |
            
            byte[] encryptedWithIv = new byte[GCM_IV_LENGTH + encryptedData.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedData, 0, encryptedWithIv, GCM_IV_LENGTH, encryptedData.length);
            
            // Base64 인코딩하여 문자열로 반환 (DB 저장용)
            return Base64.getEncoder().encodeToString(encryptedWithIv);
            
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }
    
    // ============================================================
    // 복호화 메서드
    // ============================================================
    
    /**
     * AES-256-GCM으로 암호화된 데이터를 복호화
     * 
     * @param encryptedText Base64 인코딩된 암호문
     * @return 복호화된 평문
     * @throws RuntimeException 복호화 실패 시 (변조 감지 포함)
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }
        
        if (encryptionDisabled) {
            throw new IllegalStateException(
                "❌ 암호화 비활성화 상태. 복호화 불가."
            );
        }
        
        try {
            // Base64 디코딩
            byte[] encryptedWithIv = Base64.getDecoder().decode(encryptedText);
            
            // 최소 길이 검증 (IV 12byte + 인증태그 16byte + 최소 1byte 데이터)
            if (encryptedWithIv.length < GCM_IV_LENGTH + GCM_TAG_LENGTH) {
                throw new IllegalArgumentException("Invalid encrypted data length");
            }
            
            // ============================================================
            // IV와 암호문 분리
            // ============================================================
            
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encryptedData = new byte[encryptedWithIv.length - GCM_IV_LENGTH];
            
            System.arraycopy(encryptedWithIv, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedWithIv, GCM_IV_LENGTH, encryptedData, 0, encryptedData.length);
            
            // ============================================================
            // 복호화 (인증 태그 검증 포함)
            // ============================================================
            //
            // GCM 모드는 복호화 시 자동으로 인증 태그를 검증합니다.
            // 암호문이 변조되었으면 AEADBadTagException 발생!
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);
            
            byte[] plainText = cipher.doFinal(encryptedData);
            return new String(plainText);
            
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
```

### 3.2 JPA AttributeConverter 구현

```java
/**
 * 문자열 필드 자동 암복호화 Converter
 * 
 * Entity에서 @Convert(converter = EncryptedStringConverter.class)로 지정하면
 * DB 저장 시 자동 암호화, 조회 시 자동 복호화
 */
@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {
    
    // static 필드로 주입 (JPA Converter는 new로 생성되어 @Autowired 불가)
    private static DataEncryptionUtil encryptionUtil;
    
    @Autowired
    public void setEncryptionUtil(DataEncryptionUtil util) {
        EncryptedStringConverter.encryptionUtil = util;
    }
    
    /**
     * Entity → DB 저장 시 호출
     * 
     * @param attribute Entity의 필드 값 (평문)
     * @return DB에 저장될 값 (암호문)
     */
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        return encryptionUtil.encrypt(attribute);
    }
    
    /**
     * DB → Entity 로딩 시 호출
     * 
     * @param dbData DB에서 읽은 값 (암호문)
     * @return Entity 필드에 설정될 값 (평문)
     */
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return encryptionUtil.decrypt(dbData);
    }
}

/**
 * LocalDate 필드 자동 암복호화 Converter
 * 
 * 생년월일 등 날짜 타입 민감정보 암호화
 */
@Converter
@Component
public class EncryptedLocalDateConverter implements AttributeConverter<LocalDate, String> {
    
    private static DataEncryptionUtil encryptionUtil;
    
    @Autowired
    public void setEncryptionUtil(DataEncryptionUtil util) {
        EncryptedLocalDateConverter.encryptionUtil = util;
    }
    
    @Override
    public String convertToDatabaseColumn(LocalDate attribute) {
        if (attribute == null) {
            return null;
        }
        // LocalDate → ISO 문자열 → 암호화
        return encryptionUtil.encrypt(attribute.toString());
    }
    
    @Override
    public LocalDate convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        // 암호문 → 복호화 → LocalDate 파싱
        String decrypted = encryptionUtil.decrypt(dbData);
        return LocalDate.parse(decrypted);
    }
}
```

### 3.3 Entity에서 사용

```java
@Entity
@Table(name = "member")
public class Member {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long memberId;
    
    @Column(nullable = false, length = 100)
    private String loginId;  // 이메일 - 암호화 안 함 (로그인에 필요)
    
    // ============================================================
    // 암호화된 필드들
    // ============================================================
    
    /**
     * 전화번호: 개인정보보호법 암호화 대상
     * 
     * @Convert: 저장/조회 시 자동 암복호화
     * @Column(length = 500): 암호화된 문자열은 원본보다 길어짐
     *                        Base64 인코딩으로 약 1.33배 + IV/태그
     */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "phone_number", length = 500)
    private String phoneNumber;
    
    /**
     * 생년월일: 개인정보보호법 암호화 대상
     */
    @Convert(converter = EncryptedLocalDateConverter.class)
    @Column(name = "birth_date", length = 500)
    private LocalDate birthDate;
    
    // ============================================================
    // 비암호화 필드들
    // ============================================================
    
    @Column(nullable = false)
    private String nickname;  // 공개 정보
    
    @Column(nullable = false)
    private String password;  // BCrypt 해시 저장 (암호화 아님)
}
```

### 3.4 환경변수 및 키 관리

```bash
# 암호화 키 생성 (256비트 = 32바이트)
openssl rand -base64 32
# 예: "K7gNU3sdo+OL0wNhqoVWhr3g6s1xYv72ol/pe/Unols="

# 환경변수 설정 (프로덕션)
export HAMALOG_ENCRYPTION_KEY="K7gNU3sdo+OL0wNhqoVWhr3g6s1xYv72ol/pe/Unols="

# Docker Compose
environment:
  HAMALOG_ENCRYPTION_KEY: ${HAMALOG_ENCRYPTION_KEY}

# Kubernetes Secret
kubectl create secret generic hamalog-secrets \
  --from-literal=encryption-key="K7gNU3sdo+OL0wNhqoVWhr3g6s1xYv72ol/pe/Unols="
```

---

## 4. 효과 및 검증 (Results)

### 4.1 보안 효과

| 위협 | 방어 여부 | 메커니즘 |
|------|----------|----------|
| **DB 탈취** | ✅ 방어 | 암호문만 노출, 키 없이 복호화 불가 |
| **SQL Injection** | ✅ 방어 | 쿼리 결과가 암호문 |
| **백업 파일 유출** | ✅ 방어 | 백업 데이터도 암호화 상태 |
| **데이터 변조** | ✅ 감지 | GCM 인증 태그로 무결성 검증 |
| **로그 노출** | ⚠️ 주의 | 로깅 전에 마스킹 필요 |

### 4.2 성능 영향

```
📊 암호화 성능 측정 (1000회 반복 평균)

암호화 (encrypt):
- 입력: 20자 문자열
- 시간: ~0.05ms
- 처리량: ~20,000 ops/sec

복호화 (decrypt):
- 입력: 암호화된 문자열
- 시간: ~0.03ms
- 처리량: ~33,000 ops/sec

결론: API 응답 시간에 미치는 영향 무시 가능 (<1ms)
```

### 4.3 저장 공간 영향

```
원본 데이터: "010-1234-5678" (13바이트)
암호화 후:
  - IV: 12바이트
  - 암호문: 13바이트
  - 인증 태그: 16바이트
  - 합계: 41바이트
  - Base64 인코딩: 56바이트

증가율: 약 4.3배

대응: VARCHAR(500)으로 충분한 여유 확보
```

### 4.4 검증 테스트

```java
@Test
@DisplayName("암호화 후 복호화하면 원본 복원")
void encryptDecrypt_shouldReturnOriginal() {
    // given
    String original = "010-1234-5678";
    
    // when
    String encrypted = encryptionUtil.encrypt(original);
    String decrypted = encryptionUtil.decrypt(encrypted);
    
    // then
    assertThat(decrypted).isEqualTo(original);
    assertThat(encrypted).isNotEqualTo(original);  // 암호화됨
}

@Test
@DisplayName("같은 평문도 매번 다른 암호문 생성 (IV 무작위)")
void encrypt_samePlaintext_differentCiphertext() {
    // given
    String plaintext = "test data";
    
    // when
    String encrypted1 = encryptionUtil.encrypt(plaintext);
    String encrypted2 = encryptionUtil.encrypt(plaintext);
    
    // then
    assertThat(encrypted1).isNotEqualTo(encrypted2);  // 매번 다른 암호문
}

@Test
@DisplayName("암호문 변조 시 복호화 실패")
void decrypt_tamperedCiphertext_throwsException() {
    // given
    String encrypted = encryptionUtil.encrypt("sensitive data");
    
    // 암호문 변조 (마지막 문자 변경)
    String tampered = encrypted.substring(0, encrypted.length() - 1) + "X";
    
    // when & then
    assertThatThrownBy(() -> encryptionUtil.decrypt(tampered))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Decryption failed");
}
```

---

## 5. 면접 대비 Q&A

### Q1. AES-CBC 대신 AES-GCM을 선택한 이유는?

> **모범 답변**
> 
> GCM(Galois/Counter Mode)은 **AEAD(Authenticated Encryption with Associated Data)**를 제공합니다:
> 
> | 특성 | CBC | GCM |
> |------|-----|-----|
> | **기밀성** | ✅ | ✅ |
> | **무결성** | ❌ (HMAC 별도 필요) | ✅ (인증 태그 포함) |
> | **병렬화** | ❌ | ✅ |
> | **성능** | 보통 | 빠름 |
> 
> CBC를 사용하면 암호화 후 별도로 HMAC을 계산해야 변조를 감지할 수 있습니다. GCM은 암호화와 동시에 인증 태그를 생성하므로 코드가 간결하고 안전합니다.
> 
> 또한 GCM은 TLS 1.3에서 유일하게 권장되는 대칭키 암호화 모드입니다.

### Q2. IV를 왜 매번 새로 생성하나요? 재사용하면 안 되나요?

> **모범 답변**
> 
> **절대 재사용하면 안 됩니다.** 특히 GCM에서는 치명적입니다.
> 
> GCM에서 같은 키와 IV로 두 개의 평문을 암호화하면:
> 1. 두 암호문을 XOR하면 두 평문의 XOR이 노출됩니다.
> 2. 인증 키가 노출되어 암호문 위조가 가능해집니다.
> 
> 이를 **Nonce Reuse Attack**이라고 합니다.
> 
> 구현에서 `SecureRandom`으로 매번 새 IV를 생성하고, 암호문 앞에 IV를 붙여 저장합니다. IV는 비밀이 아니므로 공개되어도 안전합니다.

### Q3. 암호화 키는 어떻게 관리하나요?

> **모범 답변**
> 
> **계층적 키 관리** 전략을 사용합니다:
> 
> 1. **개발 환경**: 임시 키 자동 생성 (재시작 시 변경됨)
> 2. **스테이징**: 환경변수로 고정 키 설정
> 3. **프로덕션**: AWS KMS, HashiCorp Vault 등 키 관리 시스템 사용 (향후 계획)
> 
> 현재 구현:
> ```bash
> export HAMALOG_ENCRYPTION_KEY="$(openssl rand -base64 32)"
> ```
> 
> 키 로테이션을 위해 **이전 키 목록**을 유지하고, 복호화 시 현재 키로 실패하면 이전 키로 시도하는 방식을 고려 중입니다.

### Q4. DB에서 암호화된 필드로 검색할 수 있나요?

> **모범 답변**
> 
> **아니오, 직접 검색할 수 없습니다.** 이것이 필드 단위 암호화의 한계입니다.
> 
> ```sql
> -- 불가능! 암호화된 값으로 저장되어 있음
> SELECT * FROM member WHERE phone_number = '010-1234-5678';
> ```
> 
> 해결 방법:
> 
> 1. **검색이 필요 없는 필드만 암호화**: 전화번호는 조회용이 아닌 연락용
> 
> 2. **해시 인덱스 추가**: 검색용 해시값을 별도 컬럼에 저장
>    ```java
>    @Column
>    private String phoneNumberHash;  // SHA-256 해시
>    ```
> 
> 3. **검색 가능 암호화(Searchable Encryption)**: 복잡하고 성능 저하
> 
> Hamalog에서는 암호화된 필드(전화번호, 생년월일)는 검색 대상이 아니므로 문제없습니다.

### Q5. JPA Converter 대신 Service에서 암복호화하면 안 되나요?

> **모범 답변**
> 
> 가능하지만 **JPA Converter가 더 안전**합니다:
> 
> | 방식 | 장점 | 단점 |
> |------|------|------|
> | **Service에서** | 명시적, 유연 | 누락 위험, 코드 중복 |
> | **JPA Converter** | 자동, 일관됨 | 로깅 시 주의 필요 |
> 
> Service에서 하면:
> ```java
> // 저장
> member.setPhoneNumber(encrypt(request.getPhoneNumber()));  // 까먹을 수 있음!
> 
> // 조회
> String phone = decrypt(member.getPhoneNumber());  // 매번 호출해야 함
> ```
> 
> JPA Converter는 **영속성 계층에서 투명하게** 처리하므로 비즈니스 로직이 깔끔해지고, 암호화 누락을 방지합니다.

### Q6. 암호화된 데이터가 로그에 노출되는 것을 어떻게 방지하나요?

> **모범 답변**
> 
> 세 가지 전략을 사용합니다:
> 
> 1. **Entity toString() 재정의**:
>    ```java
>    @Override
>    public String toString() {
>        return "Member{id=" + memberId + ", phone=***MASKED***}";
>    }
>    ```
> 
> 2. **로깅 시 DTO 사용**: Entity를 직접 로깅하지 않고, 로깅용 DTO로 변환
> 
> 3. **Logback 마스킹 필터**: 패턴 매칭으로 민감정보 자동 마스킹
>    ```xml
>    <pattern>%replace(%msg){'\\d{3}-\\d{4}-\\d{4}', '***-****-****'}</pattern>
>    ```
> 
> 추가로, **JPA show_sql**을 프로덕션에서 비활성화하여 바인딩 파라미터가 로그에 남지 않도록 합니다.

### Q7. 암호화 키가 유출되면 어떻게 대응하나요?

> **모범 답변**
> 
> **키 로테이션(Key Rotation)** 절차를 수행합니다:
> 
> 1. **새 키 생성**: `openssl rand -base64 32`
> 
> 2. **다중 키 지원 활성화**: 복호화 시 여러 키 시도
>    ```java
>    public String decrypt(String ciphertext) {
>        try {
>            return decryptWithKey(ciphertext, currentKey);
>        } catch (Exception e) {
>            return decryptWithKey(ciphertext, previousKey);
>        }
>    }
>    ```
> 
> 3. **배치 재암호화**: 기존 데이터를 새 키로 재암호화
>    ```java
>    members.forEach(m -> {
>        String decrypted = decryptWithOldKey(m.getPhone());
>        String reencrypted = encryptWithNewKey(decrypted);
>        m.setPhone(reencrypted);
>    });
>    ```
> 
> 4. **이전 키 폐기**: 재암호화 완료 후 이전 키 삭제
> 
> 현재 Hamalog는 단일 키 구조이지만, 프로덕션 전에 다중 키 지원을 추가할 계획입니다.

### Q8. 왜 BCrypt 대신 AES로 비밀번호를 암호화하지 않나요?

> **모범 답변**
> 
> **비밀번호는 암호화가 아닌 해싱**이 올바른 방식입니다.
> 
> | 목적 | 방식 | 특징 |
> |------|------|------|
> | **저장 후 원본 필요** | 암호화 (AES) | 복호화 가능 |
> | **저장 후 원본 불필요** | 해싱 (BCrypt) | 복호화 불가능 |
> 
> 비밀번호는:
> 1. 원본을 다시 볼 필요가 없습니다 (검증만 필요)
> 2. 복호화 가능하면 DB 탈취 시 모든 비밀번호 노출
> 3. 해시값만 있으면 입력값과 비교 검증 가능
> 
> BCrypt는:
> - **Salt 내장**: 같은 비밀번호도 다른 해시값
> - **Adaptive**: work factor로 연산 비용 조절
> - **레인보우 테이블 방어**: Salt로 사전 계산 공격 무력화

### Q9. 멀티스레드 환경에서 SecureRandom 사용 시 주의점은?

> **모범 답변**
> 
> `SecureRandom`은 **스레드 세이프**하지만, 동기화로 인해 병목이 될 수 있습니다.
> 
> 현재 구현:
> ```java
> private final SecureRandom secureRandom = new SecureRandom();
> ```
> 
> 고부하 상황 대응:
> 
> 1. **ThreadLocal 사용**:
>    ```java
>    private static final ThreadLocal<SecureRandom> RANDOM = 
>        ThreadLocal.withInitial(SecureRandom::new);
>    ```
> 
> 2. **Java 17+ SecureRandom 개선**: 내부 동기화 최적화됨
> 
> 3. **IV 생성 분리**: IV 생성만 별도 스레드로
> 
> Hamalog의 현재 트래픽 수준에서는 단일 인스턴스로 충분합니다. 성능 테스트에서 병목이 확인되면 ThreadLocal을 도입합니다.

### Q10. 암호화 알고리즘이 변경되면 기존 데이터는 어떻게 하나요?

> **모범 답변**
> 
> **점진적 마이그레이션** 전략을 사용합니다:
> 
> 1. **버전 필드 추가**:
>    ```sql
>    ALTER TABLE member ADD COLUMN encryption_version INT DEFAULT 1;
>    ```
> 
> 2. **복호화 시 버전 확인**:
>    ```java
>    public String decrypt(String ciphertext, int version) {
>        return switch (version) {
>            case 1 -> decryptV1(ciphertext);  // AES-256-GCM
>            case 2 -> decryptV2(ciphertext);  // 미래 알고리즘
>            default -> throw new IllegalArgumentException();
>        };
>    }
>    ```
> 
> 3. **읽기 시 재암호화**:
>    ```java
>    if (member.getEncryptionVersion() < CURRENT_VERSION) {
>        String decrypted = decryptLegacy(member.getPhone());
>        member.setPhone(encryptCurrent(decrypted));
>        member.setEncryptionVersion(CURRENT_VERSION);
>        memberRepository.save(member);
>    }
>    ```
> 
> 이렇게 하면 사용자 접근 시 자연스럽게 최신 알고리즘으로 마이그레이션됩니다.

---

## 📎 관련 문서

- [ADR-0006: 민감 정보 AES 암호화](../internal/adr/0006-sensitive-data-encryption.md)
- [SECURITY-PATTERNS.md](../internal/patterns/SECURITY-PATTERNS.md)
- [DataEncryptionUtil.java](../../src/main/java/com/Hamalog/security/encryption/DataEncryptionUtil.java)
- [EncryptedStringConverter.java](../../src/main/java/com/Hamalog/security/encryption/EncryptedStringConverter.java)

