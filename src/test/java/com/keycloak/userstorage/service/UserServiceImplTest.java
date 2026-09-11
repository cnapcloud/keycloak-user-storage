package com.keycloak.userstorage.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.keycloak.userstorage.model.User;
import com.keycloak.userstorage.repository.AddressRepository;
import com.keycloak.userstorage.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final String EXISTING_USER_ID = "u-092d66b8";

    @Mock
    private UserRepository userRepository;
    @Mock
    private AddressRepository addressRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @Tag("AC-011")
    @DisplayName("T-007: given an existing user, when deleteUser, "
            + "then all of that user's addresses are cascade-deleted before the user row is deleted")
    void deleteUser_existingUser_deletesAddressesBeforeDeletingUser() {
        User existing = new User();
        existing.setId(EXISTING_USER_ID);
        existing.setUsername("existing-user");

        when(userRepository.findById(EXISTING_USER_ID)).thenReturn(Optional.of(existing));

        userService.deleteUser(EXISTING_USER_ID);

        InOrder inOrder = inOrder(addressRepository, userRepository);
        inOrder.verify(addressRepository).deleteByUserId(EXISTING_USER_ID);
        inOrder.verify(userRepository).deleteById(EXISTING_USER_ID);
    }
}
