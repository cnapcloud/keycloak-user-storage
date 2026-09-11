package com.keycloak.userstorage.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.keycloak.userstorage.model.Address;

public interface AddressRepository extends JpaRepository<Address, String> {

    Optional<Address> findByIdAndUserId(String id, String userId);
}
