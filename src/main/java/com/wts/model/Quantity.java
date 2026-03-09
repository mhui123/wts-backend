package com.wts.model;

import java.math.BigDecimal;

public record Quantity(BigDecimal value) {

    public static Quantity zero() {
        return new Quantity(BigDecimal.ZERO);
    }

    public Quantity add(Quantity other) {
        return new Quantity(value.add(other.value));
    }

    public Quantity subtract(Quantity other) {
        return new Quantity(value.subtract(other.value));
    }

    public boolean isZero() {
        return value.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isNegative() {
        return value.compareTo(BigDecimal.ZERO) < 0;
    }

    public static Quantity of(BigDecimal qty) {
        return new Quantity(qty);
    }
}
