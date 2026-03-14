package com.keycloak.userstorage.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.keycloak.userstorage.model.User;

public interface UserRepository extends JpaRepository<User, String>, UserQueryRepository {

    Optional<User> findByUsername(String username);

    @Query("SELECT p FROM User p WHERE p.username LIKE %:s% OR p.firstName LIKE %:s% OR p.lastName LIKE %:s%")
    List<User> searchUsers(@Param("s") String search, Pageable pageable);

    @Query("SELECT COUNT(p) FROM User p WHERE p.username LIKE %:s% OR p.firstName LIKE %:s% OR p.lastName LIKE %:s%")
    Long countUsers(@Param("s") String search);
}
