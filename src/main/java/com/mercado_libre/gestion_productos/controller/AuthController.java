package com.mercado_libre.gestion_productos.controller;

import com.mercado_libre.gestion_productos.dto.AuthResponse;
import com.mercado_libre.gestion_productos.dto.ForgotPasswordRequest;
import com.mercado_libre.gestion_productos.dto.LoginRequest;
import com.mercado_libre.gestion_productos.dto.MessageResponse;
import com.mercado_libre.gestion_productos.dto.RegisterRequest;
import com.mercado_libre.gestion_productos.dto.ResetPasswordRequest;
import com.mercado_libre.gestion_productos.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }
}
