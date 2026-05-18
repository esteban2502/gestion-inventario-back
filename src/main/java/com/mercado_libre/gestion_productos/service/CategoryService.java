package com.mercado_libre.gestion_productos.service;

import com.mercado_libre.gestion_productos.dto.CategoryDTO;
import com.mercado_libre.gestion_productos.exception.CategoryHasProductsException;
import com.mercado_libre.gestion_productos.exception.ResourceNotFoundException;
import com.mercado_libre.gestion_productos.model.Category;
import com.mercado_libre.gestion_productos.model.User;
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
    private final CurrentUserService currentUserService;

    public CategoryService(
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            CurrentUserService currentUserService
    ) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.currentUserService = currentUserService;
    }

    public CategoryDTO create(CategoryDTO dto) {
        User owner = currentUserService.getCurrentUser();
        Category category = Category.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .owner(owner)
                .build();
        return toDTO(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<CategoryDTO> findAll() {
        Long ownerId = currentUserService.getCurrentUserId();
        return categoryRepository.findByOwner_Id(ownerId).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public CategoryDTO findById(Long id) {
        Long ownerId = currentUserService.getCurrentUserId();
        Category category = categoryRepository.findByIdAndOwner_Id(id, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id " + id));
        return toDTO(category);
    }

    public CategoryDTO update(Long id, CategoryDTO dto) {
        Long ownerId = currentUserService.getCurrentUserId();
        Category category = categoryRepository.findByIdAndOwner_Id(id, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id " + id));

        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        return toDTO(categoryRepository.save(category));
    }

    public void delete(Long id) {
        Long ownerId = currentUserService.getCurrentUserId();
        if (!categoryRepository.existsByIdAndOwner_Id(id, ownerId)) {
            throw new ResourceNotFoundException("Category not found with id " + id);
        }
        if (productRepository.existsByCategory_IdAndOwner_Id(id, ownerId)) {
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
