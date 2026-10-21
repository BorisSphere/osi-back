package com.osinergmin.osinergmin.security;

import com.osinergmin.osinergmin.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Carga el usuario desde LAB_USUARIO para validar la contraseña en el login. */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository repository;

    public UsuarioDetailsService(UsuarioRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return repository.findByUsernameIgnoreCase(username)
                .map(usuario -> User.withUsername(usuario.getUsername())
                        .password(usuario.getPasswordHash())
                        .roles(usuario.getRol().name()) // solo para el login; los permisos de cada petición vienen del JWT
                        .disabled(!usuario.getActivo())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }
}
