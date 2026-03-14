package com.keycloak.userstorage.config;

import java.time.LocalDateTime;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class JacksonConfig {

	@Bean
	public ObjectMapper objectMapper() {
		return getMapper();
	}

	public static ObjectMapper getMapper() {
		return new Jackson2ObjectMapperBuilder().featuresToEnable(SerializationFeature.INDENT_OUTPUT)
				.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
				.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
				.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
				.serializationInclusion(JsonInclude.Include.NON_NULL)
				.modules(new JavaTimeModule(), createCustomModule()).build();
	}

	private static SimpleModule createCustomModule() {
		SimpleModule module = new SimpleModule();
		module.addSerializer(LocalDateTime.class, new CustomLocalDateTimeSerializer());
		module.addDeserializer(LocalDateTime.class, new CustomLocalDateTimeDeserializer());
		return module;
	}
}
