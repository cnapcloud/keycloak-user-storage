package com.keycloak.userstorage;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keycloak.userstorage.config.JacksonConfig;
import com.keycloak.userstorage.model.CredentialData;
import com.keycloak.userstorage.model.User;
import com.keycloak.userstorage.repository.CredentialDataRepository;
import com.keycloak.userstorage.repository.UserRepository;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
public class UserApplication {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CredentialDataRepository credentialDataRepository;

    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }

    @PostConstruct
    public void initData() {
        ObjectMapper objectMapper = JacksonConfig.getMapper();

        try {
            InputStream credentialInputStream = getClass().getClassLoader().getResourceAsStream("credential-data.json");
            if (credentialInputStream == null) {
                throw new RuntimeException("credential-data.json file not found in resources folder");
            }
            CredentialData credential = objectMapper.readValue(credentialInputStream,
                    new TypeReference<CredentialData>() {});

            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("user-data.json");
            if (inputStream == null) {
                throw new RuntimeException("user-data.json file not found in resources folder");
            }
            List<User> users = objectMapper.readValue(inputStream, new TypeReference<List<User>>() {});

            String prefix = "3f9c1";
            for (int i = 0; i < users.size(); i++) {
                User user = users.get(i);
                user.setId("u-" + prefix + String.format("%03d", i + 1));
                user.setCreatedDate(LocalDateTime.now());
                // attributes null 체크 (CLAUDE.md 규칙)
                if (user.getAttributes() == null) {
                    user.setAttributes(new java.util.HashMap<>());
                }
                userRepository.save(user);

                // 각 사용자마다 새로운 credential 객체 생성
                CredentialData userCredential = new CredentialData();
                userCredential.setId(user.getId());
                userCredential.setValue(credential.getValue());
                userCredential.setSalt(credential.getSalt());
                userCredential.setAlgorithm(credential.getAlgorithm());
                userCredential.setIterations(credential.getIterations());
                userCredential.setType(credential.getType());
                userCredential.setAdditionParameters(credential.getAdditionParameters());
                credentialDataRepository.save(userCredential);
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to read and process user-data.json", e);
        }
    }
}
