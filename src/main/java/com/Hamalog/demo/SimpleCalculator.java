package com.Hamalog.demo;

import lombok.extern.slf4j.Slf4j;

/**
 * 간단한 계산기 데모 클래스
 * 기본적인 산술 연산을 제공합니다.
 */
@Slf4j
public class SimpleCalculator {

    /**
     * 두 수를 더합니다.
     *
     * @param a 첫 번째 수
     * @param b 두 번째 수
     * @return 합계
     */
    public double add(double a, double b) {
        log.debug("Adding {} and {}", a, b);
        return a + b;
    }

    /**
     * 두 수를 뺍니다.
     *
     * @param a 첫 번째 수
     * @param b 두 번째 수
     * @return 차이
     */
    public double subtract(double a, double b) {
        log.debug("Subtracting {} from {}", b, a);
        return a - b;
    }

    /**
     * 두 수를 곱합니다.
     *
     * @param a 첫 번째 수
     * @param b 두 번째 수
     * @return 곱
     */
    public double multiply(double a, double b) {
        log.debug("Multiplying {} and {}", a, b);
        return a * b;
    }

    /**
     * 두 수를 나눕니다.
     *
     * @param a 첫 번째 수 (피제수)
     * @param b 두 번째 수 (제수)
     * @return 몫
     * @throws IllegalArgumentException 제수가 0인 경우
     */
    public double divide(double a, double b) {
        log.debug("Dividing {} by {}", a, b);
        if (b == 0) {
            log.error("Division by zero attempted: {} / {}", a, b);
            throw new IllegalArgumentException("0으로 나눌 수 없습니다.");
        }
        return a / b;
    }

    /**
     * 제곱근을 계산합니다.
     *
     * @param a 제곱근을 구할 수
     * @return 제곱근
     * @throws IllegalArgumentException 음수인 경우
     */
    public double sqrt(double a) {
        log.debug("Calculating square root of {}", a);
        if (a < 0) {
            log.error("Square root of negative number attempted: {}", a);
            throw new IllegalArgumentException("음수의 제곱근을 계산할 수 없습니다.");
        }
        return Math.sqrt(a);
    }

    /**
     * 거듭제곱을 계산합니다.
     *
     * @param base 밑
     * @param exponent 지수
     * @return 거듭제곱 결과
     */
    public double power(double base, double exponent) {
        log.debug("Calculating {} raised to the power of {}", base, exponent);
        return Math.pow(base, exponent);
    }

    /**
     * 절댓값을 계산합니다.
     *
     * @param a 절댓값을 구할 수
     * @return 절댓값
     */
    public double abs(double a) {
        log.debug("Calculating absolute value of {}", a);
        return Math.abs(a);
    }
}