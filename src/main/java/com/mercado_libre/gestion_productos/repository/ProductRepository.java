package com.mercado_libre.gestion_productos.repository;

import com.mercado_libre.gestion_productos.model.Product;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByStockLessThan(Integer threshold);

    boolean existsByCategoryId(Long categoryId);

    List<Product> findByCategoryId(Long categoryId);
}

