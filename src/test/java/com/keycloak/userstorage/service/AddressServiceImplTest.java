package com.keycloak.userstorage.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    private static final String OTHER_USER_ID = "u-198a2c33";
    private static final String EXISTING_ADDRESS_ID = "addr-3f9c1001";
    private static final String NONEXISTENT_ADDRESS_ID = "addr-00000000";
    private static final String SAMPLE_POSTAL_CODE = "06236";
    private static final String SAMPLE_ROAD_ADDRESS = "서울특별시 강남구 테헤란로 123";
    private static final String OLD_DETAIL_ADDRESS = "101호";
    private static final String NEW_POSTAL_CODE = "12345";
    private static final String NEW_ROAD_ADDRESS = "서울특별시 종로구 세종대로 200";
    private static final String NEW_DETAIL_ADDRESS = "3층 301호";

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

    @Test
    @Tag("AC-008")
    @DisplayName("T-005: given an existing address owned by the user, when updateAddress, "
            + "then the stored address is saved with the submitted postal code, road address, and detail address")
    void updateAddress_existingAddressOfOwningUser_replacesAllFieldsAndSaves() {
        Address existing = new Address();
        existing.setId(EXISTING_ADDRESS_ID);
        existing.setUserId(EXISTING_USER_ID);
        existing.setPostalCode(SAMPLE_POSTAL_CODE);
        existing.setRoadAddress(SAMPLE_ROAD_ADDRESS);
        existing.setDetailAddress(OLD_DETAIL_ADDRESS);

        Address submitted = new Address();
        submitted.setPostalCode(NEW_POSTAL_CODE);
        submitted.setRoadAddress(NEW_ROAD_ADDRESS);
        submitted.setDetailAddress(NEW_DETAIL_ADDRESS);

        when(addressRepository.findByIdAndUserId(EXISTING_ADDRESS_ID, EXISTING_USER_ID))
                .thenReturn(Optional.of(existing));

        addressService.updateAddress(EXISTING_USER_ID, EXISTING_ADDRESS_ID, submitted);

        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressRepository).save(captor.capture());
        Address saved = captor.getValue();
        assertEquals(EXISTING_ADDRESS_ID, saved.getId());
        assertEquals(EXISTING_USER_ID, saved.getUserId());
        assertEquals(NEW_POSTAL_CODE, saved.getPostalCode());
        assertEquals(NEW_ROAD_ADDRESS, saved.getRoadAddress());
        assertEquals(NEW_DETAIL_ADDRESS, saved.getDetailAddress());
    }

    @Test
    @Tag("AC-008")
    @DisplayName("T-005: given an existing address with a detail address, when updateAddress is submitted "
            + "with no detail address, then the stored detail address is overwritten with null (full replace, not partial)")
    void updateAddress_detailAddressOmitted_overwritesExistingDetailAddressWithNull() {
        Address existing = new Address();
        existing.setId(EXISTING_ADDRESS_ID);
        existing.setUserId(EXISTING_USER_ID);
        existing.setPostalCode(SAMPLE_POSTAL_CODE);
        existing.setRoadAddress(SAMPLE_ROAD_ADDRESS);
        existing.setDetailAddress(OLD_DETAIL_ADDRESS);

        Address submitted = new Address();
        submitted.setPostalCode(NEW_POSTAL_CODE);
        submitted.setRoadAddress(NEW_ROAD_ADDRESS);
        submitted.setDetailAddress(null);

        when(addressRepository.findByIdAndUserId(EXISTING_ADDRESS_ID, EXISTING_USER_ID))
                .thenReturn(Optional.of(existing));

        addressService.updateAddress(EXISTING_USER_ID, EXISTING_ADDRESS_ID, submitted);

        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressRepository).save(captor.capture());
        assertNull(captor.getValue().getDetailAddress());
    }

    @Test
    @Tag("AC-005")
    @DisplayName("T-005: given an existing address, when updateAddress is submitted with a postal code "
            + "that is not exactly 5 digits, then the system throws 400 and does not save the change")
    void updateAddress_invalidPostalCode_throws400AndDoesNotSave() {
        Address existing = new Address();
        existing.setId(EXISTING_ADDRESS_ID);
        existing.setUserId(EXISTING_USER_ID);
        existing.setPostalCode(SAMPLE_POSTAL_CODE);
        existing.setRoadAddress(SAMPLE_ROAD_ADDRESS);

        Address submitted = new Address();
        submitted.setPostalCode("1234");
        submitted.setRoadAddress(NEW_ROAD_ADDRESS);

        when(addressRepository.findByIdAndUserId(EXISTING_ADDRESS_ID, EXISTING_USER_ID))
                .thenReturn(Optional.of(existing));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> addressService.updateAddress(EXISTING_USER_ID, EXISTING_ADDRESS_ID, submitted));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("postal code must be exactly 5 digits", ex.getReason());
        verify(addressRepository, never()).save(any(Address.class));
    }

    @Test
    @Tag("AC-013")
    @DisplayName("T-005: given an existing address, when updateAddress is submitted with a blank road address, "
            + "then the system throws 400 and does not save the change")
    void updateAddress_blankRoadAddress_throws400AndDoesNotSave() {
        Address existing = new Address();
        existing.setId(EXISTING_ADDRESS_ID);
        existing.setUserId(EXISTING_USER_ID);
        existing.setPostalCode(SAMPLE_POSTAL_CODE);
        existing.setRoadAddress(SAMPLE_ROAD_ADDRESS);

        Address submitted = new Address();
        submitted.setPostalCode(NEW_POSTAL_CODE);
        submitted.setRoadAddress("");

        when(addressRepository.findByIdAndUserId(EXISTING_ADDRESS_ID, EXISTING_USER_ID))
                .thenReturn(Optional.of(existing));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> addressService.updateAddress(EXISTING_USER_ID, EXISTING_ADDRESS_ID, submitted));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("road address must not be blank", ex.getReason());
        verify(addressRepository, never()).save(any(Address.class));
    }

    @Test
    @Tag("AC-010")
    @DisplayName("T-005: given an addressId that does not exist, when updateAddress, "
            + "then the system throws 404 and does not save any change")
    void updateAddress_nonexistentAddressId_throws404AndDoesNotSave() {
        Address submitted = new Address();
        submitted.setPostalCode(NEW_POSTAL_CODE);
        submitted.setRoadAddress(NEW_ROAD_ADDRESS);

        when(addressRepository.findByIdAndUserId(NONEXISTENT_ADDRESS_ID, EXISTING_USER_ID))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> addressService.updateAddress(EXISTING_USER_ID, NONEXISTENT_ADDRESS_ID, submitted));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("address not found", ex.getReason());
        verify(addressRepository, never()).save(any(Address.class));
    }

    @Test
    @Tag("AC-014")
    @DisplayName("T-005: given an addressId that belongs to a different user, when updateAddress, "
            + "then the system throws 404 and does not save any change")
    void updateAddress_addressBelongsToDifferentUser_throws404AndDoesNotSave() {
        Address submitted = new Address();
        submitted.setPostalCode(NEW_POSTAL_CODE);
        submitted.setRoadAddress(NEW_ROAD_ADDRESS);

        when(addressRepository.findByIdAndUserId(EXISTING_ADDRESS_ID, OTHER_USER_ID))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> addressService.updateAddress(OTHER_USER_ID, EXISTING_ADDRESS_ID, submitted));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("address not found", ex.getReason());
        verify(addressRepository, never()).save(any(Address.class));
    }
}
