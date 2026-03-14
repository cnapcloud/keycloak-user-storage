package com.keycloak.userstorage.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.keycloak.userstorage.model.CredentialData;
import com.keycloak.userstorage.service.CredentialDataService;

@RestController
@RequestMapping("/credential")
public class CredentialDataController {

    private final CredentialDataService credentialDataService;

    public CredentialDataController(CredentialDataService credentialDataService) {
        this.credentialDataService = credentialDataService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<CredentialData> getCredentialById(@PathVariable String id) {
        return credentialDataService.getCredentialById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "credential not found"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateCredential(@PathVariable String id,
            @RequestBody CredentialData credential) {
        credentialDataService.upsertCredential(id, credential);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCredential(@PathVariable String id) {
        credentialDataService.deleteCredential(id);
        return ResponseEntity.noContent().build();
    }
}
