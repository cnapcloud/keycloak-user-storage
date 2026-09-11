package com.keycloak.userstorage.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.keycloak.userstorage.model.Address;

public interface AddressRepository extends JpaRepository<Address, String> {
}
