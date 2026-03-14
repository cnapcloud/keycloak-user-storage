package com.keycloak.userstorage.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.keycloak.userstorage.model.CredentialData;
import com.keycloak.userstorage.repository.CredentialDataRepository;

@Service
public class CredentialDataServiceImpl implements CredentialDataService {

    private final CredentialDataRepository credentialDataRepository;

    @Autowired
    public CredentialDataServiceImpl(CredentialDataRepository credentialDataRepository) {
        this.credentialDataRepository = credentialDataRepository;
    }

    @Override
    public Optional<CredentialData> getCredentialById(String id) {
        return resolve(id);
    }

    @Override
    @Transactional
    public CredentialData upsertCredential(String id, CredentialData credential) {
        CredentialData existing = resolve(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "credential not found"));
        credential.setId(existing.getId());
        credential.setCreatedDate(existing.getCreatedDate());
        credential.setUpdatedDate(Timestamp.from(Instant.now()));
        return credentialDataRepository.save(credential);
    }

    @Override
    public void deleteCredential(String id) {
        CredentialData existing = resolve(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "credential not found"));
        credentialDataRepository.deleteById(existing.getId());
    }

    // PK이면 findById, username이면 JOIN 쿼리로 단일 조회
    private Optional<CredentialData> resolve(String id) {
        if (id != null && id.matches("u-[0-9a-f]{8}")) {
            return credentialDataRepository.findById(id);
        }
        return credentialDataRepository.findByUsername(id);
    }
}
