package com.mercado_libre.gestion_productos.repository;

import com.mercado_libre.gestion_productos.model.Product;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByOwner_IdAndStockLessThan(Long ownerId, Integer threshold);

    boolean existsByCategory_IdAndOwner_Id(Long categoryId, Long ownerId);

    List<Product> findByOwner_IdAndCategory_Id(Long ownerId, Long categoryId);

    List<Product> findByOwner_Id(Long ownerId);

    Optional<Product> findByIdAndOwner_Id(Long id, Long ownerId);

    boolean existsByIdAndOwner_Id(Long id, Long ownerId);

    long countByOwner_Id(Long ownerId);

    boolean existsByOwner_Id(Long ownerId);

    List<Product> findTop30ByStockLessThanOrderByStockAsc(Integer stock);

    long countByStockLessThan(Integer stock);

    long countByCategoryIsNull();

    @Query("SELECT COALESCE(SUM(p.price * p.stock), 0) FROM Product p")
    BigDecimal sumInventoryValue();

    @Query("SELECT c.name, COUNT(p) FROM Product p JOIN p.category c GROUP BY c.id, c.name")
    List<Object[]> countProductsGroupedByCategory();
}
