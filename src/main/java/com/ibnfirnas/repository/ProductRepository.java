package com.ibnfirnas.repository;

import com.ibnfirnas.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByIsFeaturedTrue();
    List<Product> findByCategoryId(Long categoryId);
    List<Product> findByIsActiveTrue();
}