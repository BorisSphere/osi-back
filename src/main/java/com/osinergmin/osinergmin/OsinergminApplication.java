// package com.osinergmin.osinergmin;

// import org.springframework.boot.SpringApplication;
// import org.springframework.boot.autoconfigure.SpringBootApplication;
// import org.springframework.scheduling.annotation.EnableScheduling;

// @SpringBootApplication
// @EnableScheduling
// public class OsinergminApplication {

// 	public static void main(String[] args) {
// 		SpringApplication.run(OsinergminApplication.class, args);
// 	}

// }
package com.osinergmin.osinergmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OsinergminApplication extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(OsinergminApplication.class);
	}

	public static void main(String[] args) {
		SpringApplication.run(OsinergminApplication.class, args);
	}
}