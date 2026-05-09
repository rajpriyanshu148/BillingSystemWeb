package com.billing.repository;

import com.billing.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByActiveTrueOrderByNameAsc();

    List<Product> findAllByOrderByNameAsc();

    List<Product> findByActiveTrueAndNameContainingIgnoreCase(String keyword);

    @Modifying
    @Query("UPDATE Product p SET p.active = false WHERE p.id = :id")
    int softDelete(Long id);

    @Modifying
    @Query("UPDATE Product p SET p.quantity = p.quantity - :qty WHERE p.id = :id AND p.quantity >= :qty")
    int decreaseStock(Long id, int qty);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.active = true")
    long countActive();
}
