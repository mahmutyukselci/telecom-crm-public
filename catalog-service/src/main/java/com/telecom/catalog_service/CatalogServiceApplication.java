package com.telecom.catalog_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;


@EnableCaching
@EnableAutoConfiguration(exclude={MongoAutoConfiguration.class})
@SpringBootApplication(scanBasePackages = {"com.telecom"})
public class CatalogServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CatalogServiceApplication.class, args);
	}

}
