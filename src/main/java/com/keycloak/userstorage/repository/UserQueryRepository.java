package com.keycloak.userstorage.repository;

import com.keycloak.userstorage.model.User;
import java.util.List;
import java.util.Map;

public interface UserQueryRepository {
    List<User> findUsersByConditions(Map<String, Object> params, Integer first, Integer max);
    
    Long countUsersByConditions(Map<String, Object> params);

	List<User> searchUserWithAllFields(String search, Integer first, Integer max);

	Long countUsersWithWithAllFields(String search);
}
