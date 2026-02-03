package com.Hamalog.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * DateExtensions.kt 단위 테스트
 */
@DisplayName("DateExtensions 테스트")
class DateExtensionsTest {

    @Nested
    @DisplayName("LocalDate 확장 프로퍼티")
    inner class LocalDateProperties {

        @Test
        @DisplayName("isToday: 오늘 날짜인 경우 true 반환")
        fun isToday_today_returnsTrue() {
            // given
            val today = LocalDate.now()

            // when & then
            assertThat(today.isToday).isTrue()
        }

        @Test
        @DisplayName("isToday: 어제 날짜인 경우 false 반환")
        fun isToday_yesterday_returnsFalse() {
            // given
            val yesterday = LocalDate.now().minusDays(1)

            // when & then
            assertThat(yesterday.isToday).isFalse()
        }

        @Test
        @DisplayName("isPast: 과거 날짜인 경우 true 반환")
        fun isPast_pastDate_returnsTrue() {
            // given
            val pastDate = LocalDate.now().minusDays(1)

            // when & then
            assertThat(pastDate.isPast).isTrue()
        }

        @Test
        @DisplayName("isPast: 오늘 날짜인 경우 false 반환")
        fun isPast_today_returnsFalse() {
            // given
            val today = LocalDate.now()

            // when & then
            assertThat(today.isPast).isFalse()
        }

        @Test
        @DisplayName("isFuture: 미래 날짜인 경우 true 반환")
        fun isFuture_futureDate_returnsTrue() {
            // given
            val futureDate = LocalDate.now().plusDays(1)

            // when & then
            assertThat(futureDate.isFuture).isTrue()
        }

        @Test
        @DisplayName("isFuture: 오늘 날짜인 경우 false 반환")
        fun isFuture_today_returnsFalse() {
            // given
            val today = LocalDate.now()

            // when & then
            assertThat(today.isFuture).isFalse()
        }
    }

    @Nested
    @DisplayName("LocalDate 포맷 변환")
    inner class LocalDateFormatting {

        @Test
        @DisplayName("toKoreanFormat: 한국어 형식으로 변환")
        fun toKoreanFormat_success() {
            // given
            val date = LocalDate.of(2025, 1, 11)

            // when
            val result = date.toKoreanFormat()

            // then
            assertThat(result).isEqualTo("2025년 01월 11일")
        }

        @Test
        @DisplayName("toIsoFormat: ISO 형식으로 변환")
        fun toIsoFormat_success() {
            // given
            val date = LocalDate.of(2025, 1, 11)

            // when
            val result = date.toIsoFormat()

            // then
            assertThat(result).isEqualTo("2025-01-11")
        }
    }

    @Nested
    @DisplayName("LocalDate 범위 확인")
    inner class LocalDateRange {

        @Test
        @DisplayName("isWithinRange: 범위 내 날짜인 경우 true 반환")
        fun isWithinRange_withinRange_returnsTrue() {
            // given
            val date = LocalDate.of(2025, 1, 15)
            val start = LocalDate.of(2025, 1, 10)
            val end = LocalDate.of(2025, 1, 20)

            // when & then
            assertThat(date.isWithinRange(start, end)).isTrue()
        }

        @Test
        @DisplayName("isWithinRange: 시작일과 같은 경우 true 반환")
        fun isWithinRange_startDate_returnsTrue() {
            // given
            val date = LocalDate.of(2025, 1, 10)
            val start = LocalDate.of(2025, 1, 10)
            val end = LocalDate.of(2025, 1, 20)

            // when & then
            assertThat(date.isWithinRange(start, end)).isTrue()
        }

        @Test
        @DisplayName("isWithinRange: 종료일과 같은 경우 true 반환")
        fun isWithinRange_endDate_returnsTrue() {
            // given
            val date = LocalDate.of(2025, 1, 20)
            val start = LocalDate.of(2025, 1, 10)
            val end = LocalDate.of(2025, 1, 20)

            // when & then
            assertThat(date.isWithinRange(start, end)).isTrue()
        }

        @Test
        @DisplayName("isWithinRange: 범위 밖 날짜인 경우 false 반환")
        fun isWithinRange_outsideRange_returnsFalse() {
            // given
            val date = LocalDate.of(2025, 1, 5)
            val start = LocalDate.of(2025, 1, 10)
            val end = LocalDate.of(2025, 1, 20)

            // when & then
            assertThat(date.isWithinRange(start, end)).isFalse()
        }

        @Test
        @DisplayName("isWithinPrescriptionPeriod: 처방 기간 내인 경우 true 반환")
        fun isWithinPrescriptionPeriod_withinPeriod_returnsTrue() {
            // given
            val date = LocalDate.of(2025, 1, 15)
            val startDate = LocalDate.of(2025, 1, 10)
            val days = 30

            // when & then
            assertThat(date.isWithinPrescriptionPeriod(startDate, days)).isTrue()
        }

        @Test
        @DisplayName("isWithinPrescriptionPeriod: 처방 기간 밖인 경우 false 반환")
        fun isWithinPrescriptionPeriod_outsidePeriod_returnsFalse() {
            // given
            val date = LocalDate.of(2025, 3, 1)
            val startDate = LocalDate.of(2025, 1, 10)
            val days = 30

            // when & then
            assertThat(date.isWithinPrescriptionPeriod(startDate, days)).isFalse()
        }
    }
}
