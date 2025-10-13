package com.Hamalog.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;

/**
 * SimpleCalculator 클래스에 대한 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SimpleCalculator Tests")
class SimpleCalculatorTest {

    private SimpleCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new SimpleCalculator();
    }

    @Test
    @DisplayName("Should add two positive numbers correctly")
    void add_PositiveNumbers_ReturnsCorrectSum() {
        // given
        double a = 5.0;
        double b = 3.0;

        // when
        double result = calculator.add(a, b);

        // then
        assertThat(result).isEqualTo(8.0);
    }

    @Test
    @DisplayName("Should add negative numbers correctly")
    void add_NegativeNumbers_ReturnsCorrectSum() {
        // given
        double a = -5.0;
        double b = -3.0;

        // when
        double result = calculator.add(a, b);

        // then
        assertThat(result).isEqualTo(-8.0);
    }

    @Test
    @DisplayName("Should add zero correctly")
    void add_WithZero_ReturnsCorrectSum() {
        // given
        double a = 5.0;
        double b = 0.0;

        // when
        double result = calculator.add(a, b);

        // then
        assertThat(result).isEqualTo(5.0);
    }

    @Test
    @DisplayName("Should subtract two positive numbers correctly")
    void subtract_PositiveNumbers_ReturnsCorrectDifference() {
        // given
        double a = 10.0;
        double b = 3.0;

        // when
        double result = calculator.subtract(a, b);

        // then
        assertThat(result).isEqualTo(7.0);
    }

    @Test
    @DisplayName("Should subtract resulting in negative number")
    void subtract_ResultingInNegative_ReturnsCorrectDifference() {
        // given
        double a = 3.0;
        double b = 10.0;

        // when
        double result = calculator.subtract(a, b);

        // then
        assertThat(result).isEqualTo(-7.0);
    }

    @Test
    @DisplayName("Should multiply two positive numbers correctly")
    void multiply_PositiveNumbers_ReturnsCorrectProduct() {
        // given
        double a = 4.0;
        double b = 5.0;

        // when
        double result = calculator.multiply(a, b);

        // then
        assertThat(result).isEqualTo(20.0);
    }

    @Test
    @DisplayName("Should multiply by zero correctly")
    void multiply_ByZero_ReturnsZero() {
        // given
        double a = 4.0;
        double b = 0.0;

        // when
        double result = calculator.multiply(a, b);

        // then
        assertThat(result).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should multiply negative numbers correctly")
    void multiply_NegativeNumbers_ReturnsPositiveProduct() {
        // given
        double a = -4.0;
        double b = -5.0;

        // when
        double result = calculator.multiply(a, b);

        // then
        assertThat(result).isEqualTo(20.0);
    }

    @Test
    @DisplayName("Should divide two positive numbers correctly")
    void divide_PositiveNumbers_ReturnsCorrectQuotient() {
        // given
        double a = 10.0;
        double b = 2.0;

        // when
        double result = calculator.divide(a, b);

        // then
        assertThat(result).isEqualTo(5.0);
    }

    @Test
    @DisplayName("Should throw exception when dividing by zero")
    void divide_ByZero_ThrowsIllegalArgumentException() {
        // given
        double a = 10.0;
        double b = 0.0;

        // when & then
        assertThatThrownBy(() -> calculator.divide(a, b))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("0으로 나눌 수 없습니다.");
    }

    @Test
    @DisplayName("Should divide resulting in decimal correctly")
    void divide_ResultingInDecimal_ReturnsCorrectQuotient() {
        // given
        double a = 7.0;
        double b = 2.0;

        // when
        double result = calculator.divide(a, b);

        // then
        assertThat(result).isEqualTo(3.5);
    }

    @Test
    @DisplayName("Should calculate square root of positive number correctly")
    void sqrt_PositiveNumber_ReturnsCorrectSquareRoot() {
        // given
        double a = 25.0;

        // when
        double result = calculator.sqrt(a);

        // then
        assertThat(result).isEqualTo(5.0);
    }

    @Test
    @DisplayName("Should calculate square root of zero correctly")
    void sqrt_Zero_ReturnsZero() {
        // given
        double a = 0.0;

        // when
        double result = calculator.sqrt(a);

        // then
        assertThat(result).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should throw exception when calculating square root of negative number")
    void sqrt_NegativeNumber_ThrowsIllegalArgumentException() {
        // given
        double a = -4.0;

        // when & then
        assertThatThrownBy(() -> calculator.sqrt(a))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("음수의 제곱근을 계산할 수 없습니다.");
    }

    @Test
    @DisplayName("Should calculate power correctly with positive exponent")
    void power_PositiveExponent_ReturnsCorrectPower() {
        // given
        double base = 2.0;
        double exponent = 3.0;

        // when
        double result = calculator.power(base, exponent);

        // then
        assertThat(result).isEqualTo(8.0);
    }

    @Test
    @DisplayName("Should calculate power correctly with zero exponent")
    void power_ZeroExponent_ReturnsOne() {
        // given
        double base = 5.0;
        double exponent = 0.0;

        // when
        double result = calculator.power(base, exponent);

        // then
        assertThat(result).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should calculate power correctly with negative exponent")
    void power_NegativeExponent_ReturnsCorrectPower() {
        // given
        double base = 2.0;
        double exponent = -2.0;

        // when
        double result = calculator.power(base, exponent);

        // then
        assertThat(result).isEqualTo(0.25);
    }

    @Test
    @DisplayName("Should calculate absolute value of positive number correctly")
    void abs_PositiveNumber_ReturnsPositiveValue() {
        // given
        double a = 5.0;

        // when
        double result = calculator.abs(a);

        // then
        assertThat(result).isEqualTo(5.0);
    }

    @Test
    @DisplayName("Should calculate absolute value of negative number correctly")
    void abs_NegativeNumber_ReturnsPositiveValue() {
        // given
        double a = -5.0;

        // when
        double result = calculator.abs(a);

        // then
        assertThat(result).isEqualTo(5.0);
    }

    @Test
    @DisplayName("Should calculate absolute value of zero correctly")
    void abs_Zero_ReturnsZero() {
        // given
        double a = 0.0;

        // when
        double result = calculator.abs(a);

        // then
        assertThat(result).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should handle floating point precision correctly")
    void operations_FloatingPointPrecision_HandledCorrectly() {
        // given
        double a = 0.1;
        double b = 0.2;

        // when
        double result = calculator.add(a, b);

        // then
        assertThat(result).isCloseTo(0.3, within(0.0001));
    }

    @Test
    @DisplayName("Should handle very large numbers correctly")
    void operations_LargeNumbers_HandledCorrectly() {
        // given
        double a = Double.MAX_VALUE / 2;
        double b = Double.MAX_VALUE / 2;

        // when
        double result = calculator.add(a, b);

        // then
        assertThat(result).isEqualTo(Double.MAX_VALUE);
    }

    @Test
    @DisplayName("Should handle very small numbers correctly")
    void operations_SmallNumbers_HandledCorrectly() {
        // given
        double a = Double.MIN_VALUE;
        double b = Double.MIN_VALUE;

        // when
        double result = calculator.add(a, b);

        // then
        assertThat(result).isEqualTo(Double.MIN_VALUE * 2);
    }
}