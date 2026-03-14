package com.keycloak.userstorage.model;

import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class CredentialData {
    @Id
    private String id;
    @Column(name = "\"value\"")
	private String value;
	private String salt ;
	private String algorithm;
	private Integer iterations;
	private String additionParameters;
	private String type;
    private Timestamp createdDate;
    private Timestamp updatedDate;
}
