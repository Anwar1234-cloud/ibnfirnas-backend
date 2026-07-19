package com.ibnfirnas.service;

import com.ibnfirnas.dto.request.ServiceRequest; // Ye import zaroori hai
import com.ibnfirnas.dto.response.PageResponse;
import com.ibnfirnas.entity.ServiceEntity;
import com.ibnfirnas.exception.ResourceNotFoundException;
import com.ibnfirnas.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final ServiceRepository serviceRepository;

    public PageResponse<ServiceEntity> getAllServices(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), clampPageSize(size));
        return PageResponse.from(serviceRepository.findByIsActiveTrue(pageable));
    }

    private int clampPageSize(int size) {
        if (size < 1) return DEFAULT_PAGE_SIZE;
        return Math.min(size, MAX_PAGE_SIZE);
    }

    public List<ServiceEntity> getFeaturedServices() {
        return serviceRepository.findByIsFeaturedTrue();
    }

    public ServiceEntity getServiceById(Long id) {
        return serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
    }

    public ServiceEntity saveService(ServiceEntity service) {
        return serviceRepository.save(service);
    }

    public ServiceEntity createService(ServiceRequest request) {
        ServiceEntity service = ServiceEntity.builder()
                .name(request.getName())
                .description(request.getDescription())
                .shortDescription(request.getShortDescription())
                .iconUrl(request.getIconUrl())
                .imageUrl(request.getImageUrl())
                .isFeatured(request.getIsFeatured() != null ? request.getIsFeatured() : false)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .build();
        return serviceRepository.save(service);
    }

    public ServiceEntity updateService(Long id, ServiceRequest request) {
        ServiceEntity service = getServiceById(id);
        if (request.getName() != null) service.setName(request.getName());
        if (request.getDescription() != null) service.setDescription(request.getDescription());
        if (request.getShortDescription() != null) service.setShortDescription(request.getShortDescription());
        if (request.getIconUrl() != null) service.setIconUrl(request.getIconUrl());
        if (request.getImageUrl() != null) service.setImageUrl(request.getImageUrl());
        if (request.getIsFeatured() != null) service.setIsFeatured(request.getIsFeatured());
        if (request.getIsActive() != null) service.setIsActive(request.getIsActive());
        if (request.getDisplayOrder() != null) service.setDisplayOrder(request.getDisplayOrder());
        return serviceRepository.save(service);
    }

    public void deleteService(Long id) {
        serviceRepository.deleteById(id);
    }
}
