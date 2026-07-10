package com.ibnfirnas.service;

import com.ibnfirnas.dto.request.InquiryRequest;
import com.ibnfirnas.dto.response.InquiryResponse;
import com.ibnfirnas.entity.Inquiry;
import com.ibnfirnas.entity.User;
import com.ibnfirnas.entity.enums.InquiryStatus;
import com.ibnfirnas.exception.ResourceNotFoundException;
import com.ibnfirnas.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InquiryService {

    private final InquiryRepository inquiryRepository;

    // ============ toDTO ============
    public InquiryResponse toDTO(Inquiry inquiry) {
        if (inquiry == null) return null;
        return InquiryResponse.builder()
                .id(inquiry.getId())
                .name(inquiry.getName())
                .email(inquiry.getEmail())
                .phone(inquiry.getPhone())
                .subject(inquiry.getSubject())
                .message(inquiry.getMessage())
                .status(inquiry.getStatus() != null
                        ? inquiry.getStatus().name() : null)
                .priority(inquiry.getPriority())
                .createdAt(inquiry.getCreatedAt())
                .build();
    }

    // ============ toEntity ============
    public Inquiry toEntity(InquiryRequest request, User user) {
        if (request == null) return null;
        return Inquiry.builder()
                .user(user)
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .subject(request.getSubject())
                .message(request.getMessage())
                .status(InquiryStatus.OPEN)
                .priority("NORMAL")
                .build();
    }

    // ============ CRUD ============
    public InquiryResponse submitInquiry(InquiryRequest request, User user) {
        Inquiry inquiry = toEntity(request, user);
        return toDTO(inquiryRepository.save(inquiry));
    }

    public List<InquiryResponse> getAllInquiries() {
        return inquiryRepository.findAll()
                .stream().map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<InquiryResponse> getMyInquiries(Long userId) {
        return inquiryRepository.findByUserId(userId)
                .stream().map(this::toDTO)
                .collect(Collectors.toList());
    }

    public InquiryResponse getInquiryById(Long id) {
        Inquiry inquiry = inquiryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inquiry not found with id: " + id));
        return toDTO(inquiry);
    }

    public InquiryResponse markResolved(Long id) {
        Inquiry inquiry = inquiryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inquiry not found with id: " + id));
        inquiry.setStatus(InquiryStatus.RESOLVED);
        return toDTO(inquiryRepository.save(inquiry));
    }
}