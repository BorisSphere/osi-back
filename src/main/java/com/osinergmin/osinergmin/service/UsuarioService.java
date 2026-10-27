package com.osinergmin.osinergmin.service;

import com.osinergmin.osinergmin.dto.RegistroUsuarioRequest;
import com.osinergmin.osinergmin.dto.UsuarioResponse;
import com.osinergmin.osinergmin.exception.EntraAccesoDenegadoException;
import com.osinergmin.osinergmin.exception.UsuarioYaExisteException;
import com.osinergmin.osinergmin.model.Rol;
import com.osinergmin.osinergmin.model.Usuario;
import com.osinergmin.osinergmin.repository.UsuarioRepository;
import java.util.List;
import java.util.UUID;
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

    /**
     * Busca el usuario de Entra (por oid, correo o username) y lo vincula; el rol siempre se sincroniza desde Entra.
     * Si no existe, lo crea solo cuando autoCrear está activo. La contraseña local queda aleatoria (inutilizable).
     */
    @Transactional
    public Usuario sincronizarDesdeEntra(String oid, String correo, Rol rol, boolean autoCrear) {
        Usuario usuario = repository.findByEntraOid(oid)
                .or(() -> repository.findByCorreoIgnoreCase(correo))
                .or(() -> repository.findByUsernameIgnoreCase(correo))
                .orElse(null);

        if (usuario == null) {
            if (!autoCrear) {
                throw new EntraAccesoDenegadoException("Su cuenta no está registrada en el sistema");
            }
            usuario = new Usuario();
            usuario.setUsername(correo);
            usuario.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
            usuario.setActivo(true);
        } else if (usuario.getEntraOid() != null && !usuario.getEntraOid().equalsIgnoreCase(oid)) {
            throw new EntraAccesoDenegadoException("El correo ya está vinculado a otra cuenta de Microsoft");
        }

        if (!usuario.getActivo()) {
            throw new EntraAccesoDenegadoException("El usuario está inactivo");
        }
        usuario.setEntraOid(oid);
        usuario.setCorreo(correo);
        usuario.setRol(rol);
        return repository.save(usuario);
    }

    @Transactional(readOnly = true)
    public Usuario buscarPorUsername(String username) {
        return repository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado: " + username));
    }
}
