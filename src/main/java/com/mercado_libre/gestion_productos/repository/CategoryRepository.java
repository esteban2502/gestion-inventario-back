package com.mercado_libre.gestion_productos.repository;

import com.mercado_libre.gestion_productos.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}

