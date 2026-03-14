package com.keycloak.userstorage.service;

import java.util.Optional;

import com.keycloak.userstorage.model.CredentialData;

public interface CredentialDataService {
    Optional<CredentialData> getCredentialById(String id);
    CredentialData upsertCredential(String id, CredentialData credential);
    void deleteCredential(String id);
}
