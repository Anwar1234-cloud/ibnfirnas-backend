package com.ibnfirnas.controller;

import com.ibnfirnas.dto.response.ApiResponse;
import com.ibnfirnas.entity.Company;
import com.ibnfirnas.repository.CompanyRepository;
import com.ibnfirnas.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/company")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;
    private final CompanyRepository companyRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<Company>> getCompanyInfo() {
        return ResponseEntity.ok(
                ApiResponse.success("Company info fetched", companyService.getCompanyInfo()));
    }
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Company>> update(
            @PathVariable Long id,
            @RequestBody Company company) {
        company.setId(id);
        return ResponseEntity.ok(ApiResponse.success("Company updated",
                companyRepository.save(company)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Company>> create(
            @RequestBody Company company) {
        return ResponseEntity.ok(ApiResponse.success("Company created",
                companyRepository.save(company)));
    }
}
