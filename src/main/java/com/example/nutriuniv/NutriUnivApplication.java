package com.example.nutriuniv;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class NutriUnivApplication {

	public static void main(String[] args) {
		SpringApplication.run(NutriUnivApplication.class, args);
	}

}
