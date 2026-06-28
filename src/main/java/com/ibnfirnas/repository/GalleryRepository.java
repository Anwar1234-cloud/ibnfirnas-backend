package com.ibnfirnas.repository;

import com.ibnfirnas.entity.Gallery;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GalleryRepository extends JpaRepository<Gallery, Long> {
    List<Gallery> findByIsActiveTrueOrderByDisplayOrderAsc();
}
