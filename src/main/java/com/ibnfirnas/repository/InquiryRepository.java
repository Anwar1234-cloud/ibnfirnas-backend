package com.ibnfirnas.repository;

import com.ibnfirnas.entity.Inquiry;
import com.ibnfirnas.entity.enums.InquiryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    List<Inquiry> findByUserId(Long userId);
    List<Inquiry> findByStatus(InquiryStatus status);
    long countByStatus(InquiryStatus status);
}