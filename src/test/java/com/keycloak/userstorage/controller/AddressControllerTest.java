package com.keycloak.userstorage.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.keycloak.userstorage.model.Address;
import com.keycloak.userstorage.service.AddressService;

@WebMvcTest(AddressController.class)
class AddressControllerTest {

    private static final String EXISTING_USER_ID = "u-092d66b8";
    private static final String NONEXISTENT_USER_ID = "u-00000000";
    private static final String OTHER_USER_ID = "u-198a2c33";
    private static final String EXISTING_ADDRESS_ID = "addr-3f9c1001";
    private static final String NONEXISTENT_ADDRESS_ID = "addr-00000000";
    private static final String SAMPLE_POSTAL_CODE = "06236";
    private static final String SAMPLE_ROAD_ADDRESS = "서울특별시 강남구 테헤란로 123";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AddressService addressService;

    @Test
    @Tag("AC-007")
    @DisplayName("T-004: given the service returns two addresses, when GET /user/{userId}/addresses, "
            + "then 200 with both addresses in the response body")
    void getAddresses_serviceReturnsAddresses_returns200WithList() throws Exception {
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

        when(addressService.getAddresses(EXISTING_USER_ID)).thenReturn(List.of(first, second));

        mockMvc.perform(get("/user/" + EXISTING_USER_ID + "/addresses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("addr-3f9c1001"))
                .andExpect(jsonPath("$[1].id").value("addr-3f9c1002"));
    }

    @Test
    @Tag("AC-007")
    @DisplayName("T-004: given the service returns no addresses, when GET /user/{userId}/addresses, "
            + "then 200 with an empty array")
    void getAddresses_serviceReturnsEmptyList_returns200WithEmptyArray() throws Exception {
        when(addressService.getAddresses(EXISTING_USER_ID)).thenReturn(List.of());

        mockMvc.perform(get("/user/" + EXISTING_USER_ID + "/addresses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @Tag("AC-015")
    @DisplayName("T-004: given the service throws 404, when GET /user/{userId}/addresses, "
            + "then the controller returns 404 with the error body")
    void getAddresses_serviceThrows404_returns404() throws Exception {
        when(addressService.getAddresses(anyString()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));

        mockMvc.perform(get("/user/" + NONEXISTENT_USER_ID + "/addresses"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("user not found"));
    }

    @Test
    @Tag("AC-008")
    @DisplayName("T-005: given the service accepts the update, when PUT /user/{userId}/addresses/{addressId}, "
            + "then 204 with no body and the service is called with the path ids and submitted fields")
    void updateAddress_serviceAccepts_returns204() throws Exception {
        String requestBody = "{\"postalCode\":\"" + SAMPLE_POSTAL_CODE + "\",\"roadAddress\":\""
                + SAMPLE_ROAD_ADDRESS + "\",\"detailAddress\":\"3층 301호\"}";

        mockMvc.perform(put("/user/" + EXISTING_USER_ID + "/addresses/" + EXISTING_ADDRESS_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNoContent());

        verify(addressService).updateAddress(eq(EXISTING_USER_ID), eq(EXISTING_ADDRESS_ID), any(Address.class));
    }

    @Test
    @Tag("AC-005")
    @DisplayName("T-005: given the service throws 400 for an invalid postal code, when PUT, "
            + "then the controller returns 400 with the error body")
    void updateAddress_serviceThrowsBadRequestForPostalCode_returns400() throws Exception {
        String requestBody = "{\"postalCode\":\"1234\",\"roadAddress\":\"" + SAMPLE_ROAD_ADDRESS + "\"}";

        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "postal code must be exactly 5 digits"))
                .when(addressService).updateAddress(anyString(), anyString(), any(Address.class));

        mockMvc.perform(put("/user/" + EXISTING_USER_ID + "/addresses/" + EXISTING_ADDRESS_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("postal code must be exactly 5 digits"));
    }

    @Test
    @Tag("AC-013")
    @DisplayName("T-005: given the service throws 400 for a blank road address, when PUT, "
            + "then the controller returns 400 with the error body")
    void updateAddress_serviceThrowsBadRequestForRoadAddress_returns400() throws Exception {
        String requestBody = "{\"postalCode\":\"" + SAMPLE_POSTAL_CODE + "\",\"roadAddress\":\"\"}";

        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "road address must not be blank"))
                .when(addressService).updateAddress(anyString(), anyString(), any(Address.class));

        mockMvc.perform(put("/user/" + EXISTING_USER_ID + "/addresses/" + EXISTING_ADDRESS_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("road address must not be blank"));
    }

    @Test
    @Tag("AC-010")
    @DisplayName("T-005: given the service throws 404 for a nonexistent addressId, when PUT, "
            + "then the controller returns 404 with the error body")
    void updateAddress_serviceThrowsNotFoundForNonexistentAddressId_returns404() throws Exception {
        String requestBody = "{\"postalCode\":\"" + SAMPLE_POSTAL_CODE + "\",\"roadAddress\":\""
                + SAMPLE_ROAD_ADDRESS + "\"}";

        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "address not found"))
                .when(addressService).updateAddress(anyString(), anyString(), any(Address.class));

        mockMvc.perform(put("/user/" + EXISTING_USER_ID + "/addresses/" + NONEXISTENT_ADDRESS_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("address not found"));
    }

    @Test
    @Tag("AC-014")
    @DisplayName("T-005: given the service throws 404 because the address belongs to a different user, when PUT, "
            + "then the controller returns 404 with the error body")
    void updateAddress_serviceThrowsNotFoundForDifferentOwner_returns404() throws Exception {
        String requestBody = "{\"postalCode\":\"" + SAMPLE_POSTAL_CODE + "\",\"roadAddress\":\""
                + SAMPLE_ROAD_ADDRESS + "\"}";

        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "address not found"))
                .when(addressService).updateAddress(anyString(), anyString(), any(Address.class));

        mockMvc.perform(put("/user/" + OTHER_USER_ID + "/addresses/" + EXISTING_ADDRESS_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("address not found"));
    }
}
