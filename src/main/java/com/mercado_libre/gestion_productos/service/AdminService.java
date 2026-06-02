package com.mercado_libre.gestion_productos.service;

import com.mercado_libre.gestion_productos.dto.AdminLowStockRowDTO;
import com.mercado_libre.gestion_productos.dto.AdminMetricsDTO;
import com.mercado_libre.gestion_productos.dto.AdminVendorUpdateRequest;
import com.mercado_libre.gestion_productos.dto.VendorAdminDTO;
import com.mercado_libre.gestion_productos.exception.EmailAlreadyExistsException;
import com.mercado_libre.gestion_productos.exception.ResourceNotFoundException;
import com.mercado_libre.gestion_productos.exception.VendorHasInventoryException;
import com.mercado_libre.gestion_productos.model.Product;
import com.mercado_libre.gestion_productos.model.Role;
import com.mercado_libre.gestion_productos.model.User;
import com.mercado_libre.gestion_productos.repository.CategoryRepository;
import com.mercado_libre.gestion_productos.repository.ProductRepository;
import com.mercado_libre.gestion_productos.repository.UserRepository;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class AdminService {

    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 5;

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(
            UserRepository userRepository,
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public AdminMetricsDTO getMetrics(Integer threshold) {
        int t = threshold == null || threshold < 0 ? DEFAULT_LOW_STOCK_THRESHOLD : threshold;
        long vendors = userRepository.countByRole(Role.SELLER);
        long products = productRepository.count();
        long categories = categoryRepository.count();
        long lowCount = productRepository.countByStockLessThan(t);

        List<Product> low = productRepository.findTop30ByStockLessThanOrderByStockAsc(t);
        List<AdminLowStockRowDTO> lowRows = low.stream()
                .map(p -> AdminLowStockRowDTO.builder()
                        .productId(p.getId())
                        .name(p.getName())
                        .categoryName(p.getCategory() != null ? p.getCategory().getName() : "-")
                        .stock(p.getStock())
                        .vendorEmail(p.getOwner() != null ? p.getOwner().getEmail() : "-")
                        .build())
                .toList();

        BigDecimal inventoryValue = productRepository.sumInventoryValue();
        if (inventoryValue == null) {
            inventoryValue = BigDecimal.ZERO;
        }

        Map<String, Long> perCategory = new LinkedHashMap<>();
        for (Object[] row : productRepository.countProductsGroupedByCategory()) {
            String name = (String) row[0];
            long count = ((Number) row[1]).longValue();
            perCategory.put(name, count);
        }
        long withoutCategory = productRepository.countByCategoryIsNull();
        if (withoutCategory > 0) {
            perCategory.put("Sin categoria", withoutCategory);
        }

        return AdminMetricsDTO.builder()
                .totalVendors(vendors)
                .totalProducts(products)
                .totalCategories(categories)
                .lowStockProductsCount(lowCount)
                .totalInventoryValue(inventoryValue)
                .lowStockProducts(lowRows)
                .productsPerCategory(perCategory)
                .build();
    }

    @Transactional(readOnly = true)
    public List<VendorAdminDTO> listVendors() {
        return userRepository.findByRoleOrderByCreatedAtDesc(Role.SELLER).stream()
                .map(this::toVendorDto)
                .toList();
    }

    public VendorAdminDTO updateVendor(Long id, AdminVendorUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + id));
        if (user.getRole() != Role.SELLER) {
            throw new ResourceNotFoundException("User not found with id " + id);
        }
        String newEmail = normalizeEmail(request.getEmail());
        if (!newEmail.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
            throw new EmailAlreadyExistsException("Email is already registered");
        }
        user.setFirstName(sanitize(request.getFirstName()));
        user.setLastName(sanitize(request.getLastName()));
        user.setAddress(sanitize(request.getAddress()));
        user.setPhone(sanitizePhone(request.getPhone()));
        user.setEmail(newEmail);
        if (StringUtils.hasText(request.getNewPassword())) {
            if (request.getNewPassword().length() < 8) {
                throw new IllegalArgumentException("La nueva contrasena debe tener al menos 8 caracteres.");
            }
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        }
        return toVendorDto(userRepository.save(user));
    }

    public void deleteVendor(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + id));
        if (user.getRole() != Role.SELLER) {
            throw new ResourceNotFoundException("User not found with id " + id);
        }
        if (productRepository.existsByOwner_Id(user.getId()) || categoryRepository.existsByOwner_Id(user.getId())) {
            throw new VendorHasInventoryException(
                    "No se puede eliminar el vendedor porque tiene productos o categorias asociadas.");
        }
        userRepository.delete(user);
    }

    private VendorAdminDTO toVendorDto(User user) {
        return VendorAdminDTO.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(user.getAddress())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private String normalizeEmail(String email) {
        return sanitize(email).toLowerCase();
    }

    private String sanitize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private String sanitizePhone(String value) {
        return sanitize(value).replaceAll("[^0-9+\\-\\s]", "");
    }
}
