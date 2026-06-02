package com.mercado_libre.gestion_productos.repository;

import com.mercado_libre.gestion_productos.model.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByOwner_Id(Long ownerId);

    Optional<Category> findByIdAndOwner_Id(Long id, Long ownerId);

    long countByOwner_Id(Long ownerId);

    boolean existsByIdAndOwner_Id(Long id, Long ownerId);

    boolean existsByOwner_IdAndNameIgnoreCase(Long ownerId, String name);

    boolean existsByOwner_IdAndNameIgnoreCaseAndIdNot(Long ownerId, String name, Long id);

    boolean existsByOwner_Id(Long ownerId);
}
