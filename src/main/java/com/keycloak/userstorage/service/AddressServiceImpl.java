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
        address.setUserId(userId);
        address.setId("addr-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        return addressRepository.save(address);
    }
}
