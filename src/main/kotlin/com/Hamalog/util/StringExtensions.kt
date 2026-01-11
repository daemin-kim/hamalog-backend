package com.Hamalog.util

/**
 * String 확장 함수 모음
 */

/**
 * 전화번호 형식으로 변환 (01012345678 -> 010-1234-5678)
 */
fun String.toPhoneFormat(): String = this.replace(Regex("(\\d{3})(\\d{4})(\\d{4})"), "$1-$2-$3")

/**
 * 전화번호 하이픈 제거 (010-1234-5678 -> 01012345678)
 */
fun String.removePhoneHyphens(): String = this.replace("-", "")

/**
 * 마스킹 처리 (중간 부분을 *로 대체)
 * @param visiblePrefix 앞에서 보여줄 글자 수
 * @param visibleSuffix 뒤에서 보여줄 글자 수
 */
fun String.mask(visiblePrefix: Int = 3, visibleSuffix: Int = 4): String {
    if (this.length <= visiblePrefix + visibleSuffix) return this

    val prefix = this.take(visiblePrefix)
    val suffix = this.takeLast(visibleSuffix)
    val masked = "*".repeat(this.length - visiblePrefix - visibleSuffix)

    return "$prefix$masked$suffix"
}

/**
 * 이메일 마스킹 (example@domain.com -> exa***@domain.com)
 */
fun String.maskEmail(): String {
    val parts = this.split("@")
    if (parts.size != 2) return this

    val localPart = parts[0]
    val domain = parts[1]

    val maskedLocal = if (localPart.length <= 3) {
        localPart
    } else {
        localPart.take(3) + "*".repeat(localPart.length - 3)
    }

    return "$maskedLocal@$domain"
}

/**
 * null이거나 빈 문자열이면 기본값 반환
 */
fun String?.orDefault(default: String = ""): String = if (this.isNullOrBlank()) default else this

/**
 * 문자열이 유효한 이메일 형식인지 확인
 */
fun String.isValidEmail(): Boolean {
    val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
    return this.matches(emailRegex)
}

/**
 * 문자열이 유효한 한국 휴대폰 번호 형식인지 확인 (하이픈 없이)
 */
fun String.isValidKoreanPhoneNumber(): Boolean {
    val phoneRegex = Regex("^010\\d{8}$")
    return this.matches(phoneRegex)
}
