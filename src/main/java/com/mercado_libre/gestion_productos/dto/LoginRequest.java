package com.mercado_libre.gestion_productos.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email format is invalid")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    /**
     * {@code seller}: solo cuentas con rol SELLER. {@code admin}: solo ADMIN.
     * Si se omite o va vacío, se interpreta como {@code seller} (compatibilidad).
     */
    @Size(max = 16, message = "portal must not exceed 16 characters")
    @Pattern(regexp = "^$|^(seller|admin)$", message = "portal must be seller or admin")
    private String portal;
}
