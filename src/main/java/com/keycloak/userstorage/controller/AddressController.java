package com.keycloak.userstorage.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.keycloak.userstorage.model.Address;
import com.keycloak.userstorage.service.AddressService;

@RestController
@RequestMapping("/user/{userId}/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Address> createAddress(@PathVariable String userId, @RequestBody Address address) {
        Address saved = addressService.createAddress(userId, address);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/{addressId}")
    public ResponseEntity<Address> getAddress(@PathVariable String userId, @PathVariable String addressId) {
        Address address = addressService.getAddress(userId, addressId);
        return ResponseEntity.ok(address);
    }

    @GetMapping
    public ResponseEntity<List<Address>> getAddresses(@PathVariable String userId) {
        List<Address> addresses = addressService.getAddresses(userId);
        return ResponseEntity.ok(addresses);
    }

    @PutMapping("/{addressId}")
    @Transactional
    public ResponseEntity<Void> updateAddress(@PathVariable String userId, @PathVariable String addressId,
            @RequestBody Address address) {
        addressService.updateAddress(userId, addressId, address);
        return ResponseEntity.noContent().build();
    }
}
