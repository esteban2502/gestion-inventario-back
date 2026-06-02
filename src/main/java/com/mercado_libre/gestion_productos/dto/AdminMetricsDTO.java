package com.mercado_libre.gestion_productos.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminMetricsDTO {

    private Long totalVendors;
    private Long totalProducts;
    private Long totalCategories;
    private Long lowStockProductsCount;
    private BigDecimal totalInventoryValue;
    private List<AdminLowStockRowDTO> lowStockProducts;
    private Map<String, Long> productsPerCategory;
}
