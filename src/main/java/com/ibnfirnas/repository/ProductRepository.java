package com.ibnfirnas.repository;

import com.ibnfirnas.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByIsFeaturedTrue();
    List<Product> findByCategoryId(Long categoryId);
    Page<Product> findByIsActiveTrue(Pageable pageable);
    Page<Product> findByIsActiveTrueAndCategoryId(Long categoryId, Pageable pageable);
    long countByIsActiveTrue();
    long countByIsFeaturedTrue();
}