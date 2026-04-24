package com.mercado_libre.gestion_productos.service;

import com.mercado_libre.gestion_productos.model.Product;
import com.mercado_libre.gestion_productos.repository.CategoryRepository;
import com.mercado_libre.gestion_productos.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public DashboardService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public Long getTotalProducts() {
        return productRepository.count();
    }

    public Long getTotalCategories() {
        return categoryRepository.count();
    }

    public List<Product> getLowStockProducts(Integer threshold) {
        return productRepository.findByStockLessThan(threshold);
    }

    public BigDecimal getTotalInventoryValue() {
        return productRepository.findAll().stream()
                .map(product -> product.getPrice().multiply(BigDecimal.valueOf(product.getStock())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Map<String, Long> getProductsPerCategory() {
        Map<String, Long> map = new LinkedHashMap<>();
        categoryRepository.findAll().forEach(category ->
                map.put(category.getName(), (long) productRepository.findByCategoryId(category.getId()).size()));
        return map;
    }
}

