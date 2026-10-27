package com.osinergmin.osinergmin.config;

import com.osinergmin.osinergmin.security.MailGraphProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MailGraphProperties.class)
public class MailPropertiesConfig {
}
