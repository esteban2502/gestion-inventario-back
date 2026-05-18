package com.mercado_libre.gestion_productos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthenticatedUserDTO {
    private Long id;
    private String fullName;
    private String email;
    private String role;
}
