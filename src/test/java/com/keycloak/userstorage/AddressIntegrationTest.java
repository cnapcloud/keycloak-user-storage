package com.keycloak.userstorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * 통합 테스트 — `/user/{userId}/addresses` 신규 엔드포인트 (2026-09-11-address-management)
 *
 * T-001: POST 주소 등록 — happy path + 1:N + 무제한 (AC-001, AC-002, AC-003, AC-012)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AddressIntegrationTest {

    private static final String SAMPLE_POSTAL_CODE = "06236";
    private static final String SAMPLE_ROAD_ADDRESS = "서울특별시 강남구 테헤란로 123";
    private static final String SAMPLE_DETAIL_ADDRESS = "10층 1001호";
    private static final String SAMPLE_ADDRESS_BODY = String.format(
            "{\"postalCode\": \"%s\", \"roadAddress\": \"%s\", \"detailAddress\": \"%s\"}",
            SAMPLE_POSTAL_CODE, SAMPLE_ROAD_ADDRESS, SAMPLE_DETAIL_ADDRESS);

    @Autowired
    private TestRestTemplate rest;

    @Test
    @Tag("AC-003")
    @DisplayName("T-001: given an existing user, when POST /user/{userId}/addresses is submitted, "
            + "then the system creates the address and returns 201 with its fields")
    void createAddress_existingUser_returns201WithAddressFields() {
        String userId = createTempUser("address-test-user-1");
        try {
            ResponseEntity<Map<String, Object>> response = rest.exchange(
                    "/user/" + userId + "/addresses", HttpMethod.POST, jsonEntity(SAMPLE_ADDRESS_BODY),
                    new ParameterizedTypeReference<>() {});

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            Map<String, Object> address = response.getBody();
            assertNotNull(address);

            assertNotNull(address.get("id"), "id 필드 필수");
            assertTrue(address.get("id") instanceof String, "id는 문자열이어야 함");
            assertEquals(userId, address.get("userId"));
            assertEquals(SAMPLE_POSTAL_CODE, address.get("postalCode"));
            assertEquals(SAMPLE_ROAD_ADDRESS, address.get("roadAddress"));
            assertEquals(SAMPLE_DETAIL_ADDRESS, address.get("detailAddress"));
        } finally {
            rest.delete("/user/" + userId);
        }
    }

    @Test
    @Tag("AC-001")
    @DisplayName("T-001: given an existing user, when POST /user/{userId}/addresses is submitted twice, "
            + "then both addresses are created (1:N, no limit — AC-012)")
    void createAddress_sameUserTwice_bothSucceed() {
        String userId = createTempUser("address-test-user-2");
        try {
            ResponseEntity<Map<String, Object>> first = rest.exchange(
                    "/user/" + userId + "/addresses", HttpMethod.POST, jsonEntity(SAMPLE_ADDRESS_BODY),
                    new ParameterizedTypeReference<>() {});
            ResponseEntity<Map<String, Object>> second = rest.exchange(
                    "/user/" + userId + "/addresses", HttpMethod.POST, jsonEntity(SAMPLE_ADDRESS_BODY),
                    new ParameterizedTypeReference<>() {});

            assertEquals(HttpStatus.CREATED, first.getStatusCode(), "첫 번째 주소 등록 성공");
            assertEquals(HttpStatus.CREATED, second.getStatusCode(), "두 번째 주소도 제한 없이 등록 성공");

            String firstId = (String) first.getBody().get("id");
            String secondId = (String) second.getBody().get("id");
            assertNotNull(firstId);
            assertNotNull(secondId);
            assertTrue(!firstId.equals(secondId), "두 주소는 서로 다른 id를 가져야 함");
        } finally {
            rest.delete("/user/" + userId);
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private HttpEntity<String> jsonEntity(String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(json, headers);
    }

    private String createTempUser(String username) {
        String body = String.format("{\"username\": \"%s\", \"email\": \"%s@kind.internal\"}", username, username);
        ResponseEntity<Map<String, String>> response = rest.exchange(
                "/user", HttpMethod.POST, jsonEntity(body),
                new ParameterizedTypeReference<>() {});
        return response.getBody().get("id");
    }
}
