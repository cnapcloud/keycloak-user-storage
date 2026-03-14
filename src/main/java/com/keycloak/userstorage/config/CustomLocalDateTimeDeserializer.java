package com.keycloak.userstorage.config;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * LocalDateTime 역직렬화 — 여러 ISO 8601 변형을 허용.
 *
 * <p>처리 순서:
 * <ol>
 *   <li>{@code yyyy-MM-dd'T'HH:mm:ss} — 백엔드 표준 포맷
 *   <li>{@code yyyy-MM-dd'T'HH:mm:ss.SSS} — 밀리초 포함
 *   <li>OffsetDateTime (timezone 포함, 예: {@code 2026-03-10T09:44:44.000+00:00}) — USP 전송 시
 *       timezone을 버리고 LocalDateTime으로 변환
 * </ol>
 */
public class CustomLocalDateTimeDeserializer extends StdDeserializer<LocalDateTime> {

    private static final DateTimeFormatter STANDARD = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter WITH_MILLIS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    public CustomLocalDateTimeDeserializer() {
        super(LocalDateTime.class);
    }

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
        String text = p.getText().trim();

        try {
            return LocalDateTime.parse(text, STANDARD);
        } catch (DateTimeParseException ignored) {}

        try {
            return LocalDateTime.parse(text, WITH_MILLIS);
        } catch (DateTimeParseException ignored) {}

        // timezone 포함 형식 (USP의 Timestamp → JSON): timezone을 버리고 변환
        return OffsetDateTime.parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime();
    }
}
