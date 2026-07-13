package com.ibnfirnas.controller;

import com.ibnfirnas.dto.response.ApiResponse;
import com.ibnfirnas.entity.Company;
import com.ibnfirnas.exception.ResourceNotFoundException;
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
            @RequestBody Company updatedCompany) {

        Company existing = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Company not found with id: " + id));


        if (updatedCompany.getName() != null)
            existing.setName(updatedCompany.getName());
        if (updatedCompany.getDescription() != null)
            existing.setDescription(updatedCompany.getDescription());
        if (updatedCompany.getPhone() != null)
            existing.setPhone(updatedCompany.getPhone());
        if (updatedCompany.getEmail() != null)
            existing.setEmail(updatedCompany.getEmail());
        if (updatedCompany.getAddress() != null)
            existing.setAddress(updatedCompany.getAddress());
        if (updatedCompany.getGoogleMapsUrl() != null)
            existing.setGoogleMapsUrl(updatedCompany.getGoogleMapsUrl());
        if (updatedCompany.getVision() != null)
            existing.setVision(updatedCompany.getVision());
        if (updatedCompany.getMission() != null)
            existing.setMission(updatedCompany.getMission());
        if (updatedCompany.getLogoUrl() != null)
            existing.setLogoUrl(updatedCompany.getLogoUrl());
        if (updatedCompany.getBannerUrl() != null)
            existing.setBannerUrl(updatedCompany.getBannerUrl());

        return ResponseEntity.ok(ApiResponse.success("Company updated",
                companyRepository.save(existing)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Company>> create(
            @RequestBody Company company) {
        return ResponseEntity.ok(ApiResponse.success("Company created",
                companyRepository.save(company)));
    }
}
