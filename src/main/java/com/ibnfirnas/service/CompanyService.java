package com.ibnfirnas.service;

import com.ibnfirnas.entity.Company;
import com.ibnfirnas.exception.ResourceNotFoundException;
import com.ibnfirnas.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;

    public Company getCompanyInfo() {
        return companyRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Company info not found"));
    }
}