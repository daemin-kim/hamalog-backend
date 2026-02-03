package com.Hamalog.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * StringExtensions.kt 단위 테스트
 */
@DisplayName("StringExtensions 테스트")
class StringExtensionsTest {

    @Nested
    @DisplayName("전화번호 변환")
    inner class PhoneNumber {

        @Test
        @DisplayName("toPhoneFormat: 하이픈 없는 전화번호를 형식화")
        fun toPhoneFormat_success() {
            // given
            val phone = "01012345678"

            // when
            val result = phone.toPhoneFormat()

            // then
            assertThat(result).isEqualTo("010-1234-5678")
        }

        @Test
        @DisplayName("removePhoneHyphens: 하이픈 제거")
        fun removePhoneHyphens_success() {
            // given
            val phone = "010-1234-5678"

            // when
            val result = phone.removePhoneHyphens()

            // then
            assertThat(result).isEqualTo("01012345678")
        }
    }

    @Nested
    @DisplayName("마스킹")
    inner class Masking {

        @Test
        @DisplayName("mask: 기본 마스킹 처리")
        fun mask_default_success() {
            // given
            val text = "01012345678"

            // when
            val result = text.mask()

            // then
            assertThat(result).isEqualTo("010****5678")
        }

        @Test
        @DisplayName("mask: 커스텀 마스킹 처리")
        fun mask_custom_success() {
            // given
            val text = "01012345678"

            // when
            val result = text.mask(visiblePrefix = 4, visibleSuffix = 2)

            // then
            assertThat(result).isEqualTo("0101*****78")
        }

        @Test
        @DisplayName("mask: 짧은 문자열은 그대로 반환")
        fun mask_shortString_returnsOriginal() {
            // given
            val text = "1234"

            // when
            val result = text.mask(visiblePrefix = 3, visibleSuffix = 4)

            // then
            assertThat(result).isEqualTo("1234")
        }

        @Test
        @DisplayName("maskEmail: 이메일 마스킹 처리")
        fun maskEmail_success() {
            // given
            val email = "example@domain.com"

            // when
            val result = email.maskEmail()

            // then
            assertThat(result).isEqualTo("exa****@domain.com")
        }

        @Test
        @DisplayName("maskEmail: 짧은 로컬파트는 그대로 유지")
        fun maskEmail_shortLocalPart_success() {
            // given
            val email = "abc@domain.com"

            // when
            val result = email.maskEmail()

            // then
            assertThat(result).isEqualTo("abc@domain.com")
        }

        @Test
        @DisplayName("maskEmail: 잘못된 형식은 그대로 반환")
        fun maskEmail_invalidFormat_returnsOriginal() {
            // given
            val invalidEmail = "invalid-email"

            // when
            val result = invalidEmail.maskEmail()

            // then
            assertThat(result).isEqualTo("invalid-email")
        }
    }

    @Nested
    @DisplayName("기본값 처리")
    inner class DefaultValue {

        @Test
        @DisplayName("orDefault: null인 경우 기본값 반환")
        fun orDefault_null_returnsDefault() {
            // given
            val nullString: String? = null

            // when
            val result = nullString.orDefault("기본값")

            // then
            assertThat(result).isEqualTo("기본값")
        }

        @Test
        @DisplayName("orDefault: 빈 문자열인 경우 기본값 반환")
        fun orDefault_empty_returnsDefault() {
            // given
            val emptyString: String? = ""

            // when
            val result = emptyString.orDefault("기본값")

            // then
            assertThat(result).isEqualTo("기본값")
        }

        @Test
        @DisplayName("orDefault: 공백만 있는 경우 기본값 반환")
        fun orDefault_blank_returnsDefault() {
            // given
            val blankString: String? = "   "

            // when
            val result = blankString.orDefault("기본값")

            // then
            assertThat(result).isEqualTo("기본값")
        }

        @Test
        @DisplayName("orDefault: 유효한 값인 경우 원본 반환")
        fun orDefault_validString_returnsOriginal() {
            // given
            val validString: String? = "유효한 값"

            // when
            val result = validString.orDefault("기본값")

            // then
            assertThat(result).isEqualTo("유효한 값")
        }
    }

    @Nested
    @DisplayName("유효성 검증")
    inner class Validation {

        @Test
        @DisplayName("isValidEmail: 유효한 이메일 형식")
        fun isValidEmail_validEmail_returnsTrue() {
            // given
            val email = "test@example.com"

            // when & then
            assertThat(email.isValidEmail()).isTrue()
        }

        @Test
        @DisplayName("isValidEmail: 특수문자 포함 이메일")
        fun isValidEmail_withSpecialChars_returnsTrue() {
            // given
            val email = "test+alias@example.com"

            // when & then
            assertThat(email.isValidEmail()).isTrue()
        }

        @Test
        @DisplayName("isValidEmail: 잘못된 이메일 형식")
        fun isValidEmail_invalidEmail_returnsFalse() {
            // given
            val invalidEmail = "invalid-email"

            // when & then
            assertThat(invalidEmail.isValidEmail()).isFalse()
        }

        @Test
        @DisplayName("isValidKoreanPhoneNumber: 유효한 한국 휴대폰 번호")
        fun isValidKoreanPhoneNumber_validNumber_returnsTrue() {
            // given
            val phone = "01012345678"

            // when & then
            assertThat(phone.isValidKoreanPhoneNumber()).isTrue()
        }

        @Test
        @DisplayName("isValidKoreanPhoneNumber: 잘못된 휴대폰 번호")
        fun isValidKoreanPhoneNumber_invalidNumber_returnsFalse() {
            // given
            val phone = "02012345678"

            // when & then
            assertThat(phone.isValidKoreanPhoneNumber()).isFalse()
        }

        @Test
        @DisplayName("isValidKoreanPhoneNumber: 하이픈 포함 시 false")
        fun isValidKoreanPhoneNumber_withHyphen_returnsFalse() {
            // given
            val phone = "010-1234-5678"

            // when & then
            assertThat(phone.isValidKoreanPhoneNumber()).isFalse()
        }
    }
}
