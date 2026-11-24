package ua.nupp.maintenance.service;

/** Прості перевірки вхідних даних, спільні для сервісів. */
final class Validators {
    private Validators() {
    }

    static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("Поле «" + field + "» є обов'язковим");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new ValidationException("Поле «" + field + "» довше за " + maxLength + " символів");
        }
        return trimmed;
    }

    static String optionalText(String value, int maxLength, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return requireText(value, field, maxLength);
    }

    static <T> T requireNotNull(T value, String field) {
        if (value == null) {
            throw new ValidationException("Поле «" + field + "» є обов'язковим");
        }
        return value;
    }
}
