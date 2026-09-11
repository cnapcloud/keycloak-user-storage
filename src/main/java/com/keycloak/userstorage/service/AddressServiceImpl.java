package com.keycloak.userstorage.service;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.keycloak.userstorage.model.Address;
import com.keycloak.userstorage.repository.AddressRepository;
import com.keycloak.userstorage.repository.UserRepository;

@Service
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public AddressServiceImpl(AddressRepository addressRepository, UserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public Address createAddress(String userId, Address address) {
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found");
        }
        validate(address);
        address.setUserId(userId);
        address.setId("addr-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        return addressRepository.save(address);
    }

    @Override
    public Address getAddress(String userId, String addressId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "address not found"));
    }

    private void validate(Address address) {
        if (address.getPostalCode() == null || !address.getPostalCode().matches("\\d{5}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "postal code must be exactly 5 digits");
        }
        if (address.getRoadAddress() == null || address.getRoadAddress().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "road address must not be blank");
        }
    }
}
