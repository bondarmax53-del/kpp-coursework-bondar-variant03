package ua.nupp.maintenance.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/** Єдиний налаштований ObjectMapper для всього сервера. */
public final class Json {
    public static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())                       // LocalDate / LocalDateTime
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)    // дати як "2026-11-24T10:00:00"
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private Json() {
    }
}
