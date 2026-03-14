package com.keycloak.userstorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

/**
 * 통합 테스트 — 실서버 기동 후 seed data(user-data.json, credential-data.json) 기준으로 USP API 검증
 *
 * 검증 범위:
 *   User   : GET /count/all, GET /search, GET /count, GET /{id}, POST, DELETE /{id}, PATCH /{id}/attributes
 *   Credential: GET /credential/{id}
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserStorageIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @BeforeAll
    static void enablePatchSupport(@Autowired TestRestTemplate restTemplate) {
        // HttpURLConnection은 PATCH 미지원 → Apache HttpClient5로 교체
        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(HttpClients.createDefault());
        restTemplate.getRestTemplate().setRequestFactory(factory);
    }

    // =========================================================================
    // GET /user/count/all
    // =========================================================================

    @Test
    @Order(1)
    void countAll_returnsSeededUserCount() {
        Integer count = rest.getForObject("/user/count/all", Integer.class);
        assertEquals(17, count, "seed data 17명 기준");
    }

    // =========================================================================
    // GET /user/search
    // =========================================================================

    @Test
    @Order(2)
    @SuppressWarnings("unchecked")
    void search_byUsername_returnsMatchWithRequiredFields() {
        ResponseEntity<List<Map<String, Object>>> response = rest.exchange(
                "/user/search?username=admin", HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {});

        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Map<String, Object>> users = response.getBody();
        assertNotNull(users);
        assertEquals(1, users.size());

        Map<String, Object> user = users.get(0);
        assertEquals("admin", user.get("username"));
        assertNotNull(user.get("id"), "id 필드 필수");
        assertTrue(((String) user.get("id")).startsWith("u-"), "id 포맷: u-XXXXXXXX");
        assertNotNull(user.get("attributes"), "attributes는 절대 null 이면 안 됨");
    }

    @Test
    @Order(3)
    void search_noMatch_returnsEmptyArray_not404() {
        ResponseEntity<List<Map<String, Object>>> response = rest.exchange(
                "/user/search?username=zzz_nobody_999", HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {});

        assertEquals(HttpStatus.OK, response.getStatusCode(), "결과 없어도 200 반환 (404 금지)");
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty(), "일치 없으면 빈 배열 반환");
    }

    @Test
    @Order(4)
    void search_withPagination_returnsBoundedResults() {
        ResponseEntity<List<Map<String, Object>>> response = rest.exchange(
                "/user/search?first=0&max=5", HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {});

        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Map<String, Object>> users = response.getBody();
        assertNotNull(users);
        assertTrue(users.size() <= 5, "max=5 이므로 최대 5개");
    }

    // =========================================================================
    // GET /user/count
    // =========================================================================

    @Test
    @Order(5)
    void count_withFilter_returnsMatchingCount() {
        Long count = rest.getForObject("/user/count?username=admin", Long.class);
        assertEquals(1L, count);
    }

    @Test
    @Order(6)
    void count_noMatch_returnsZero() {
        Long count = rest.getForObject("/user/count?username=zzz_nobody_999", Long.class);
        assertEquals(0L, count);
    }

    // =========================================================================
    // GET /user/{id}
    // =========================================================================

    @Test
    @Order(7)
    @SuppressWarnings("unchecked")
    void getById_existingUser_returnsFullUserWithAttributes() {
        String id = getAdminId();

        ResponseEntity<Map<String, Object>> response = rest.exchange(
                "/user/" + id, HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {});

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> user = response.getBody();
        assertNotNull(user);

        // 기본 필드
        assertEquals(id, user.get("id"));
        assertEquals("admin", user.get("username"));
        assertNotNull(user.get("email"));
        assertNotNull(user.get("enabled"));
        assertNotNull(user.get("emailVerified"));

        // createdDate ISO 8601 T 구분자
        String createdDate = (String) user.get("createdDate");
        assertNotNull(createdDate, "createdDate 필드 필수");
        assertTrue(createdDate.contains("T"), "createdDate 포맷: yyyy-MM-ddTHH:mm:ss");

        // attributes
        Map<String, String> attrs = (Map<String, String>) user.get("attributes");
        assertNotNull(attrs, "attributes는 null 이면 안 됨");
        assertEquals("ACTIVE", attrs.get("dormantStatus"));
        assertNotNull(attrs.get("phoneNumber"));
        assertNotNull(attrs.get("otpMethod"));
    }

    @Test
    @Order(8)
    void getById_notFound_returns404() {
        ResponseEntity<String> response = rest.getForEntity("/user/u-notexist0", String.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // =========================================================================
    // POST /user
    // =========================================================================

    @Test
    @Order(9)
    @SuppressWarnings("unchecked")
    void createUser_success_returns201WithIdUsernameEmail() {
        String body = """
                {
                  "username": "integration-test-user",
                  "email": "it@kind.internal",
                  "enabled": true,
                  "emailVerified": false,
                  "attributes": {"phoneNumber": "010-0000-0000"}
                }
                """;

        ResponseEntity<Map<String, String>> response = rest.exchange(
                "/user", HttpMethod.POST, jsonEntity(body),
                new ParameterizedTypeReference<>() {});

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Map<String, String> result = response.getBody();
        assertNotNull(result);
        assertTrue(result.get("id").startsWith("u-"), "id 포맷: u-XXXXXXXX");
        assertEquals("integration-test-user", result.get("username"));
        assertEquals("it@kind.internal", result.get("email"));

        // 정리
        rest.delete("/user/" + result.get("id"));
    }

    @Test
    @Order(10)
    void createUser_attributesEmpty_returns201() {
        String body = """
                {"username": "no-attrs-user", "email": "noattr@kind.internal"}
                """;

        ResponseEntity<Map<String, String>> response = rest.exchange(
                "/user", HttpMethod.POST, jsonEntity(body),
                new ParameterizedTypeReference<>() {});

        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        // 생성된 사용자 attributes → 빈 객체
        String newId = response.getBody().get("id");
        ResponseEntity<Map<String, Object>> getResponse = rest.exchange(
                "/user/" + newId, HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {});
        assertNotNull(getResponse.getBody().get("attributes"), "attributes 빈 객체여야 함 (null 금지)");

        // 정리
        rest.delete("/user/" + newId);
    }

    @Test
    @Order(11)
    void createUser_duplicateUsername_returns409() {
        String body = """
                {"username": "admin", "email": "another@kind.internal"}
                """;

        ResponseEntity<String> response = rest.exchange(
                "/user", HttpMethod.POST, jsonEntity(body), String.class);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    // =========================================================================
    // DELETE /user/{id}
    // =========================================================================

    @Test
    @Order(12)
    void deleteUser_success_returns204AndUserGone() {
        // 삭제용 사용자 생성
        String body = """
                {"username": "delete-target-user", "email": "del@kind.internal"}
                """;
        ResponseEntity<Map<String, String>> created = rest.exchange(
                "/user", HttpMethod.POST, jsonEntity(body),
                new ParameterizedTypeReference<>() {});
        String targetId = created.getBody().get("id");

        // DELETE
        ResponseEntity<Void> deleteResponse = rest.exchange(
                "/user/" + targetId, HttpMethod.DELETE, null, Void.class);
        assertEquals(HttpStatus.NO_CONTENT, deleteResponse.getStatusCode());

        // 삭제 후 조회 → 404
        ResponseEntity<String> getResponse = rest.getForEntity("/user/" + targetId, String.class);
        assertEquals(HttpStatus.NOT_FOUND, getResponse.getStatusCode());

        // count 복원 확인
        Integer count = rest.getForObject("/user/count/all", Integer.class);
        assertEquals(17, count, "삭제 후 다시 17명");
    }

    @Test
    @Order(13)
    void deleteUser_notFound_returns404() {
        ResponseEntity<String> response = rest.exchange(
                "/user/u-notexist0", HttpMethod.DELETE, null, String.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // =========================================================================
    // PATCH /user/{id}/attributes
    // =========================================================================

    @Test
    @Order(14)
    @SuppressWarnings("unchecked")
    void patchAttributes_addsNewKey_returns204() {
        String id = getAdminId();

        ResponseEntity<Void> patchResponse = rest.exchange(
                "/user/" + id + "/attributes", HttpMethod.PATCH,
                jsonEntity("{\"lastLoginDate\":\"2026-03-09T12:00:00\"}"),
                Void.class);
        assertEquals(HttpStatus.NO_CONTENT, patchResponse.getStatusCode());

        Map<String, Object> user = rest.exchange("/user/" + id, HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, Object>>() {}).getBody();
        Map<String, String> attrs = (Map<String, String>) user.get("attributes");
        assertEquals("2026-03-09T12:00:00", attrs.get("lastLoginDate"));
    }

    @Test
    @Order(15)
    @SuppressWarnings("unchecked")
    void patchAttributes_nullValueRemovesKey() {
        String id = getAdminId();

        // null → 키 제거
        ResponseEntity<Void> patchResponse = rest.exchange(
                "/user/" + id + "/attributes", HttpMethod.PATCH,
                jsonEntity("{\"lastLoginDate\":null}"),
                Void.class);
        assertEquals(HttpStatus.NO_CONTENT, patchResponse.getStatusCode());

        Map<String, Object> user = rest.exchange("/user/" + id, HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, Object>>() {}).getBody();
        Map<String, String> attrs = (Map<String, String>) user.get("attributes");
        assertFalse(attrs.containsKey("lastLoginDate"), "null 로 PATCH한 키는 제거되어야 함");
    }

    @Test
    @Order(16)
    @SuppressWarnings("unchecked")
    void patchAttributes_unincludedKeyPreserved() {
        String id = getAdminId();

        // testKey만 추가 — phoneNumber는 미포함
        rest.exchange("/user/" + id + "/attributes", HttpMethod.PATCH,
                jsonEntity("{\"testKey\":\"testValue\"}"), Void.class);

        Map<String, Object> user = rest.exchange("/user/" + id, HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, Object>>() {}).getBody();
        Map<String, String> attrs = (Map<String, String>) user.get("attributes");

        assertNotNull(attrs.get("phoneNumber"), "PATCH 미포함 키(phoneNumber)는 유지되어야 함");
        assertEquals("testValue", attrs.get("testKey"), "새 키가 추가되어야 함");

        // 정리
        rest.exchange("/user/" + id + "/attributes", HttpMethod.PATCH,
                jsonEntity("{\"testKey\":null}"), Void.class);
    }

    @Test
    @Order(17)
    void patchAttributes_notFound_returns404() {
        ResponseEntity<String> response = rest.exchange(
                "/user/u-notexist0/attributes", HttpMethod.PATCH,
                jsonEntity("{\"key\":\"value\"}"), String.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // =========================================================================
    // GET /credential/{id}
    // =========================================================================

    @Test
    @Order(18)
    @SuppressWarnings("unchecked")
    void getCredential_existingUser_returnsAllRequiredFields() {
        String userId = getAdminId();

        ResponseEntity<Map<String, Object>> response = rest.exchange(
                "/credential/" + userId, HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {});

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> cred = response.getBody();
        assertNotNull(cred);

        // USP 필수 필드
        assertNotNull(cred.get("value"), "value 필드 필수");
        assertNotNull(cred.get("salt"), "salt 필드 필수");
        assertNotNull(cred.get("algorithm"), "algorithm 필드 필수");
        assertNotNull(cred.get("iterations"), "iterations 필드 필수");
        assertNotNull(cred.get("additionParameters"), "additionParameters 필드 필수");

        // additionParameters는 JSON 문자열 (파싱된 객체가 아님)
        assertTrue(cred.get("additionParameters") instanceof String,
                "additionParameters는 JSON 문자열이어야 함 (파싱 금지)");

        // 평문 비밀번호 포함 금지
        String valueStr = (String) cred.get("value");
        assertFalse(valueStr.isEmpty(), "value가 비어있으면 안 됨");
    }

    @Test
    @Order(19)
    void getCredential_notFound_returns404() {
        ResponseEntity<String> response = rest.getForEntity("/credential/u-notexist0", String.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // =========================================================================
    // BL-09: 에러 포맷 통일 ({"error": "..."})
    // =========================================================================

    @Test
    @Order(20)
    void errorResponse_userNotFound_hasErrorField() {
        ResponseEntity<Map<String, Object>> response = rest.exchange(
                "/user/u-notexist0", HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {});

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().get("error"), "에러 응답에 'error' 필드 필수");
        assertEquals("user not found", response.getBody().get("error"));
    }

    @Test
    @Order(21)
    void errorResponse_credentialNotFound_hasErrorField() {
        ResponseEntity<Map<String, Object>> response = rest.exchange(
                "/credential/u-notexist0", HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {});

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("credential not found", response.getBody().get("error"));
    }

    // =========================================================================
    // BL-09: PUT /credential/{id} → 204 / 404
    // =========================================================================

    @Test
    @Order(22)
    void updateCredential_success_returns204() {
        String userId = getAdminId();
        String body = """
                {
                  "value": "newHashValue==",
                  "salt": "newSalt==",
                  "algorithm": "argon2",
                  "iterations": 5,
                  "type": "password",
                  "additionParameters": "{\\"hashLength\\":[\\"32\\"]}"
                }
                """;

        ResponseEntity<Void> response = rest.exchange(
                "/credential/" + userId, HttpMethod.PUT, jsonEntity(body), Void.class);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    @Order(23)
    void updateCredential_notFound_returns404WithErrorField() {
        String body = """
                {"value": "x", "salt": "y", "algorithm": "argon2", "iterations": 1,
                 "type": "password", "additionParameters": "{}"}
                """;

        ResponseEntity<Map<String, Object>> response = rest.exchange(
                "/credential/u-notexist0", HttpMethod.PUT, jsonEntity(body),
                new ParameterizedTypeReference<>() {});

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("credential not found", response.getBody().get("error"));
    }

    // =========================================================================
    // BL-09: DELETE /credential/{id} → 404 처리 추가
    // =========================================================================

    @Test
    @Order(24)
    void deleteCredential_notFound_returns404() {
        ResponseEntity<Map<String, Object>> response = rest.exchange(
                "/credential/u-notexist0", HttpMethod.DELETE, null,
                new ParameterizedTypeReference<>() {});

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("credential not found", response.getBody().get("error"));
    }

    // =========================================================================
    // BL-09: 스펙 외 엔드포인트 제거 확인
    // =========================================================================

    @Test
    @Order(25)
    void getCredentialAll_endpointRemoved_returnsNot2xx() {
        ResponseEntity<String> response = rest.getForEntity("/credential", String.class);
        assertFalse(response.getStatusCode().is2xxSuccessful(),
                "GET /credential (전체 목록) 엔드포인트는 제거되어 2xx 아님");
    }

    @Test
    @Order(26)
    void postCredential_endpointRemoved_returnsNot2xx() {
        ResponseEntity<String> response = rest.exchange(
                "/credential", HttpMethod.POST, jsonEntity("{}"), String.class);
        assertFalse(response.getStatusCode().is2xxSuccessful(),
                "POST /credential 엔드포인트는 제거되어 2xx 아님");
    }

    // =========================================================================
    // BL-10: /user/search — attributes 키 검색 지원
    // =========================================================================

    @Test
    @Order(27)
    void search_byAttributeKey_dormantStatus_returnsAllUsers() {
        ResponseEntity<List<Map<String, Object>>> response = rest.exchange(
                "/user/search?dormantStatus=ACTIVE", HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {});

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(17, response.getBody().size(), "dormantStatus=ACTIVE 는 시드 17명 전원");
    }

    @Test
    @Order(28)
    void search_byAttributeKey_otpMethod_returnsAllUsers() {
        ResponseEntity<List<Map<String, Object>>> response = rest.exchange(
                "/user/search?otpMethod=SKIP", HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {});

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(17, response.getBody().size(), "otpMethod=SKIP 는 시드 17명 전원");
    }

    @Test
    @Order(29)
    void search_byAttributeKey_phoneNumber_returnsUser() {
        ResponseEntity<List<Map<String, Object>>> response = rest.exchange(
                "/user/search?phoneNumber=821012345601", HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {});

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size(), "phoneNumber=821012345601 는 시드 1명 (admin)");
    }

    @Test
    @Order(30)
    void search_combinedFieldAndAttribute_returnsIntersection() {
        // admin 은 otpMethod=SKIP → 1명
        ResponseEntity<List<Map<String, Object>>> response = rest.exchange(
                "/user/search?username=admin&otpMethod=SKIP", HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {});

        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Map<String, Object>> users = response.getBody();
        assertEquals(1, users.size());
        assertEquals("admin", users.get(0).get("username"));
    }

    @Test
    @Order(31)
    void count_byAttributeKey_otpMethod_returns17() {
        Long count = rest.getForObject("/user/count?otpMethod=SKIP", Long.class);
        assertEquals(17L, count, "otpMethod=SKIP 카운트 17명 전원");
    }

    @Test
    @Order(32)
    void search_existingField_unaffected_byAttributeChange() {
        // 기존 직접 필드 검색 동작 유지 확인
        ResponseEntity<List<Map<String, Object>>> response = rest.exchange(
                "/user/search?username=john", HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {});

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("john", response.getBody().get(0).get("username"));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private HttpEntity<String> jsonEntity(String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(json, headers);
    }

    @SuppressWarnings("unchecked")
    private String getAdminId() {
        ResponseEntity<List<Map<String, Object>>> response = rest.exchange(
                "/user/search?username=admin", HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {});
        return (String) response.getBody().get(0).get("id");
    }
}
