package com.mercado_libre.gestion_productos.service;

import com.mercado_libre.gestion_productos.dto.ProductDTO;
import com.mercado_libre.gestion_productos.exception.ResourceNotFoundException;
import com.mercado_libre.gestion_productos.model.Category;
import com.mercado_libre.gestion_productos.model.Product;
import com.mercado_libre.gestion_productos.model.User;
import com.mercado_libre.gestion_productos.repository.CategoryRepository;
import com.mercado_libre.gestion_productos.repository.ProductRepository;
import com.mercado_libre.gestion_productos.util.InventoryTextSanitization;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CurrentUserService currentUserService;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            CurrentUserService currentUserService
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.currentUserService = currentUserService;
    }

    public ProductDTO create(ProductDTO dto) {
        User owner = currentUserService.getCurrentUser();
        Long ownerId = owner.getId();
        String name = sanitizeProductText(dto.getName());
        if (!StringUtils.hasText(name) || name.length() < 3) {
            throw new IllegalArgumentException("El nombre del producto debe tener al menos 3 caracteres validos.");
        }
        Category category = getCategoryForOwner(dto.getCategoryId(), ownerId);
        Product product = Product.builder()
                .name(name)
                .description(sanitizeProductDescription(dto.getDescription()))
                .price(dto.getPrice())
                .stock(dto.getStock())
                .category(category)
                .owner(owner)
                .build();
        return toDTO(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> findAll(Long categoryId) {
        Long ownerId = currentUserService.getCurrentUserId();
        List<Product> products = categoryId == null
                ? productRepository.findByOwner_Id(ownerId)
                : productRepository.findByOwner_IdAndCategory_Id(ownerId, categoryId);
        return products.stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public ProductDTO findById(Long id) {
        Long ownerId = currentUserService.getCurrentUserId();
        Product product = productRepository.findByIdAndOwner_Id(id, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + id));
        return toDTO(product);
    }

    public ProductDTO update(Long id, ProductDTO dto) {
        Long ownerId = currentUserService.getCurrentUserId();
        Product product = productRepository.findByIdAndOwner_Id(id, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + id));
        Category category = getCategoryForOwner(dto.getCategoryId(), ownerId);

        String name = sanitizeProductText(dto.getName());
        if (!StringUtils.hasText(name) || name.length() < 3) {
            throw new IllegalArgumentException("El nombre del producto debe tener al menos 3 caracteres validos.");
        }

        product.setName(name);
        product.setDescription(sanitizeProductDescription(dto.getDescription()));
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock());
        product.setCategory(category);
        return toDTO(productRepository.save(product));
    }

    public void delete(Long id) {
        Long ownerId = currentUserService.getCurrentUserId();
        if (!productRepository.existsByIdAndOwner_Id(id, ownerId)) {
            throw new ResourceNotFoundException("Product not found with id " + id);
        }
        productRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Product> findLowStock(Integer threshold) {
        Long ownerId = currentUserService.getCurrentUserId();
        return productRepository.findByOwner_IdAndStockLessThan(ownerId, threshold);
    }

    @Transactional(readOnly = true)
    public Long countProducts() {
        return productRepository.countByOwner_Id(currentUserService.getCurrentUserId());
    }

    @Transactional(readOnly = true)
    public List<Product> findAllEntities() {
        return productRepository.findByOwner_Id(currentUserService.getCurrentUserId());
    }

    private Category getCategoryForOwner(Long categoryId, Long ownerId) {
        return categoryRepository.findByIdAndOwner_Id(categoryId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id " + categoryId));
    }

    private String sanitizeProductText(String raw) {
        return InventoryTextSanitization.sanitize(raw);
    }

    private String sanitizeProductDescription(String raw) {
        String s = sanitizeProductText(raw);
        return StringUtils.hasText(s) ? s : null;
    }

    public ProductDTO toDTO(Product product) {
        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .build();
    }
}
