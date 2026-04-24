package com.mercado_libre.gestion_productos.service;

import com.mercado_libre.gestion_productos.dto.CategoryDTO;
import com.mercado_libre.gestion_productos.exception.CategoryHasProductsException;
import com.mercado_libre.gestion_productos.exception.ResourceNotFoundException;
import com.mercado_libre.gestion_productos.model.Category;
import com.mercado_libre.gestion_productos.repository.CategoryRepository;
import com.mercado_libre.gestion_productos.repository.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    public CategoryDTO create(CategoryDTO dto) {
        Category category = Category.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .build();
        return toDTO(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<CategoryDTO> findAll() {
        return categoryRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public CategoryDTO findById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id " + id));
        return toDTO(category);
    }

    public CategoryDTO update(Long id, CategoryDTO dto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id " + id));

        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        return toDTO(categoryRepository.save(category));
    }

    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category not found with id " + id);
        }
        if (productRepository.existsByCategoryId(id)) {
            throw new CategoryHasProductsException("Category cannot be deleted because it has associated products.");
        }
        categoryRepository.deleteById(id);
    }

    private CategoryDTO toDTO(Category category) {
        return CategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }
}

