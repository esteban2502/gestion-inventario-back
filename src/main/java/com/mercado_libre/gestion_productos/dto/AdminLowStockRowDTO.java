package com.mercado_libre.gestion_productos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminLowStockRowDTO {

    private Long productId;
    private String name;
    private String categoryName;
    private Integer stock;
    private String vendorEmail;
}
