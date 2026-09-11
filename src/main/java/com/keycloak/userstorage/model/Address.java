package com.keycloak.userstorage.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class Address {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private String postalCode;

    @Column(nullable = false)
    private String roadAddress;

    private String detailAddress;
}
