package com.mercado_libre.gestion_productos.controller;

import com.mercado_libre.gestion_productos.dto.DashboardSummaryDTO;
import com.mercado_libre.gestion_productos.dto.ProductDTO;
import com.mercado_libre.gestion_productos.model.Product;
import com.mercado_libre.gestion_productos.service.DashboardService;
import com.mercado_libre.gestion_productos.service.ProductService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final ProductService productService;

    public DashboardController(DashboardService dashboardService, ProductService productService) {
        this.dashboardService = dashboardService;
        this.productService = productService;
    }

    @GetMapping("/summary")
    public DashboardSummaryDTO summary(@RequestParam(defaultValue = "5") Integer threshold) {
        List<Product> lowStock = dashboardService.getLowStockProducts(threshold);
        List<ProductDTO> lowStockDtos = lowStock.stream().map(productService::toDTO).toList();

        return DashboardSummaryDTO.builder()
                .totalProducts(dashboardService.getTotalProducts())
                .totalCategories(dashboardService.getTotalCategories())
                .lowStockProductsCount((long) lowStockDtos.size())
                .lowStockProducts(lowStockDtos)
                .totalInventoryValue(dashboardService.getTotalInventoryValue())
                .productsPerCategory(dashboardService.getProductsPerCategory())
                .build();
    }
}

