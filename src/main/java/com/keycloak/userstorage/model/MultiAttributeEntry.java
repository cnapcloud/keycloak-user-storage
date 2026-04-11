package com.keycloak.userstorage.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class MultiAttributeEntry {

    @Column(name = "attr_key", nullable = false)
    private String key;

    @Column(name = "attr_value")
    private String value;
}
