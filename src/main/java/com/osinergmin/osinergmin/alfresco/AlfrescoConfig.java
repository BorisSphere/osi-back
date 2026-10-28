package com.osinergmin.osinergmin.alfresco;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(AlfrescoProperties.class)
public class AlfrescoConfig {

    @Bean
    public AlfrescoClient alfrescoClient(AlfrescoProperties properties) {
        // Tiempos acotados: si Alfresco no responde, el usuario recibe un error claro en segundos.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(30_000);
        return new AlfrescoClient(properties, RestClient.builder().requestFactory(factory));
    }
}
