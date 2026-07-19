package com.ibnfirnas.repository;

import com.ibnfirnas.entity.ServiceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ServiceRepository extends JpaRepository<ServiceEntity, Long> {
    List<ServiceEntity> findByIsFeaturedTrue();
    Page<ServiceEntity> findByIsActiveTrue(Pageable pageable);
}