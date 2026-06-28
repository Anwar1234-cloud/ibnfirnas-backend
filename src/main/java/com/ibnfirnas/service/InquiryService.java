package com.ibnfirnas.service;

import com.ibnfirnas.dto.request.InquiryRequest;
import com.ibnfirnas.entity.Inquiry;
import com.ibnfirnas.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InquiryService {

    private final InquiryRepository inquiryRepository;

    public Inquiry submitInquiry(InquiryRequest request) {
        Inquiry inquiry = Inquiry.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .subject(request.getSubject())
                .message(request.getMessage())
                .build();
        return inquiryRepository.save(inquiry);
    }
}
