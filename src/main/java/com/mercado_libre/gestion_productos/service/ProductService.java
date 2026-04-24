package com.mercado_libre.gestion_productos.service;

import com.mercado_libre.gestion_productos.dto.ProductDTO;
import com.mercado_libre.gestion_productos.exception.ResourceNotFoundException;
import com.mercado_libre.gestion_productos.model.Category;
import com.mercado_libre.gestion_productos.model.Product;
import com.mercado_libre.gestion_productos.repository.CategoryRepository;
import com.mercado_libre.gestion_productos.repository.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public ProductDTO create(ProductDTO dto) {
        Category category = getExistingCategory(dto.getCategoryId());
        Product product = Product.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .stock(dto.getStock())
                .category(category)
                .build();
        return toDTO(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> findAll(Long categoryId) {
        List<Product> products = categoryId == null
                ? productRepository.findAll()
                : productRepository.findByCategoryId(categoryId);
        return products.stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public ProductDTO findById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + id));
        return toDTO(product);
    }

    public ProductDTO update(Long id, ProductDTO dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + id));
        Category category = getExistingCategory(dto.getCategoryId());

        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock());
        product.setCategory(category);
        return toDTO(productRepository.save(product));
    }

    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id " + id);
        }
        productRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Product> findLowStock(Integer threshold) {
        return productRepository.findByStockLessThan(threshold);
    }

    @Transactional(readOnly = true)
    public Long countProducts() {
        return productRepository.count();
    }

    @Transactional(readOnly = true)
    public List<Product> findAllEntities() {
        return productRepository.findAll();
    }

    private Category getExistingCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id " + categoryId));
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

