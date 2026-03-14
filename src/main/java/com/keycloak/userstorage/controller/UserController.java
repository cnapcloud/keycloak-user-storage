package com.keycloak.userstorage.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.keycloak.userstorage.model.User;
import com.keycloak.userstorage.service.UserService;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/search")
    public List<User> searchUsers(@RequestParam Map<String, String> reqParams,
            @RequestParam(required = false) Integer first,
            @RequestParam(required = false) Integer max) {
        return userService.searchUsers(reqParams, first, max);
    }

    @GetMapping("/count/all")
    public Integer getUserCountAll() {
        return userService.getAllUserCount();
    }

    @GetMapping("/count")
    public Long getUserCount(@RequestParam Map<String, String> reqParams) {
        return userService.countUsers(reqParams);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable String id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        return userService.getUserByUsername(username)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Map<String, String>> createUser(@RequestBody User user) {
        User saved = userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", saved.getId(),
                "username", saved.getUsername(),
                "email", saved.getEmail() != null ? saved.getEmail() : ""
        ));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> updateUser(@PathVariable String id, @RequestBody User patch) {
        userService.updateUser(id, patch);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/attributes")
    @Transactional
    public ResponseEntity<Void> patchAttributes(@PathVariable String id,
            @RequestBody Map<String, String> attrs) {
        userService.patchAttributes(id, attrs);
        return ResponseEntity.noContent().build();
    }
}
