package com.osinergmin.osinergmin.config;

import io.micrometer.observation.ObservationPredicate;
import io.micrometer.observation.ObservationRegistry;
import java.util.EnumSet;
import javax.sql.DataSource;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import net.ttddyy.observation.tracing.DataSourceObservationListener;
import net.ttddyy.observation.tracing.JdbcObservationDocumentation;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.observation.ServerRequestObservationContext;

@Configuration(proxyBeanMethods = false)
public class TracingConfig {

    /** Sin trazas para /actuator/**: Prometheus lo consulta cada 15 s y solo llenaría Tempo de ruido. */
    @Bean
    public ObservationPredicate sinTrazasDeActuator() {
        return (name, context) -> !(context instanceof ServerRequestObservationContext request
                && request.getCarrier().getRequestURI().contains("/actuator/"));
    }

    /**
     * Envuelve el DataSource para que cada conexión y query a Oracle aparezca como span en la traza.
     * No se registran los valores de los parámetros (pueden ser datos personales).
     */
    @Bean
    public static BeanPostProcessor dataSourceTracingPostProcessor(ObjectProvider<ObservationRegistry> registry) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (!(bean instanceof DataSource dataSource) || bean instanceof ProxyDataSource) {
                    return bean;
                }
                DataSourceObservationListener listener = new DataSourceObservationListener(registry::getObject);
                listener.setSupportedTypes(EnumSet.of(JdbcObservationDocumentation.CONNECTION,
                        JdbcObservationDocumentation.QUERY));
                return ProxyDataSourceBuilder.create(dataSource)
                        .name(beanName)
                        .listener(listener)
                        .methodListener(listener)
                        .build();
            }
        };
    }
}
