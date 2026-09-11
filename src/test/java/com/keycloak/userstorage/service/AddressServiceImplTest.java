package com.keycloak.userstorage.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.keycloak.userstorage.model.Address;
import com.keycloak.userstorage.repository.AddressRepository;
import com.keycloak.userstorage.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AddressServiceImplTest {

    private static final String EXISTING_USER_ID = "u-092d66b8";
    private static final String NONEXISTENT_USER_ID = "u-00000000";
    private static final String SAMPLE_POSTAL_CODE = "06236";
    private static final String SAMPLE_ROAD_ADDRESS = "서울특별시 강남구 테헤란로 123";

    @Mock
    private AddressRepository addressRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AddressServiceImpl addressService;

    @Test
    @Tag("AC-007")
    @DisplayName("T-004: given an existing user with two addresses, when getAddresses, "
            + "then returns both addresses belonging to that user")
    void getAddresses_existingUserWithAddresses_returnsAllAddresses() {
        Address first = new Address();
        first.setId("addr-3f9c1001");
        first.setUserId(EXISTING_USER_ID);
        first.setPostalCode(SAMPLE_POSTAL_CODE);
        first.setRoadAddress(SAMPLE_ROAD_ADDRESS);

        Address second = new Address();
        second.setId("addr-3f9c1002");
        second.setUserId(EXISTING_USER_ID);
        second.setPostalCode(SAMPLE_POSTAL_CODE);
        second.setRoadAddress(SAMPLE_ROAD_ADDRESS);

        when(userRepository.existsById(EXISTING_USER_ID)).thenReturn(true);
        when(addressRepository.findByUserId(EXISTING_USER_ID)).thenReturn(List.of(first, second));

        List<Address> result = addressService.getAddresses(EXISTING_USER_ID);

        assertEquals(2, result.size());
        assertTrue(result.contains(first));
        assertTrue(result.contains(second));
    }

    @Test
    @Tag("AC-007")
    @DisplayName("T-004: given an existing user with no addresses, when getAddresses, then returns an empty list")
    void getAddresses_existingUserWithNoAddresses_returnsEmptyList() {
        when(userRepository.existsById(EXISTING_USER_ID)).thenReturn(true);
        when(addressRepository.findByUserId(EXISTING_USER_ID)).thenReturn(List.of());

        List<Address> result = addressService.getAddresses(EXISTING_USER_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    @Tag("AC-015")
    @DisplayName("T-004: given a userId that does not exist, when getAddresses, then throws 404")
    void getAddresses_nonexistentUser_throws404() {
        when(userRepository.existsById(NONEXISTENT_USER_ID)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> addressService.getAddresses(NONEXISTENT_USER_ID));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("user not found", ex.getReason());
    }
}
