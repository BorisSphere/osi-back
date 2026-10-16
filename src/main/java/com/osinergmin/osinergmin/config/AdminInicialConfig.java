package com.osinergmin.osinergmin.config;

import com.osinergmin.osinergmin.dto.RegistroUsuarioRequest;
import com.osinergmin.osinergmin.model.Rol;
import com.osinergmin.osinergmin.repository.UsuarioRepository;
import com.osinergmin.osinergmin.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Crea el primer COORDINADOR (administrador de usuarios) al arrancar, solo si no existe
 * ninguno y si se definieron las variables ADMIN_USERNAME y ADMIN_PASSWORD. Resuelve el
 * "huevo y la gallina": registrar usuarios requiere un COORDINADOR.
 */
@Configuration
public class AdminInicialConfig {

    private static final Logger log = LoggerFactory.getLogger(AdminInicialConfig.class);

    @Bean
    public ApplicationRunner crearAdminInicial(UsuarioRepository repository, UsuarioService usuarioService,
            @Value("${app.admin-inicial.username:}") String username,
            @Value("${app.admin-inicial.password:}") String password) {
        return args -> {
            if (!StringUtils.hasText(username) || !StringUtils.hasText(password) || repository.existsByRol(Rol.COORDINADOR)) {
                return;
            }
            usuarioService.registrar(new RegistroUsuarioRequest(username, password, Rol.COORDINADOR));
            log.info("Se creó el usuario coordinador inicial '{}'", username);
        };
    }
}
