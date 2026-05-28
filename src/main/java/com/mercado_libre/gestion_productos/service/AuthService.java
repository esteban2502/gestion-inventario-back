package com.mercado_libre.gestion_productos.service;

import com.mercado_libre.gestion_productos.dto.AuthResponse;
import com.mercado_libre.gestion_productos.dto.AuthenticatedUserDTO;
import com.mercado_libre.gestion_productos.dto.ForgotPasswordRequest;
import com.mercado_libre.gestion_productos.dto.LoginRequest;
import com.mercado_libre.gestion_productos.dto.MessageResponse;
import com.mercado_libre.gestion_productos.dto.RegisterRequest;
import com.mercado_libre.gestion_productos.dto.ResetPasswordRequest;
import com.mercado_libre.gestion_productos.exception.EmailAlreadyExistsException;
import com.mercado_libre.gestion_productos.exception.InvalidPasswordResetTokenException;
import com.mercado_libre.gestion_productos.model.Role;
import com.mercado_libre.gestion_productos.model.User;
import com.mercado_libre.gestion_productos.repository.UserRepository;
import com.mercado_libre.gestion_productos.security.JwtService;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String FORGOT_PASSWORD_MESSAGE =
            "Si el correo esta registrado, recibiras instrucciones para restablecer tu contrasena.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final PasswordResetEmailService passwordResetEmailService;
    private final long passwordResetExpirationMinutes;
    private final String passwordResetFrontendUrl;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            PasswordResetEmailService passwordResetEmailService,
            @Value("${app.password-reset.expiration-minutes:60}") long passwordResetExpirationMinutes,
            @Value("${app.password-reset.frontend-url:http://localhost:4200}") String passwordResetFrontendUrl
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.passwordResetEmailService = passwordResetEmailService;
        this.passwordResetExpirationMinutes = passwordResetExpirationMinutes;
        this.passwordResetFrontendUrl = passwordResetFrontendUrl;
    }

    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException("Email is already registered");
        }

        User user = User.builder()
                .firstName(sanitizeText(request.getFirstName()))
                .lastName(sanitizeText(request.getLastName()))
                .address(sanitizeText(request.getAddress()))
                .phone(sanitizeText(request.getPhone()))
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.SELLER)
                .build();

        User created = userRepository.save(user);
        UserDetails principal = toPrincipal(created);
        String token = jwtService.generateToken(principal, Map.of("role", created.getRole().name()));
        return buildResponse(created, token);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
            );

            User user = userRepository.findByEmail(normalizedEmail)
                    .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

            String token = jwtService.generateToken(
                    (UserDetails) authentication.getPrincipal(),
                    Map.of("role", user.getRole().name())
            );

            return buildResponse(user, token);
        } catch (BadCredentialsException ex) {
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        userRepository.findByEmail(normalizedEmail).ifPresent(user -> {
            String token = UUID.randomUUID().toString().replace("-", "");
            user.setPasswordResetToken(token);
            user.setPasswordResetTokenExpiresAt(
                    LocalDateTime.now().plusMinutes(passwordResetExpirationMinutes)
            );
            userRepository.save(user);
            String resetUrl = buildPasswordResetUrl(token);
            passwordResetEmailService.sendPasswordResetEmail(user, resetUrl);
        });

        return MessageResponse.builder().message(FORGOT_PASSWORD_MESSAGE).build();
    }

    public MessageResponse resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByPasswordResetToken(request.getToken().trim())
                .orElseThrow(() -> new InvalidPasswordResetTokenException("Invalid or expired reset token"));

        if (user.getPasswordResetTokenExpiresAt() == null
                || user.getPasswordResetTokenExpiresAt().isBefore(LocalDateTime.now())) {
            clearPasswordResetToken(user);
            userRepository.save(user);
            throw new InvalidPasswordResetTokenException("Invalid or expired reset token");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        clearPasswordResetToken(user);
        userRepository.save(user);

        return MessageResponse.builder()
                .message("Contrasena actualizada correctamente. Ya puedes iniciar sesion.")
                .build();
    }

    private void clearPasswordResetToken(User user) {
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiresAt(null);
    }

    private AuthResponse buildResponse(User user, String token) {
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationMs() / 1000)
                .user(AuthenticatedUserDTO.builder()
                        .id(user.getId())
                        .fullName(user.getFirstName() + " " + user.getLastName())
                        .email(user.getEmail())
                        .role(user.getRole().name())
                        .build())
                .build();
    }

    private UserDetails toPrincipal(User user) {
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        "ROLE_" + user.getRole().name()
                ))
        );
    }

    private String buildPasswordResetUrl(String token) {
        String baseUrl = passwordResetFrontendUrl.replaceAll("/+$", "");
        return baseUrl + "/reset-password/" + token;
    }

    private String normalizeEmail(String email) {
        return sanitizeText(email).toLowerCase();
    }

    private String sanitizeText(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }
}
