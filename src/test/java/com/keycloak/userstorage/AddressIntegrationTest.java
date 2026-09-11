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
 * T-002: POST 주소 등록 — 검증/사용자 없음 실패 경로 (AC-004, AC-005, AC-013)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AddressIntegrationTest {

    private static final String SAMPLE_POSTAL_CODE = "06236";
    private static final String SAMPLE_ROAD_ADDRESS = "서울특별시 강남구 테헤란로 123";
    private static final String SAMPLE_DETAIL_ADDRESS = "10층 1001호";
    private static final String SAMPLE_ADDRESS_BODY = String.format(
            "{\"postalCode\": \"%s\", \"roadAddress\": \"%s\", \"detailAddress\": \"%s\"}",
            SAMPLE_POSTAL_CODE, SAMPLE_ROAD_ADDRESS, SAMPLE_DETAIL_ADDRESS);
    private static final String INVALID_POSTAL_CODE = "1234";
    private static final String INVALID_POSTAL_CODE_BODY = String.format(
            "{\"postalCode\": \"%s\", \"roadAddress\": \"%s\", \"detailAddress\": \"%s\"}",
            INVALID_POSTAL_CODE, SAMPLE_ROAD_ADDRESS, SAMPLE_DETAIL_ADDRESS);
    private static final String BLANK_ROAD_ADDRESS = "   ";
    private static final String BLANK_ROAD_ADDRESS_BODY = String.format(
            "{\"postalCode\": \"%s\", \"roadAddress\": \"%s\", \"detailAddress\": \"%s\"}",
            SAMPLE_POSTAL_CODE, BLANK_ROAD_ADDRESS, SAMPLE_DETAIL_ADDRESS);
    private static final String NONEXISTENT_USER_ID = "u-00000000";
    private static final String NONEXISTENT_ADDRESS_ID = "addr-00000000";
    private static final String ADDRESS_NOT_FOUND_ERROR = "address not found";

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

    @Test
    @Tag("AC-004")
    @DisplayName("T-002: given a userId that does not exist, when POST /user/{userId}/addresses is submitted, "
            + "then the system responds with 404 and does not create an address")
    void createAddress_nonexistentUser_returns404() {
        ResponseEntity<Map<String, Object>> response = rest.exchange(
                "/user/" + NONEXISTENT_USER_ID + "/addresses", HttpMethod.POST, jsonEntity(SAMPLE_ADDRESS_BODY),
                new ParameterizedTypeReference<>() {});

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("user not found", response.getBody().get("error"));
    }

    @Test
    @Tag("AC-005")
    @DisplayName("T-002: given an existing user, when POST /user/{userId}/addresses is submitted with a postal code "
            + "that is not exactly 5 digits, then the system responds with 400 and does not create an address")
    void createAddress_invalidPostalCode_returns400() {
        String userId = createTempUser("address-test-user-3");
        try {
            ResponseEntity<Map<String, Object>> response = rest.exchange(
                    "/user/" + userId + "/addresses", HttpMethod.POST, jsonEntity(INVALID_POSTAL_CODE_BODY),
                    new ParameterizedTypeReference<>() {});

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("postal code must be exactly 5 digits", response.getBody().get("error"));
        } finally {
            rest.delete("/user/" + userId);
        }
    }

    @Test
    @Tag("AC-013")
    @DisplayName("T-002: given an existing user, when POST /user/{userId}/addresses is submitted with a blank road "
            + "address, then the system responds with 400 and does not create an address")
    void createAddress_blankRoadAddress_returns400() {
        String userId = createTempUser("address-test-user-4");
        try {
            ResponseEntity<Map<String, Object>> response = rest.exchange(
                    "/user/" + userId + "/addresses", HttpMethod.POST, jsonEntity(BLANK_ROAD_ADDRESS_BODY),
                    new ParameterizedTypeReference<>() {});

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("road address must not be blank", response.getBody().get("error"));
        } finally {
            rest.delete("/user/" + userId);
        }
    }

    @Test
    @Tag("AC-006")
    @DisplayName("T-003: given an address that exists and belongs to the user, when GET "
            + "/user/{userId}/addresses/{addressId} is submitted, then the system responds with 200 and "
            + "that address's id, user id, postal code, road address, and detail address")
    void getAddress_existingAddressOfOwningUser_returns200WithAddressFields() {
        String userId = createTempUser("address-test-user-5");
        try {
            String addressId = createTempAddress(userId, SAMPLE_ADDRESS_BODY);

            ResponseEntity<Map<String, Object>> response = rest.exchange(
                    "/user/" + userId + "/addresses/" + addressId, HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {});

            assertEquals(HttpStatus.OK, response.getStatusCode());
            Map<String, Object> address = response.getBody();
            assertNotNull(address);
            assertEquals(addressId, address.get("id"));
            assertEquals(userId, address.get("userId"));
            assertEquals(SAMPLE_POSTAL_CODE, address.get("postalCode"));
            assertEquals(SAMPLE_ROAD_ADDRESS, address.get("roadAddress"));
            assertEquals(SAMPLE_DETAIL_ADDRESS, address.get("detailAddress"));
        } finally {
            rest.delete("/user/" + userId);
        }
    }

    @Test
    @Tag("AC-010")
    @DisplayName("T-003: given an addressId that does not exist, when GET /user/{userId}/addresses/{addressId} "
            + "is submitted, then the system responds with 404 and leaves all address data unchanged")
    void getAddress_nonexistentAddressId_returns404() {
        String userId = createTempUser("address-test-user-6");
        try {
            ResponseEntity<Map<String, Object>> response = rest.exchange(
                    "/user/" + userId + "/addresses/" + NONEXISTENT_ADDRESS_ID, HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {});

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals(ADDRESS_NOT_FOUND_ERROR, response.getBody().get("error"));
        } finally {
            rest.delete("/user/" + userId);
        }
    }

    @Test
    @Tag("AC-014")
    @DisplayName("T-003: given an addressId that exists but belongs to a different user, when GET "
            + "/user/{userId}/addresses/{addressId} is submitted with the other user's id, then the system "
            + "responds with 404 and leaves all address data unchanged")
    void getAddress_addressBelongsToDifferentUser_returns404() {
        String userIdA = createTempUser("address-test-user-7a");
        String userIdB = createTempUser("address-test-user-7b");
        try {
            String addressIdOfA = createTempAddress(userIdA, SAMPLE_ADDRESS_BODY);

            ResponseEntity<Map<String, Object>> response = rest.exchange(
                    "/user/" + userIdB + "/addresses/" + addressIdOfA, HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {});

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals(ADDRESS_NOT_FOUND_ERROR, response.getBody().get("error"));
        } finally {
            rest.delete("/user/" + userIdA);
            rest.delete("/user/" + userIdB);
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

    private String createTempAddress(String userId, String addressBody) {
        ResponseEntity<Map<String, Object>> response = rest.exchange(
                "/user/" + userId + "/addresses", HttpMethod.POST, jsonEntity(addressBody),
                new ParameterizedTypeReference<>() {});
        return (String) response.getBody().get("id");
    }
}
