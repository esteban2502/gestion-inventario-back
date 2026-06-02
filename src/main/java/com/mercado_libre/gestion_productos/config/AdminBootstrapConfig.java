package com.mercado_libre.gestion_productos.config;

import com.mercado_libre.gestion_productos.model.Role;
import com.mercado_libre.gestion_productos.model.User;
import com.mercado_libre.gestion_productos.repository.UserRepository;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

/**
 * Crea un usuario ADMIN inicial solo si {@code app.admin.bootstrap-email} y
 * {@code app.admin.bootstrap-password} estan definidos (p. ej. en .env).
 */
@Configuration
public class AdminBootstrapConfig {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapConfig.class);

    @Bean
    public ApplicationRunner bootstrapAdminUser(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.bootstrap-email:}") String bootstrapEmail,
            @Value("${app.admin.bootstrap-password:}") String bootstrapPassword,
            @Value("${app.admin.bootstrap-promote-seller:false}") boolean promoteSeller
    ) {
        return args -> {
            if (!StringUtils.hasText(bootstrapEmail) || !StringUtils.hasText(bootstrapPassword)) {
                log.debug("Bootstrap admin omitido: email o contrasena vacios.");
                return;
            }
            String email = bootstrapEmail.trim().toLowerCase();
            String rawPassword = bootstrapPassword.trim();
            Optional<User> existing = userRepository.findByEmail(email);

            if (existing.isEmpty()) {
                User admin = User.builder()
                        .firstName("Admin")
                        .lastName("Sistema")
                        .address("-")
                        .phone("-")
                        .email(email)
                        .passwordHash(passwordEncoder.encode(rawPassword))
                        .role(Role.ADMIN)
                        .build();
                userRepository.save(admin);
                log.info("Usuario ADMIN de arranque creado para {}", email);
                return;
            }

            User user = existing.get();
            if (user.getRole() == Role.ADMIN) {
                log.info("Bootstrap admin: {} ya es ADMIN; no se modifica.", email);
                return;
            }

            if (user.getRole() == Role.SELLER && promoteSeller) {
                user.setRole(Role.ADMIN);
                user.setPasswordHash(passwordEncoder.encode(rawPassword));
                userRepository.save(user);
                log.warn(
                        "Usuario {} promovido a ADMIN por bootstrap (ADMIN_BOOTSTRAP_PROMOTE_SELLER=true). "
                                + "No uses esto en produccion salvo que lo entiendas.",
                        email);
                return;
            }

            log.warn(
                    "Bootstrap admin: el email {} ya existe como {}. Opciones: usar otro "
                            + "ADMIN_BOOTSTRAP_EMAIL, o poner ADMIN_BOOTSTRAP_PROMOTE_SELLER=true para promover "
                            + "ese vendedor a ADMIN y aplicar ADMIN_BOOTSTRAP_PASSWORD.",
                    email,
                    user.getRole());
        };
    }
}
