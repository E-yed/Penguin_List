package com.penguinlist.api.model;

public enum TodoDay {
    YESTERDAY,
    TODAY,
    TOMORROW;

    public static TodoDay from(String value) {
        try {
            return TodoDay.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("day must be one of: yesterday, today, tomorrow");
        }
    }
}
