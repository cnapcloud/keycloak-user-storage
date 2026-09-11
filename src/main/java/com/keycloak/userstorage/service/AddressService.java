package com.keycloak.userstorage.service;

import java.util.List;

import com.keycloak.userstorage.model.Address;

public interface AddressService {
    Address createAddress(String userId, Address address);

    Address getAddress(String userId, String addressId);

    List<Address> getAddresses(String userId);

    void updateAddress(String userId, String addressId, Address address);

    void deleteAddress(String userId, String addressId);
}
