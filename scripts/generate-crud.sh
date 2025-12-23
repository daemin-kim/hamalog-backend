#!/bin/bash

# =============================================================================
# Hamalog CRUD 스캐폴딩 스크립트
# =============================================================================
# 사용법: ./scripts/generate-crud.sh --domain <DomainName> --fields "<field1:Type1,field2:Type2>"
# 예시: ./scripts/generate-crud.sh --domain Notification --fields "title:String,content:String,isRead:Boolean"
# =============================================================================

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 프로젝트 루트 경로
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PACKAGE_PATH="src/main/java/com/Hamalog"
TEST_PATH="src/test/java/com/Hamalog"

# 도움말 출력
show_help() {
    echo -e "${BLUE}Hamalog CRUD 스캐폴딩 스크립트${NC}"
    echo ""
    echo "사용법:"
    echo "  ./scripts/generate-crud.sh --domain <DomainName> [--fields \"<fields>\"]"
    echo ""
    echo "옵션:"
    echo "  --domain, -d    도메인 이름 (PascalCase, 필수)"
    echo "  --fields, -f    필드 정의 (선택)"
    echo "  --help, -h      도움말 출력"
    echo ""
    echo "필드 형식:"
    echo "  \"field1:Type1,field2:Type2,...\""
    echo ""
    echo "지원 타입:"
    echo "  String, Long, Integer, Boolean, LocalDate, LocalDateTime"
    echo ""
    echo "예시:"
    echo "  ./scripts/generate-crud.sh --domain Notification --fields \"title:String,content:String,isRead:Boolean\""
    echo ""
}

# 인자 파싱
DOMAIN=""
FIELDS=""

while [[ $# -gt 0 ]]; do
    case $1 in
        --domain|-d)
            DOMAIN="$2"
            shift 2
            ;;
        --fields|-f)
            FIELDS="$2"
            shift 2
            ;;
        --help|-h)
            show_help
            exit 0
            ;;
        *)
            echo -e "${RED}알 수 없는 옵션: $1${NC}"
            show_help
            exit 1
            ;;
    esac
done

# 필수 인자 검증
if [ -z "$DOMAIN" ]; then
    echo -e "${RED}오류: --domain 옵션은 필수입니다${NC}"
    show_help
    exit 1
fi

# 이름 변환 함수
to_camel_case() {
    echo "$1" | sed 's/\([A-Z]\)/_\L\1/g' | sed 's/^_//'
}

to_snake_case() {
    echo "$1" | sed 's/\([A-Z]\)/_\L\1/g' | sed 's/^_//' | tr '[:upper:]' '[:lower:]'
}

to_kebab_case() {
    echo "$1" | sed 's/\([A-Z]\)/-\L\1/g' | sed 's/^-//'
}

# 변수 설정
DOMAIN_LOWER=$(echo "$DOMAIN" | tr '[:upper:]' '[:lower:]')
DOMAIN_CAMEL=$(echo "${DOMAIN:0:1}" | tr '[:upper:]' '[:lower:]')${DOMAIN:1}
DOMAIN_SNAKE=$(to_snake_case "$DOMAIN")
DOMAIN_KEBAB=$(to_kebab_case "$DOMAIN")

echo -e "${BLUE}=== Hamalog CRUD 스캐폴딩 ===${NC}"
echo -e "도메인: ${GREEN}$DOMAIN${NC}"
echo -e "필드: ${YELLOW}$FIELDS${NC}"
echo ""

# 디렉토리 생성
echo -e "${YELLOW}디렉토리 생성 중...${NC}"

DIRS=(
    "$PROJECT_ROOT/$PACKAGE_PATH/domain/$DOMAIN_LOWER"
    "$PROJECT_ROOT/$PACKAGE_PATH/dto/$DOMAIN_LOWER/request"
    "$PROJECT_ROOT/$PACKAGE_PATH/dto/$DOMAIN_LOWER/response"
    "$PROJECT_ROOT/$PACKAGE_PATH/repository/$DOMAIN_LOWER"
    "$PROJECT_ROOT/$PACKAGE_PATH/service/$DOMAIN_LOWER"
    "$PROJECT_ROOT/$PACKAGE_PATH/controller/$DOMAIN_LOWER"
    "$PROJECT_ROOT/$TEST_PATH/service/$DOMAIN_LOWER"
)

for dir in "${DIRS[@]}"; do
    mkdir -p "$dir"
    echo -e "  ${GREEN}✓${NC} $dir"
done

# 필드 파싱
parse_fields() {
    local fields_str="$1"
    local result=""

    if [ -n "$fields_str" ]; then
        IFS=',' read -ra FIELD_ARRAY <<< "$fields_str"
        for field in "${FIELD_ARRAY[@]}"; do
            IFS=':' read -ra PARTS <<< "$field"
            local name="${PARTS[0]}"
            local type="${PARTS[1]}"
            result+="    private $type $name;\n"
        done
    fi

    echo -e "$result"
}

ENTITY_FIELDS=$(parse_fields "$FIELDS")

# Entity 생성
echo -e "${YELLOW}Entity 생성 중...${NC}"
cat > "$PROJECT_ROOT/$PACKAGE_PATH/domain/$DOMAIN_LOWER/$DOMAIN.java" << EOF
package com.Hamalog.domain.$DOMAIN_LOWER;

import com.Hamalog.domain.member.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * $DOMAIN 엔티티
 */
@Entity
@Table(name = "$DOMAIN_SNAKE")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class $DOMAIN {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ${DOMAIN_CAMEL}Id;

$ENTITY_FIELDS
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 생성자
    public $DOMAIN(Member member) {
        this.member = member;
    }
}
EOF
echo -e "  ${GREEN}✓${NC} $DOMAIN.java"

# Create Request DTO 생성
echo -e "${YELLOW}Request DTO 생성 중...${NC}"
cat > "$PROJECT_ROOT/$PACKAGE_PATH/dto/$DOMAIN_LOWER/request/${DOMAIN}CreateRequest.java" << EOF
package com.Hamalog.dto.$DOMAIN_LOWER.request;

import jakarta.validation.constraints.NotNull;

/**
 * $DOMAIN 생성 요청 DTO
 */
public record ${DOMAIN}CreateRequest(
    @NotNull(message = "회원 ID는 필수입니다")
    Long memberId
    // TODO: 추가 필드 정의
) {}
EOF
echo -e "  ${GREEN}✓${NC} ${DOMAIN}CreateRequest.java"

# Update Request DTO 생성
cat > "$PROJECT_ROOT/$PACKAGE_PATH/dto/$DOMAIN_LOWER/request/${DOMAIN}UpdateRequest.java" << EOF
package com.Hamalog.dto.$DOMAIN_LOWER.request;

/**
 * $DOMAIN 수정 요청 DTO
 */
public record ${DOMAIN}UpdateRequest(
    // TODO: 수정 가능한 필드 정의
) {}
EOF
echo -e "  ${GREEN}✓${NC} ${DOMAIN}UpdateRequest.java"

# Response DTO 생성
echo -e "${YELLOW}Response DTO 생성 중...${NC}"
cat > "$PROJECT_ROOT/$PACKAGE_PATH/dto/$DOMAIN_LOWER/response/${DOMAIN}Response.java" << EOF
package com.Hamalog.dto.$DOMAIN_LOWER.response;

import com.Hamalog.domain.$DOMAIN_LOWER.$DOMAIN;

import java.time.LocalDateTime;

/**
 * $DOMAIN 응답 DTO
 */
public record ${DOMAIN}Response(
    Long ${DOMAIN_CAMEL}Id,
    Long memberId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    /**
     * Entity -> DTO 변환
     */
    public static ${DOMAIN}Response from($DOMAIN entity) {
        return new ${DOMAIN}Response(
            entity.get${DOMAIN}Id(),
            entity.getMember().getMemberId(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
EOF
echo -e "  ${GREEN}✓${NC} ${DOMAIN}Response.java"

# Repository 생성
echo -e "${YELLOW}Repository 생성 중...${NC}"
cat > "$PROJECT_ROOT/$PACKAGE_PATH/repository/$DOMAIN_LOWER/${DOMAIN}Repository.java" << EOF
package com.Hamalog.repository.$DOMAIN_LOWER;

import com.Hamalog.domain.$DOMAIN_LOWER.$DOMAIN;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * $DOMAIN Repository
 */
@Repository
public interface ${DOMAIN}Repository extends JpaRepository<$DOMAIN, Long> {

    /**
     * 회원 ID로 목록 조회 (페이징)
     */
    Page<$DOMAIN> findByMember_MemberId(Long memberId, Pageable pageable);

    /**
     * 회원 ID로 개수 조회
     */
    long countByMember_MemberId(Long memberId);

    /**
     * ID와 회원 ID로 조회 (소유권 검증용)
     */
    Optional<$DOMAIN> findBy${DOMAIN}IdAndMember_MemberId(Long ${DOMAIN_CAMEL}Id, Long memberId);

    /**
     * 회원 ID로 전체 삭제
     */
    void deleteByMember_MemberId(Long memberId);
}
EOF
echo -e "  ${GREEN}✓${NC} ${DOMAIN}Repository.java"

# Service 생성
echo -e "${YELLOW}Service 생성 중...${NC}"
cat > "$PROJECT_ROOT/$PACKAGE_PATH/service/$DOMAIN_LOWER/${DOMAIN}Service.java" << EOF
package com.Hamalog.service.$DOMAIN_LOWER;

import com.Hamalog.domain.$DOMAIN_LOWER.$DOMAIN;
import com.Hamalog.domain.member.Member;
import com.Hamalog.dto.$DOMAIN_LOWER.request.${DOMAIN}CreateRequest;
import com.Hamalog.dto.$DOMAIN_LOWER.request.${DOMAIN}UpdateRequest;
import com.Hamalog.dto.$DOMAIN_LOWER.response.${DOMAIN}Response;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.repository.$DOMAIN_LOWER.${DOMAIN}Repository;
import com.Hamalog.repository.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * $DOMAIN 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ${DOMAIN}Service {

    private final ${DOMAIN}Repository ${DOMAIN_CAMEL}Repository;
    private final MemberRepository memberRepository;

    /**
     * $DOMAIN 생성
     */
    @Transactional
    public ${DOMAIN}Response create(${DOMAIN}CreateRequest request) {
        log.info("$DOMAIN 생성 요청 - memberId: {}", request.memberId());

        // 1. 회원 조회
        Member member = memberRepository.findById(request.memberId())
            .orElseThrow(ErrorCode.MEMBER_NOT_FOUND::toException);

        // 2. 엔티티 생성
        $DOMAIN entity = new $DOMAIN(member);

        // 3. 저장
        $DOMAIN saved = ${DOMAIN_CAMEL}Repository.save(entity);

        // 4. 응답 변환
        return ${DOMAIN}Response.from(saved);
    }

    /**
     * $DOMAIN 상세 조회
     */
    public ${DOMAIN}Response findById(Long ${DOMAIN_CAMEL}Id) {
        $DOMAIN entity = ${DOMAIN_CAMEL}Repository.findById(${DOMAIN_CAMEL}Id)
            .orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);
        return ${DOMAIN}Response.from(entity);
    }

    /**
     * $DOMAIN 목록 조회 (페이징)
     */
    public Page<${DOMAIN}Response> findByMemberId(Long memberId, Pageable pageable) {
        return ${DOMAIN_CAMEL}Repository.findByMember_MemberId(memberId, pageable)
            .map(${DOMAIN}Response::from);
    }

    /**
     * $DOMAIN 수정
     */
    @Transactional
    public ${DOMAIN}Response update(Long ${DOMAIN_CAMEL}Id, ${DOMAIN}UpdateRequest request) {
        log.info("$DOMAIN 수정 요청 - id: {}", ${DOMAIN_CAMEL}Id);

        $DOMAIN entity = ${DOMAIN_CAMEL}Repository.findById(${DOMAIN_CAMEL}Id)
            .orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);

        // TODO: 필드 업데이트 로직 추가

        return ${DOMAIN}Response.from(entity);
    }

    /**
     * $DOMAIN 삭제
     */
    @Transactional
    public void delete(Long ${DOMAIN_CAMEL}Id) {
        log.info("$DOMAIN 삭제 요청 - id: {}", ${DOMAIN_CAMEL}Id);

        $DOMAIN entity = ${DOMAIN_CAMEL}Repository.findById(${DOMAIN_CAMEL}Id)
            .orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);

        ${DOMAIN_CAMEL}Repository.delete(entity);
    }

    /**
     * 소유권 검증 (AOP용)
     */
    public Long getOwnerMemberId(Long ${DOMAIN_CAMEL}Id) {
        return ${DOMAIN_CAMEL}Repository.findById(${DOMAIN_CAMEL}Id)
            .map(entity -> entity.getMember().getMemberId())
            .orElse(null);
    }
}
EOF
echo -e "  ${GREEN}✓${NC} ${DOMAIN}Service.java"

# Controller 생성
echo -e "${YELLOW}Controller 생성 중...${NC}"
cat > "$PROJECT_ROOT/$PACKAGE_PATH/controller/$DOMAIN_LOWER/${DOMAIN}Controller.java" << EOF
package com.Hamalog.controller.$DOMAIN_LOWER;

import com.Hamalog.dto.$DOMAIN_LOWER.request.${DOMAIN}CreateRequest;
import com.Hamalog.dto.$DOMAIN_LOWER.request.${DOMAIN}UpdateRequest;
import com.Hamalog.dto.$DOMAIN_LOWER.response.${DOMAIN}Response;
import com.Hamalog.security.RequireResourceOwnership;
import com.Hamalog.service.$DOMAIN_LOWER.${DOMAIN}Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * $DOMAIN API 컨트롤러
 */
@RestController
@RequestMapping("/$DOMAIN_KEBAB")
@RequiredArgsConstructor
@Tag(name = "$DOMAIN", description = "$DOMAIN 관리 API")
public class ${DOMAIN}Controller {

    private final ${DOMAIN}Service ${DOMAIN_CAMEL}Service;

    @Operation(summary = "$DOMAIN 생성")
    @PostMapping
    public ResponseEntity<${DOMAIN}Response> create(
            @Valid @RequestBody ${DOMAIN}CreateRequest request) {
        ${DOMAIN}Response response = ${DOMAIN_CAMEL}Service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "$DOMAIN 상세 조회")
    @GetMapping("/{id}")
    @RequireResourceOwnership(resourceType = "${DOMAIN}", idParam = "id")
    public ResponseEntity<${DOMAIN}Response> getById(@PathVariable Long id) {
        return ResponseEntity.ok(${DOMAIN_CAMEL}Service.findById(id));
    }

    @Operation(summary = "$DOMAIN 목록 조회")
    @GetMapping("/list/{memberId}")
    public ResponseEntity<Page<${DOMAIN}Response>> getList(
            @PathVariable Long memberId,
            Pageable pageable) {
        return ResponseEntity.ok(${DOMAIN_CAMEL}Service.findByMemberId(memberId, pageable));
    }

    @Operation(summary = "$DOMAIN 수정")
    @PutMapping("/{id}")
    @RequireResourceOwnership(resourceType = "${DOMAIN}", idParam = "id")
    public ResponseEntity<${DOMAIN}Response> update(
            @PathVariable Long id,
            @Valid @RequestBody ${DOMAIN}UpdateRequest request) {
        return ResponseEntity.ok(${DOMAIN_CAMEL}Service.update(id, request));
    }

    @Operation(summary = "$DOMAIN 삭제")
    @DeleteMapping("/{id}")
    @RequireResourceOwnership(resourceType = "${DOMAIN}", idParam = "id")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ${DOMAIN_CAMEL}Service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
EOF
echo -e "  ${GREEN}✓${NC} ${DOMAIN}Controller.java"

# Service Test 생성
echo -e "${YELLOW}Service Test 생성 중...${NC}"
cat > "$PROJECT_ROOT/$TEST_PATH/service/$DOMAIN_LOWER/${DOMAIN}ServiceTest.java" << EOF
package com.Hamalog.service.$DOMAIN_LOWER;

import com.Hamalog.domain.$DOMAIN_LOWER.$DOMAIN;
import com.Hamalog.domain.member.Member;
import com.Hamalog.dto.$DOMAIN_LOWER.request.${DOMAIN}CreateRequest;
import com.Hamalog.dto.$DOMAIN_LOWER.response.${DOMAIN}Response;
import com.Hamalog.exception.BusinessException;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.repository.$DOMAIN_LOWER.${DOMAIN}Repository;
import com.Hamalog.repository.member.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * $DOMAIN 서비스 테스트
 */
@DisplayName("$DOMAIN 서비스 테스트")
@ExtendWith(MockitoExtension.class)
class ${DOMAIN}ServiceTest {

    @Mock
    private ${DOMAIN}Repository ${DOMAIN_CAMEL}Repository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private ${DOMAIN}Service ${DOMAIN_CAMEL}Service;

    private Member testMember;

    @BeforeEach
    void setUp() {
        testMember = mock(Member.class);
        when(testMember.getMemberId()).thenReturn(1L);
    }

    @Nested
    @DisplayName("생성")
    class Create {

        @Test
        @DisplayName("성공: 유효한 요청으로 생성")
        void success_withValidRequest() {
            // given
            ${DOMAIN}CreateRequest request = new ${DOMAIN}CreateRequest(1L);
            $DOMAIN entity = mock($DOMAIN.class);

            when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
            when(${DOMAIN_CAMEL}Repository.save(any())).thenReturn(entity);
            when(entity.get${DOMAIN}Id()).thenReturn(1L);
            when(entity.getMember()).thenReturn(testMember);

            // when
            ${DOMAIN}Response result = ${DOMAIN_CAMEL}Service.create(request);

            // then
            assertThat(result).isNotNull();
            verify(${DOMAIN_CAMEL}Repository).save(any());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 회원")
        void fail_memberNotFound() {
            // given
            ${DOMAIN}CreateRequest request = new ${DOMAIN}CreateRequest(999L);
            when(memberRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> ${DOMAIN_CAMEL}Service.create(request))
                .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("조회")
    class FindById {

        @Test
        @DisplayName("성공: 존재하는 ID로 조회")
        void success_withExistingId() {
            // given
            $DOMAIN entity = mock($DOMAIN.class);
            when(entity.get${DOMAIN}Id()).thenReturn(1L);
            when(entity.getMember()).thenReturn(testMember);
            when(${DOMAIN_CAMEL}Repository.findById(1L)).thenReturn(Optional.of(entity));

            // when
            ${DOMAIN}Response result = ${DOMAIN_CAMEL}Service.findById(1L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.${DOMAIN_CAMEL}Id()).isEqualTo(1L);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 ID")
        void fail_notFound() {
            // given
            when(${DOMAIN_CAMEL}Repository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> ${DOMAIN_CAMEL}Service.findById(999L))
                .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("삭제")
    class Delete {

        @Test
        @DisplayName("성공: 존재하는 ID로 삭제")
        void success_withExistingId() {
            // given
            $DOMAIN entity = mock($DOMAIN.class);
            when(${DOMAIN_CAMEL}Repository.findById(1L)).thenReturn(Optional.of(entity));

            // when
            ${DOMAIN_CAMEL}Service.delete(1L);

            // then
            verify(${DOMAIN_CAMEL}Repository).delete(entity);
        }
    }
}
EOF
echo -e "  ${GREEN}✓${NC} ${DOMAIN}ServiceTest.java"

echo ""
echo -e "${GREEN}=== 스캐폴딩 완료! ===${NC}"
echo ""
echo -e "생성된 파일:"
echo -e "  📁 domain/$DOMAIN_LOWER/$DOMAIN.java"
echo -e "  📁 dto/$DOMAIN_LOWER/request/${DOMAIN}CreateRequest.java"
echo -e "  📁 dto/$DOMAIN_LOWER/request/${DOMAIN}UpdateRequest.java"
echo -e "  📁 dto/$DOMAIN_LOWER/response/${DOMAIN}Response.java"
echo -e "  📁 repository/$DOMAIN_LOWER/${DOMAIN}Repository.java"
echo -e "  📁 service/$DOMAIN_LOWER/${DOMAIN}Service.java"
echo -e "  📁 controller/$DOMAIN_LOWER/${DOMAIN}Controller.java"
echo -e "  📁 test/service/$DOMAIN_LOWER/${DOMAIN}ServiceTest.java"
echo ""
echo -e "${YELLOW}다음 단계:${NC}"
echo -e "  1. Entity 필드 및 생성자 완성"
echo -e "  2. DTO 필드 추가"
echo -e "  3. Service 비즈니스 로직 구현"
echo -e "  4. DB 마이그레이션 스크립트 추가 (V{n}__Add_${DOMAIN_SNAKE}_table.sql)"
echo -e "  5. ErrorCode에 ${DOMAIN}_NOT_FOUND 추가"
echo -e "  6. ResourceOwnershipService에 리소스 타입 추가"
echo ""

