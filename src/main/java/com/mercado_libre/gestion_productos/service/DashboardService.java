package com.mercado_libre.gestion_productos.service;

import com.mercado_libre.gestion_productos.model.Category;
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
    private final CurrentUserService currentUserService;

    public DashboardService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            CurrentUserService currentUserService
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.currentUserService = currentUserService;
    }

    public Long getTotalProducts() {
        return productRepository.countByOwner_Id(currentUserService.getCurrentUserId());
    }

    public Long getTotalCategories() {
        return categoryRepository.countByOwner_Id(currentUserService.getCurrentUserId());
    }

    public List<Product> getLowStockProducts(Integer threshold) {
        Long ownerId = currentUserService.getCurrentUserId();
        return productRepository.findByOwner_IdAndStockLessThan(ownerId, threshold);
    }

    public BigDecimal getTotalInventoryValue() {
        Long ownerId = currentUserService.getCurrentUserId();
        return productRepository.findByOwner_Id(ownerId).stream()
                .map(product -> product.getPrice().multiply(BigDecimal.valueOf(product.getStock())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Map<String, Long> getProductsPerCategory() {
        Long ownerId = currentUserService.getCurrentUserId();
        Map<String, Long> map = new LinkedHashMap<>();
        List<Category> categories = categoryRepository.findByOwner_Id(ownerId);
        for (Category category : categories) {
            long count = productRepository.findByOwner_IdAndCategory_Id(ownerId, category.getId()).size();
            map.put(category.getName(), count);
        }
        return map;
    }
}
