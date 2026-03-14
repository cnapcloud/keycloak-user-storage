package com.keycloak.userstorage.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.keycloak.userstorage.model.CredentialData;

public interface CredentialDataRepository extends JpaRepository<CredentialData, String> {

    @Query("SELECT c FROM CredentialData c JOIN User u ON c.id = u.id WHERE u.username = :username")
    Optional<CredentialData> findByUsername(@Param("username") String username);
}
