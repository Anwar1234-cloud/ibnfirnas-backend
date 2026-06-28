package com.ibnfirnas.repository;

import com.ibnfirnas.entity.Newsletter;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface NewsletterRepository extends JpaRepository<Newsletter, Long> {
    Optional<Newsletter> findByEmail(String email);
    boolean existsByEmail(String email);
}