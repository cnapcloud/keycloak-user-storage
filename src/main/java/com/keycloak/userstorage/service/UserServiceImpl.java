package com.keycloak.userstorage.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.keycloak.userstorage.model.MultiAttributeEntry;
import com.keycloak.userstorage.model.User;
import com.keycloak.userstorage.repository.UserRepository;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<User> searchUsers(Map<String, String> reqParams, Integer first, Integer max) {
        String search = reqParams.get("search");

        if (search != null) {
            if (search.equals("*")) search = "%";
            return userRepository.searchUserWithAllFields(search, first, max);
        }

        Map<String, Object> params = new HashMap<>();
        reqParams.forEach((key, value) -> {
            if (value != null && !value.isEmpty()) {
                params.put(key, value);
            }
        });

        return userRepository.findUsersByConditions(params, first, max);
    }

    @Override
    public Long countUsers(Map<String, String> reqParams) {
        String search = reqParams.get("search");
        if (search != null) {
            if (search.equals("*")) search = "%";
            return userRepository.countUsersWithWithAllFields(search);
        }
        Map<String, Object> params = new HashMap<>();
        reqParams.forEach((key, value) -> {
            if (value != null && !value.isEmpty()) {
                params.put(key, value);
            }
        });
        return userRepository.countUsersByConditions(params);
    }

    @Override
    public Integer getAllUserCount() {
        return (int) userRepository.count();
    }

    @Override
    public Optional<User> getUserById(String id) {
        return isPk(id) ? userRepository.findById(id) : userRepository.findByUsername(id);
    }

    @Override
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    @Transactional
    public void updateUser(String id, User patch) {
        User user = findByIdOrUsername(id);
        if (patch.getUsername() != null) user.setUsername(patch.getUsername());
        if (patch.getFirstName() != null) user.setFirstName(patch.getFirstName());
        if (patch.getLastName() != null) user.setLastName(patch.getLastName());
        if (patch.getEmail() != null) user.setEmail(patch.getEmail());
        user.setEnabled(patch.isEnabled());
        user.setEmailVerified(patch.isEmailVerified());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public User createUser(User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "user already exists");
        }
        user.setId("u-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        if (user.getAttributes() == null) {
            user.setAttributes(new HashMap<>());
        }
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void patchAttributes(String id, Map<String, String> attrs) {
        User user = findByIdOrUsername(id);
        attrs.forEach((key, value) -> {
            if (value == null) {
                user.getAttributes().remove(key);
            } else {
                user.getAttributes().put(key, value);
            }
        });
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void patchMultiAttributes(String id, Map<String, List<String>> attrs) {
        User user = findByIdOrUsername(id);
        List<MultiAttributeEntry> multiAttributes = user.getMultiAttributes();
        attrs.forEach((key, values) -> {
            multiAttributes.removeIf(entry -> entry.getKey().equals(key));
            if (values != null && !values.isEmpty()) {
                values.forEach(value -> multiAttributes.add(new MultiAttributeEntry(key, value)));
            }
        });
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteUser(String id) {
        User user = findByIdOrUsername(id);
        userRepository.deleteById(user.getId());
    }

    // PK(u-XXXXXXXX) 또는 username 어느 쪽으로 넘어와도 단일 쿼리로 조회
    private User findByIdOrUsername(String id) {
        Optional<User> user = isPk(id) ? userRepository.findById(id) : userRepository.findByUsername(id);
        return user.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
    }

    // PK 형식: "u-" + 8자리 hex (예: u-092d66b8, u-3f9c1001)
    private boolean isPk(String id) {
        return id != null && id.matches("u-[0-9a-f]{8}");
    }
}
