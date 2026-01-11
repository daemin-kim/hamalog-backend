# Hamalog Kotlin 가이드

> 이 문서는 Java 개발자를 위한 Kotlin 빠른 학습 가이드입니다.
> 프로젝트에서 Kotlin을 사용할 때 필요한 핵심 문법과 패턴을 다룹니다.

---

## 목차

1. [기본 문법](#1-기본-문법)
2. [Null 안전성](#2-null-안전성)
3. [Data Class](#3-data-class)
4. [확장 함수](#4-확장-함수)
5. [컬렉션 처리](#5-컬렉션-처리)
6. [스코프 함수](#6-스코프-함수)
7. [Java 상호운용](#7-java-상호운용)
8. [Hamalog 프로젝트 규칙](#8-hamalog-프로젝트-규칙)

---

## 1. 기본 문법

### 1.1 변수 선언

```kotlin
// val: 읽기 전용 (Java의 final과 유사)
val name: String = "홍길동"
val age = 25  // 타입 추론

// var: 변경 가능
var count: Int = 0
count = 10
```

### 1.2 함수 선언

```kotlin
// 기본 함수
fun greet(name: String): String {
    return "안녕하세요, $name님!"
}

// 단일 표현식 함수 (한 줄로 표현 가능할 때)
fun greet(name: String): String = "안녕하세요, $name님!"

// 기본값 파라미터
fun createUser(name: String, age: Int = 0, active: Boolean = true): User {
    return User(name, age, active)
}

// 호출 시 named arguments 사용
val user = createUser(name = "홍길동", active = false)
```

### 1.3 문자열 템플릿

```kotlin
val name = "홍길동"
val age = 25

// 변수 삽입
println("이름: $name")

// 표현식 삽입
println("나이: ${age + 1}세")
println("이름 길이: ${name.length}")
```

### 1.4 조건문

```kotlin
// if는 표현식 (값을 반환)
val max = if (a > b) a else b

// when (Java의 switch 대체, 더 강력함)
fun getMoodEmoji(mood: String): String = when (mood) {
    "HAPPY" -> "😊"
    "SAD" -> "😢"
    "ANGRY" -> "😠"
    else -> "😐"
}

// when with 범위
fun getGrade(score: Int): String = when (score) {
    in 90..100 -> "A"
    in 80..89 -> "B"
    in 70..79 -> "C"
    else -> "F"
}
```

---

## 2. Null 안전성

Kotlin의 가장 큰 장점 중 하나입니다. NullPointerException을 컴파일 타임에 방지합니다.

### 2.1 Nullable 타입

```kotlin
// Non-null 타입 (null 불가)
var name: String = "홍길동"
// name = null  // 컴파일 에러!

// Nullable 타입 (null 허용)
var nickname: String? = "길동이"
nickname = null  // OK
```

### 2.2 안전 호출 연산자 (?.)

```kotlin
val nickname: String? = getMember()?.nickName

// 체이닝 가능
val length: Int? = getMember()?.nickName?.length
```

### 2.3 엘비스 연산자 (?:)

```kotlin
// null일 경우 기본값 제공
val displayName = nickname ?: "익명"

// null일 경우 예외 던지기
val name = member?.name ?: throw IllegalStateException("이름이 없습니다")

// null일 경우 early return
fun process(input: String?) {
    val value = input ?: return
    // value는 여기서 non-null
}
```

### 2.4 Non-null 단언 (!!)

```kotlin
// null이 아님을 단언 (주의: NPE 발생 가능)
val name: String = nullableName!!

// ⚠️ 가능하면 사용을 피하고, 안전 호출이나 엘비스 연산자를 사용하세요
```

### 2.5 let 함수와 조합

```kotlin
// null이 아닐 때만 블록 실행
member?.let { m ->
    println("회원 이름: ${m.name}")
    sendWelcomeEmail(m)
}

// 간단한 경우 it 사용
nickname?.let { println("닉네임: $it") }
```

---

## 3. Data Class

Java의 record와 유사하지만 더 강력합니다.

### 3.1 기본 사용법

```kotlin
// Java record 대체
data class MedicationScheduleResponse(
    val medicationScheduleId: Long,
    val memberId: Long,
    val name: String,
    val hospitalName: String,
    val prescriptionDate: LocalDate,
    val memo: String?,  // nullable
    val startOfAd: LocalDate,
    val prescriptionDays: Int,
    val perDay: Int,
    val alarmType: AlarmType
)

// 자동 생성되는 메서드:
// - equals() / hashCode()
// - toString()
// - copy()
// - componentN() (구조 분해용)
```

### 3.2 copy() 함수

```kotlin
val original = MedicationScheduleResponse(
    medicationScheduleId = 1L,
    memberId = 1L,
    name = "혈압약",
    // ... 기타 필드
)

// 일부 필드만 변경한 복사본 생성
val updated = original.copy(name = "고혈압약")
```

### 3.3 구조 분해 (Destructuring)

```kotlin
val (id, memberId, name) = response

// 필요 없는 필드는 _로 무시
val (id, _, name) = response
```

### 3.4 Validation 어노테이션과 함께 사용

```kotlin
import jakarta.validation.constraints.*

data class MedicationScheduleCreateRequest(
    @field:NotNull(message = "{medicationSchedule.memberId.notNull}")
    val memberId: Long,

    @field:NotBlank(message = "{medicationSchedule.name.notBlank}")
    @field:Size(max = 20, message = "{medicationSchedule.name.size}")
    val name: String,

    @field:Size(max = 500, message = "{medicationSchedule.memo.size}")
    val memo: String? = null  // 기본값으로 optional 처리
)
```

> ⚠️ **중요**: Kotlin에서 Java Bean Validation을 사용할 때는 `@field:` 접두사가 필요합니다.

---

## 4. 확장 함수

기존 클래스에 새로운 함수를 추가할 수 있습니다 (상속 없이!).

### 4.1 기본 사용법

```kotlin
// String에 확장 함수 추가
fun String.toPhoneFormat(): String {
    return this.replace(Regex("(\\d{3})(\\d{4})(\\d{4})"), "$1-$2-$3")
}

// 사용
val formatted = "01012345678".toPhoneFormat()  // "010-1234-5678"
```

### 4.2 프로젝트에서 유용한 확장 함수 예시

```kotlin
// LocalDate 확장
fun LocalDate.isWithinPrescriptionPeriod(startDate: LocalDate, days: Int): Boolean {
    val endDate = startDate.plusDays(days.toLong())
    return !this.isBefore(startDate) && !this.isAfter(endDate)
}

// Entity -> Response 변환
fun MedicationSchedule.toResponse(): MedicationScheduleResponse {
    return MedicationScheduleResponse(
        medicationScheduleId = this.medicationScheduleId,
        memberId = this.member.memberId,
        name = this.name,
        // ...
    )
}

// 리스트 확장
fun <T> List<T>.takeIfNotEmpty(): List<T>? = if (this.isNotEmpty()) this else null
```

### 4.3 확장 프로퍼티

```kotlin
// 프로퍼티도 확장 가능
val LocalDate.isToday: Boolean
    get() = this == LocalDate.now()

// 사용
if (prescriptionDate.isToday) {
    println("오늘 처방된 약입니다")
}
```

---

## 5. 컬렉션 처리

Kotlin의 컬렉션 함수는 Java Stream보다 간결합니다.

### 5.1 기본 변환

```kotlin
val schedules: List<MedicationSchedule> = repository.findAll()

// map: 변환
val names: List<String> = schedules.map { it.name }

// filter: 필터링
val activeSchedules = schedules.filter { it.isActive }

// 체이닝
val activeNames = schedules
    .filter { it.isActive }
    .map { it.name }
    .sorted()
```

### 5.2 자주 사용하는 함수들

```kotlin
// find: 첫 번째 매칭 요소 (없으면 null)
val found = schedules.find { it.name == "혈압약" }

// first / firstOrNull
val first = schedules.firstOrNull { it.isActive }

// any / all / none: 조건 검사
val hasActive = schedules.any { it.isActive }
val allActive = schedules.all { it.isActive }
val noneExpired = schedules.none { it.isExpired }

// groupBy: 그룹핑
val byHospital: Map<String, List<MedicationSchedule>> = 
    schedules.groupBy { it.hospitalName }

// associate: Map으로 변환
val idToSchedule: Map<Long, MedicationSchedule> = 
    schedules.associateBy { it.medicationScheduleId }

// sumOf / maxOf / minOf
val totalDays = schedules.sumOf { it.prescriptionDays }
```

### 5.3 Sequence (지연 연산)

```kotlin
// 대용량 데이터에서 성능 최적화
val result = schedules.asSequence()
    .filter { it.isActive }
    .map { it.name }
    .take(10)
    .toList()  // 최종 연산에서만 실행
```

---

## 6. 스코프 함수

객체 컨텍스트 내에서 코드 블록을 실행하는 함수들입니다.

### 6.1 let

```kotlin
// null 체크와 함께 사용
member?.let { m ->
    sendEmail(m.email)
    logActivity(m.id)
}

// 변환에 사용
val length = name?.let { it.length } ?: 0
```

### 6.2 apply

```kotlin
// 객체 초기화에 유용 (this 반환)
val schedule = MedicationSchedule().apply {
    name = "혈압약"
    hospitalName = "서울병원"
    prescriptionDays = 30
}
```

### 6.3 also

```kotlin
// 부수 효과 처리 (원본 객체 반환)
val schedule = createSchedule().also { 
    logger.info("스케줄 생성: ${it.name}")
}
```

### 6.4 run

```kotlin
// 객체 초기화 + 결과 반환
val result = schedule.run {
    validatePrescription()
    calculateEndDate()  // 마지막 표현식이 반환값
}
```

### 6.5 with

```kotlin
// 비-null 객체에 여러 작업 수행
with(schedule) {
    println("약 이름: $name")
    println("병원: $hospitalName")
    println("기간: $prescriptionDays일")
}
```

### 6.6 스코프 함수 선택 가이드

| 함수 | 객체 참조 | 반환값 | 사용 케이스 |
|------|----------|--------|------------|
| `let` | `it` | 람다 결과 | null 체크, 변환 |
| `run` | `this` | 람다 결과 | 객체 설정 + 결과 계산 |
| `with` | `this` | 람다 결과 | non-null 객체 여러 작업 |
| `apply` | `this` | 객체 자체 | 객체 초기화 |
| `also` | `it` | 객체 자체 | 부수 효과 (로깅 등) |

---

## 7. Java 상호운용

Kotlin과 Java는 100% 상호운용 가능합니다.

### 7.1 Java 코드에서 Kotlin 호출

```java
// Kotlin data class를 Java에서 사용
MedicationScheduleResponse response = MedicationScheduleResponseKt.from(entity);

// Kotlin 확장 함수 호출
StringExtensionsKt.toPhoneFormat("01012345678");
```

### 7.2 Kotlin에서 Java 호출

```kotlin
// Java 클래스 그대로 사용
val member: Member = memberRepository.findById(1L)
    .orElseThrow { MemberNotFoundException() }

// Java Stream 대신 Kotlin 컬렉션 함수 사용 가능
val names = memberRepository.findAll()
    .map { it.name }  // Java List도 Kotlin 함수 사용 가능
```

### 7.3 @JvmStatic, @JvmOverloads

```kotlin
// companion object 메서드를 Java static으로 노출
data class Response(val id: Long, val name: String) {
    companion object {
        @JvmStatic
        fun from(entity: Entity): Response = Response(entity.id, entity.name)
    }
}

// 기본값 파라미터를 Java 오버로딩으로 노출
@JvmOverloads
fun createUser(name: String, age: Int = 0, active: Boolean = true): User {
    return User(name, age, active)
}
```

### 7.4 Nullable 처리 (@Nullable / @NotNull)

```kotlin
// Java에서 오는 nullable 타입 처리
fun processJavaString(str: String?) {
    val length = str?.length ?: 0
}
```

---

## 8. Hamalog 프로젝트 규칙

### 8.1 파일 위치

```
src/main/kotlin/com/Hamalog/
├── dto/{도메인}/request/      # Kotlin DTO Request
├── dto/{도메인}/response/     # Kotlin DTO Response
└── util/                       # 확장 함수, 유틸리티
```

### 8.2 DTO 작성 규칙

```kotlin
package com.Hamalog.dto.medication.request

import jakarta.validation.constraints.*
import java.time.LocalDate
import com.Hamalog.domain.medication.AlarmType

/**
 * 복약 스케줄 생성 요청 DTO
 */
data class MedicationScheduleCreateRequest(
    @field:NotNull(message = "{medicationSchedule.memberId.notNull}")
    val memberId: Long,

    @field:NotBlank(message = "{medicationSchedule.name.notBlank}")
    @field:Size(max = 20, message = "{medicationSchedule.name.size}")
    val name: String,

    @field:NotBlank(message = "{medicationSchedule.hospitalName.notBlank}")
    @field:Size(max = 20, message = "{medicationSchedule.hospitalName.size}")
    val hospitalName: String,

    @field:NotNull(message = "{medicationSchedule.prescriptionDate.notNull}")
    val prescriptionDate: LocalDate,

    @field:Size(max = 500, message = "{medicationSchedule.memo.size}")
    val memo: String? = null,

    @field:NotNull(message = "{medicationSchedule.startOfAd.notNull}")
    val startOfAd: LocalDate,

    @field:NotNull(message = "{medicationSchedule.prescriptionDays.notNull}")
    @field:Min(value = 1, message = "{medicationSchedule.prescriptionDays.min}")
    val prescriptionDays: Int,

    @field:NotNull(message = "{medicationSchedule.perDay.notNull}")
    @field:Min(value = 1, message = "{medicationSchedule.perDay.min}")
    val perDay: Int,

    @field:NotNull(message = "{medicationSchedule.alarmType.notNull}")
    val alarmType: AlarmType
)
```

### 8.3 Response DTO with companion object

```kotlin
package com.Hamalog.dto.medication.response

import com.Hamalog.domain.medication.MedicationSchedule
import com.Hamalog.domain.medication.AlarmType
import java.time.LocalDate

/**
 * 복약 스케줄 응답 DTO
 */
data class MedicationScheduleResponse(
    val medicationScheduleId: Long,
    val memberId: Long,
    val name: String,
    val hospitalName: String,
    val prescriptionDate: LocalDate,
    val memo: String?,
    val startOfAd: LocalDate,
    val prescriptionDays: Int,
    val perDay: Int,
    val alarmType: AlarmType,
    val isActive: Boolean
) {
    companion object {
        @JvmStatic  // Java에서 static 메서드로 호출 가능
        fun from(entity: MedicationSchedule): MedicationScheduleResponse {
            return MedicationScheduleResponse(
                medicationScheduleId = entity.medicationScheduleId,
                memberId = entity.member.memberId,
                name = entity.name,
                hospitalName = entity.hospitalName,
                prescriptionDate = entity.prescriptionDate,
                memo = entity.memo,
                startOfAd = entity.startOfAd,
                prescriptionDays = entity.prescriptionDays,
                perDay = entity.perDay,
                alarmType = entity.alarmType,
                isActive = entity.isActive
            )
        }
    }
}
```

### 8.4 확장 함수 파일

```kotlin
// src/main/kotlin/com/Hamalog/util/DateExtensions.kt
package com.Hamalog.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * LocalDate 확장 함수
 */
val LocalDate.isToday: Boolean
    get() = this == LocalDate.now()

fun LocalDate.toKoreanFormat(): String =
    this.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"))

fun LocalDate.isWithinRange(start: LocalDate, end: LocalDate): Boolean =
    !this.isBefore(start) && !this.isAfter(end)

/**
 * LocalDateTime 확장 함수
 */
fun LocalDateTime.toKoreanFormat(): String =
    this.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH:mm"))
```

### 8.5 코드 스타일

1. **들여쓰기**: 4 스페이스
2. **최대 줄 길이**: 120자
3. **후행 쉼표**: 여러 줄 파라미터에서 사용 권장
4. **import**: 와일드카드 허용 (ktlint 설정에서 비활성화됨)

```kotlin
// 후행 쉼표 예시
data class Example(
    val field1: String,
    val field2: Int,
    val field3: Boolean,  // 후행 쉼표
)
```

---

## 참고 자료

- [Kotlin 공식 문서](https://kotlinlang.org/docs/home.html)
- [Kotlin for Java Developers](https://kotlinlang.org/docs/java-to-kotlin-idioms-strings.html)
- [Spring Boot with Kotlin](https://spring.io/guides/tutorials/spring-boot-kotlin)
- [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)

---

> 📝 이 문서에 대한 질문이나 개선 제안은 팀 채널에 공유해 주세요.

