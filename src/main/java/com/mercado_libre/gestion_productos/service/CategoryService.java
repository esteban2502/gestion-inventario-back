package com.mercado_libre.gestion_productos.service;

import com.mercado_libre.gestion_productos.dto.CategoryDTO;
import com.mercado_libre.gestion_productos.exception.CategoryHasProductsException;
import com.mercado_libre.gestion_productos.exception.DuplicateCategoryNameException;
import com.mercado_libre.gestion_productos.exception.ResourceNotFoundException;
import com.mercado_libre.gestion_productos.model.Category;
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
        String name = sanitizeCategoryField(dto.getName());
        String description = sanitizeCategoryFieldOptional(dto.getDescription());
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("El nombre de la categoria es obligatorio.");
        }
        if (name.length() < 3) {
            throw new IllegalArgumentException("El nombre debe tener al menos 3 caracteres validos.");
        }
        if (categoryRepository.existsByOwner_IdAndNameIgnoreCase(owner.getId(), name)) {
            throw new DuplicateCategoryNameException("Ya existe una categoria con ese nombre en su cuenta.");
        }

        Category category = Category.builder()
                .name(name)
                .description(StringUtils.hasText(description) ? description : null)
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

        String name = sanitizeCategoryField(dto.getName());
        String description = sanitizeCategoryFieldOptional(dto.getDescription());
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("El nombre de la categoria es obligatorio.");
        }
        if (name.length() < 3) {
            throw new IllegalArgumentException("El nombre debe tener al menos 3 caracteres validos.");
        }
        if (!name.equalsIgnoreCase(category.getName())
                && categoryRepository.existsByOwner_IdAndNameIgnoreCaseAndIdNot(ownerId, name, id)) {
            throw new DuplicateCategoryNameException("Ya existe una categoria con ese nombre en su cuenta.");
        }

        category.setName(name);
        category.setDescription(StringUtils.hasText(description) ? description : null);
        return toDTO(categoryRepository.save(category));
    }

    public void delete(Long id) {
        Long ownerId = currentUserService.getCurrentUserId();
        if (!categoryRepository.existsByIdAndOwner_Id(id, ownerId)) {
            throw new ResourceNotFoundException("Category not found with id " + id);
        }
        if (productRepository.existsByCategory_IdAndOwner_Id(id, ownerId)) {
            throw new CategoryHasProductsException(
                    "No se puede eliminar la categoria porque tiene productos asociados.");
        }
        categoryRepository.deleteById(id);
    }

    /** Letras/numeros y . , - + ( ) [ ] { } " ; el resto se elimina. */
    private String sanitizeCategoryField(String raw) {
        return InventoryTextSanitization.sanitize(raw);
    }

    private String sanitizeCategoryFieldOptional(String raw) {
        String s = sanitizeCategoryField(raw);
        return s;
    }

    private CategoryDTO toDTO(Category category) {
        return CategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }
}
