package com.osinergmin.osinergmin.service;

import com.osinergmin.osinergmin.dto.RegistroUsuarioRequest;
import com.osinergmin.osinergmin.dto.UsuarioResponse;
import com.osinergmin.osinergmin.exception.UsuarioYaExisteException;
import com.osinergmin.osinergmin.model.Usuario;
import com.osinergmin.osinergmin.repository.UsuarioRepository;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UsuarioResponse registrar(RegistroUsuarioRequest request) {
        String username = request.username().trim();
        if (repository.existsByUsernameIgnoreCase(username)) {
            throw new UsuarioYaExisteException(username);
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPasswordHash(passwordEncoder.encode(request.password())); // BCrypt
        usuario.setRol(request.rol());
        usuario.setActivo(true);

        return UsuarioResponse.from(repository.save(usuario));
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return repository.findAll().stream().map(UsuarioResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Usuario buscarPorUsername(String username) {
        return repository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado: " + username));
    }
}
