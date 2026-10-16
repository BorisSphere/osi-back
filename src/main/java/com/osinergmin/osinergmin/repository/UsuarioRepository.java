package com.osinergmin.osinergmin.repository;

import com.osinergmin.osinergmin.model.Rol;
import com.osinergmin.osinergmin.model.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByRol(Rol rol);
}
