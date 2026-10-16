package com.osinergmin.osinergmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OsinergminApplication {

	public static void main(String[] args) {
		SpringApplication.run(OsinergminApplication.class, args);
	}

}
