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
public class ForgotPasswordRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email format is invalid")
    @Size(max = 150, message = "Email must not exceed 150 characters")
    private String email;

    /**
     * Ruta interna permitida tras restablecer la contraseña (lista blanca en servidor).
     * Vacío u omitido: solo se usa el correo; el enlace del mail no lleva query.
     */
    @Size(max = 64, message = "returnUrl must not exceed 64 characters")
    @Pattern(
            regexp = "^$|^(/admin/ingreso|/login)$",
            message = "returnUrl must be /admin/ingreso or /login"
    )
    private String returnUrl;
}
