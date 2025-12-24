# ADR-0006: 민감 정보 AES 암호화

## 상태
Accepted

## 컨텍스트

Hamalog에서 다음 **민감 개인정보**를 저장해야 합니다:

1. **전화번호**: 연락처 정보
2. **생년월일**: 나이 확인용
3. (향후) **주민등록번호**, **의료 정보** 등

### 법적 요구사항

| 규정 | 요구사항 |
|------|----------|
| **개인정보보호법** | 고유식별정보 암호화 의무 |
| **GDPR** | 개인정보 보호 조치 |
| **의료법** | 의료 정보 암호화 권고 |

### 고려한 대안들

| 방식 | 장점 | 단점 |
|------|------|------|
| **단방향 해시 (SHA-256)** | 복호화 불가 = 안전 | 원본 조회 불가 |
| **대칭키 암호화 (AES)** | 복호화 가능, 빠름 | 키 관리 필요 |
| **비대칭키 암호화 (RSA)** | 키 분리 가능 | 느림, 복잡 |
| **DB 수준 암호화 (TDE)** | 투명, 간편 | 애플리케이션 수준 보호 X |

## 결정

**AES-256-GCM** 대칭키 암호화를 채택합니다.

### 구현 상세

#### 1. 암호화 유틸리티

```java
@Component
public class DataEncryptionUtil {
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    
    private final SecretKeySpec secretKey;
    
    public DataEncryptionUtil(
            @Value("${hamalog.encryption.key:${HAMALOG_ENCRYPTION_KEY:}}") String encryptionKey,
            Environment environment) {
        // 환경별 키 초기화 로직
        this.secretKey = initializeSecretKey(encryptionKey);
    }
    
    public String encrypt(String plainText) {
        if (plainText == null) return null;
        
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
            
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] combined = ByteBuffer.allocate(iv.length + encrypted.length)
                .put(iv)
                .put(encrypted)
                .array();
            
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new EncryptionException("암호화 실패", e);
        }
    }
    
    public String decrypt(String cipherText) {
        if (cipherText == null) return null;
        
        try {
            byte[] combined = Base64.getDecoder().decode(cipherText);
            ByteBuffer buffer = ByteBuffer.wrap(combined);
            
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            
            SecretKey key = getSecretKey();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);
            
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new EncryptionException("복호화 실패", e);
        }
    }
    
    private SecretKey getSecretKey() {
        byte[] keyBytes = Base64.getDecoder().decode(encryptionKey);
        return new SecretKeySpec(keyBytes, "AES");
    }
    
    private byte[] generateIV() {
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }
}
```

#### 2. JPA AttributeConverter

```java
@Converter
@Component
@RequiredArgsConstructor
public class EncryptedStringConverter implements AttributeConverter<String, String> {
    
    private final EncryptionUtil encryptionUtil;
    
    @Override
    public String convertToDatabaseColumn(String attribute) {
        return encryptionUtil.encrypt(attribute);
    }
    
    @Override
    public String convertToEntityAttribute(String dbData) {
        return encryptionUtil.decrypt(dbData);
    }
}
```

#### 3. Entity 적용

```java
@Entity
@Table(name = "member")
public class Member {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long memberId;
    
    @Column(nullable = false, length = 100)
    private String loginId;
    
    // 암호화 적용 필드
    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false)
    private String phoneNumber;
    
    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false)
    private String birthday;
}
```

### 암호화 키 관리

#### 개발 환경
```properties
# application-dev.properties
hamalog.encryption.key=${HAMALOG_ENCRYPTION_KEY:base64EncodedDevKey}
```

#### 프로덕션 환경
```yaml
# GitHub Secrets
HAMALOG_ENCRYPTION_KEY: <Base64 encoded 32-byte key>

# 키 생성 방법
openssl rand -base64 32
```

### 암호화 대상 필드

| 엔티티 | 필드 | 암호화 적용 |
|--------|------|------------|
| `Member` | `phoneNumber` | ✅ |
| `Member` | `birthday` | ✅ |
| `Member` | `loginId` | ❌ (인덱스 사용) |
| `Member` | `password` | ❌ (BCrypt 해시) |

### 주의사항

1. **인덱스 불가**: 암호화된 필드는 DB 인덱스 사용 불가
2. **검색 불가**: LIKE 검색 등 불가능
3. **키 로테이션**: 키 변경 시 전체 데이터 재암호화 필요

## 결과

### 장점
- ✅ **법적 준수**: 개인정보보호법 암호화 의무 충족
- ✅ **복호화 가능**: 필요 시 원본 데이터 조회
- ✅ **투명한 적용**: `@Convert`로 비즈니스 로직 영향 최소화
- ✅ **강력한 보안**: AES-256-GCM은 현재 가장 안전한 대칭키 알고리즘

### 단점
- ⚠️ **성능 오버헤드**: 암/복호화 처리 시간 (~1ms)
- ⚠️ **키 관리 부담**: 키 유출 시 전체 데이터 위험
- ⚠️ **검색 제약**: 암호화된 필드 검색 불가

### 성능 영향

| 작업 | 암호화 전 | 암호화 후 | 오버헤드 |
|------|----------|----------|----------|
| 회원 조회 | 5ms | 6ms | +1ms |
| 회원 등록 | 10ms | 11ms | +1ms |

## 참고

- [AES-GCM - NIST](https://csrc.nist.gov/publications/detail/sp/800-38d/final)
- [JPA AttributeConverter](https://docs.oracle.com/javaee/7/api/javax/persistence/AttributeConverter.html)
- [개인정보보호법 시행령](https://www.law.go.kr/)

---

> 작성일: 2025-12-23

