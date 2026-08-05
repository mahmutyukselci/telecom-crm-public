package com.telecom.notification_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;
@EnableJpaRepositories(basePackages = "com.telecom.notification_service.repository")
@SpringBootApplication(scanBasePackages = {"com.telecom"})
@EnableKafka
public class NotificationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotificationServiceApplication.class, args);
	}
}
