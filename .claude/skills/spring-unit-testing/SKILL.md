---
name: spring-unit-testing
description: Layer-isolated unit tests for Controller and Service — @WebMvcTest + @MockBean for Controller, plain Mockito for Service. Each layer mocks the layer(s) below it. Use in /build red step alongside spring-integration-testing.
---
# Unit testing (layer-isolated)

Companion to `spring-integration-testing`, not a replacement. Scope is Controller and
Service only — Repository has no isolated-unit-test convention yet (would be
`@DataJpaTest`, out of scope until a task needs it).

## Rule
Test one layer at a time. Mock everything below the layer under test — never call
into a real collaborator from a unit test.
- **Controller** under test → mock **Service**.
- **Service** under test → mock **Repository** (and any other injected repository/service dependency).

Do not write a test that exercises Controller through a real Service — that's a
2-layer slice, not a unit, and it duplicates what the `@SpringBootTest` integration
tests already verify end-to-end. If you need to confirm Controller and Service
actually wire together correctly, that's what the integration test is for.

## One AC, which layer(s)?
- Rule/branch decides the outcome → `XxxServiceTest` only.
- Only routing/response shape is at stake → `XxxControllerTest` only.
- Both (most ACs here) → both classes, same `@Tag("AC-NNN")`. Not a duplicate —
  Service test proves the rule fires, Controller test proves it reaches the client.

## Naming
`XxxControllerTest` / `XxxServiceTest` (or `XxxServiceImplTest`) — distinct from the
existing `XxxIntegrationTest` files so the two are never confused at a glance. Same
`@Tag("AC-NNN")` + `@DisplayName("<task-id>: given …, when …, then …")` convention as
`spring-integration-testing`. No new Gradle dependency or task: `spring-boot-starter-test`
already brings Mockito + `MockMvc`, and both test styles run under the existing
`./gradlew test` (the `unit` gate in `gradle-harness` already covers `src/test/**`
regardless of style).

## Package layout
General rule, not just for `Address`: a test class's package **mirrors the full package
path of the single class it targets, to whatever depth that class sits at**.
- `AddressControllerTest` → `src/test/java/.../controller/` (same subpackage as `AddressController`).
- `AddressServiceImplTest` → `src/test/java/.../service/` (same subpackage as `AddressServiceImpl`).
- A `@DataJpaTest` repository test, if one is ever written, would go under `.../repository/` the same way.
- Today `controller/` and `service/` are flat (no feature subpackages). If either ever
  grows one (e.g. `controller/address/AddressController.java`), the test follows it down
  to the same depth (`test/.../controller/address/AddressControllerTest.java`) — the rule
  is "match the production class's package exactly," not "one fixed level under
  controller/service."

A full-stack `@SpringBootTest` integration test doesn't target one class — it exercises
a whole request flow — so it doesn't belong in a layer subpackage. Any **new** one
(opt-in only, see below) goes under `.../integration/` instead:
`src/test/java/com/keycloak/userstorage/integration/XxxIntegrationTest.java`.

This is forward-only, same as the unit-test-by-default policy: the existing
`UserStorageIntegrationTest` and `AddressIntegrationTest` stay at the root package where
they already are — not moved retroactively, since that would touch already-committed,
`done` tasks outside any active `/build` task's `files_in_scope`. A future task that
explicitly needs a new integration test starts it in `integration/`; nothing already
merged gets reorganized to match.

## Controller: `@WebMvcTest` + `@MockBean`
Loads only the web layer (controller, `@RestControllerAdvice`, Jackson config) — no
real `Service`, no database. `@WebMvcTest` auto-includes `GlobalExceptionHandler`, so
the `{"error": "..."}` envelope still applies to mocked exceptions.

```java
@WebMvcTest(AddressController.class)
class AddressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AddressService addressService;

    @Test
    @Tag("AC-006")
    @DisplayName("T-003: given the service returns an address, when GET /user/{userId}/addresses/{addressId}, "
            + "then 200 with its fields")
    void getAddress_serviceReturnsAddress_returns200() throws Exception {
        Address address = new Address();
        address.setId("addr-3f9c1001");
        address.setUserId("u-092d66b8");
        address.setPostalCode("06236");
        address.setRoadAddress("서울특별시 강남구 테헤란로 123");

        when(addressService.getAddress("u-092d66b8", "addr-3f9c1001")).thenReturn(address);

        mockMvc.perform(get("/user/u-092d66b8/addresses/addr-3f9c1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("addr-3f9c1001"))
                .andExpect(jsonPath("$.postalCode").value("06236"));
    }

    @Test
    @Tag("AC-010")
    @DisplayName("T-003: given the service throws 404, when GET, then the controller returns 404 with the error body")
    void getAddress_serviceThrows404_returns404() throws Exception {
        when(addressService.getAddress(anyString(), anyString()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "address not found"));

        mockMvc.perform(get("/user/u-092d66b8/addresses/addr-00000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("address not found"));
    }
}
```

Assert only what the Controller owns: status code, response body shape, and that it
called Service with the right arguments (`verify(addressService).getAddress(userId, addressId)`
where the wiring itself is the point of the test). Do not re-assert Service business
rules here (e.g. why postal code validation fails) — those belong in the Service test.

## Service: plain Mockito
No Spring context at all — fastest tier.

```java
@ExtendWith(MockitoExtension.class)
class AddressServiceImplTest {

    @Mock
    private AddressRepository addressRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AddressServiceImpl addressService;

    @Test
    @Tag("AC-010")
    @DisplayName("T-003: given no address with that id for that user, when getAddress, then throws 404")
    void getAddress_notFound_throws404() {
        when(addressRepository.findByIdAndUserId("addr-00000000", "u-092d66b8"))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> addressService.getAddress("u-092d66b8", "addr-00000000"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("address not found", ex.getReason());
    }
}
```

Assert business rules here: validation branches, which exception/message fires for
which input, ordering of checks (e.g. "user not found" before "postal code invalid").
Don't assert HTTP status codes as the primary point — assert the `ResponseStatusException`'s
`getStatusCode()`/`getReason()` directly; the Controller test already confirms Spring
turns that into the right HTTP response.

## Relationship to `spring-integration-testing` — unit test is the default
This skill is the **default** for every AC in the red step. `spring-integration-testing`
(full-stack `@SpringBootTest(RANDOM_PORT)`) is **opt-in only** — write one only when the
user explicitly asks for it for that task, never automatically, not even for a
"happy path" AC or to double-check cross-layer wiring. If nobody asked for one, don't
write one.

Existing `*IntegrationTest` classes (`UserStorageIntegrationTest`, `AddressIntegrationTest`)
stay as they are — not retrofitted with unit tests retroactively. This default applies
going forward, from the task where it was decided onward.
