package com.keycloak.userstorage.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.keycloak.userstorage.model.User;

public interface UserService {
    List<User> searchUsers(Map<String, String> reqParams, Integer first, Integer max);
    Long countUsers(Map<String, String> reqParams);
    Integer getAllUserCount();
    Optional<User> getUserById(String id);
    Optional<User> getUserByUsername(String username);
    void updateUser(String id, User patch);
    User createUser(User user);
    void patchAttributes(String id, Map<String, String> attrs);
    void deleteUser(String id);
}
