package com.mercado_libre.gestion_productos.repository;

import com.mercado_libre.gestion_productos.model.Role;
import com.mercado_libre.gestion_productos.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByPasswordResetToken(String passwordResetToken);

    List<User> findByRoleOrderByCreatedAtDesc(Role role);

    long countByRole(Role role);

    boolean existsByRole(Role role);
}
