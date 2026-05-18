package com.mercado_libre.gestion_productos.service;

import com.mercado_libre.gestion_productos.dto.AuthResponse;
import com.mercado_libre.gestion_productos.dto.AuthenticatedUserDTO;
import com.mercado_libre.gestion_productos.dto.LoginRequest;
import com.mercado_libre.gestion_productos.dto.RegisterRequest;
import com.mercado_libre.gestion_productos.exception.EmailAlreadyExistsException;
import com.mercado_libre.gestion_productos.model.Role;
import com.mercado_libre.gestion_productos.model.User;
import com.mercado_libre.gestion_productos.repository.UserRepository;
import com.mercado_libre.gestion_productos.security.JwtService;
import java.util.Map;
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

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
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

    private String normalizeEmail(String email) {
        return sanitizeText(email).toLowerCase();
    }

    private String sanitizeText(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }
}
