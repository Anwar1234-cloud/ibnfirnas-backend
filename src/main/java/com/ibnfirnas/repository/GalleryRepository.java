package com.ibnfirnas.repository;

import com.ibnfirnas.entity.Gallery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GalleryRepository extends JpaRepository<Gallery, Long> {
    Page<Gallery> findByIsActiveTrueOrderByDisplayOrderAsc(Pageable pageable);
}
