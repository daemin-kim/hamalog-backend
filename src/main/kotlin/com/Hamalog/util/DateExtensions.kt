package com.Hamalog.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * LocalDate 확장 함수 모음
 */

/**
 * 오늘 날짜인지 확인
 */
val LocalDate.isToday: Boolean
    get() = this == LocalDate.now()

/**
 * 과거 날짜인지 확인
 */
val LocalDate.isPast: Boolean
    get() = this.isBefore(LocalDate.now())

/**
 * 미래 날짜인지 확인
 */
val LocalDate.isFuture: Boolean
    get() = this.isAfter(LocalDate.now())

/**
 * 한국어 형식으로 변환 (예: 2025년 01월 11일)
 */
fun LocalDate.toKoreanFormat(): String = this.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"))

/**
 * ISO 형식으로 변환 (예: 2025-01-11)
 */
fun LocalDate.toIsoFormat(): String = this.format(DateTimeFormatter.ISO_LOCAL_DATE)

/**
 * 주어진 범위 내에 있는지 확인 (시작일, 종료일 포함)
 */
fun LocalDate.isWithinRange(start: LocalDate, end: LocalDate): Boolean = !this.isBefore(start) && !this.isAfter(end)

/**
 * 처방 기간 내에 있는지 확인
 * @param startDate 복용 시작일
 * @param days 처방 일수
 */
fun LocalDate.isWithinPrescriptionPeriod(startDate: LocalDate, days: Int): Boolean {
    val endDate = startDate.plusDays(days.toLong() - 1)
    return this.isWithinRange(startDate, endDate)
}

/**
 * LocalDateTime 확장 함수 모음
 */

/**
 * 한국어 날짜시간 형식으로 변환 (예: 2025년 01월 11일 14:30)
 */
fun LocalDateTime.toKoreanFormat(): String = this.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH:mm"))

/**
 * 한국어 날짜시간 형식으로 변환 (초 포함)
 */
fun LocalDateTime.toKoreanFormatWithSeconds(): String =
    this.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH:mm:ss"))

/**
 * ISO 형식으로 변환 (예: 2025-01-11T14:30:00)
 */
fun LocalDateTime.toIsoFormat(): String = this.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
