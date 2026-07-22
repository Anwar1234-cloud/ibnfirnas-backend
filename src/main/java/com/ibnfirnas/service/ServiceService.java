package com.ibnfirnas.service;

import com.ibnfirnas.dto.request.ServiceRequest;
import com.ibnfirnas.entity.ServiceEntity;
import com.ibnfirnas.exception.ResourceNotFoundException;
import com.ibnfirnas.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceService {

    private final ServiceRepository serviceRepository;

    public List<ServiceEntity> getAllServices() {
        return serviceRepository.findByIsActiveTrue();
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
                .imageUrl(request.getImageUrl())
                .imagePublicId(request.getImagePublicId())
                .isFeatured(request.getIsFeatured() != null ? request.getIsFeatured() : false)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        return serviceRepository.save(service);
    }

    public ServiceEntity updateService(Long id, ServiceRequest request) {

        ServiceEntity service = getServiceById(id);

        if (request.getName() != null)
            service.setName(request.getName());

        if (request.getDescription() != null)
            service.setDescription(request.getDescription());

        if (request.getShortDescription() != null)
            service.setShortDescription(request.getShortDescription());

        if (request.getImageUrl() != null)
            service.setImageUrl(request.getImageUrl());

        if (request.getImagePublicId() != null)
            service.setImagePublicId(request.getImagePublicId());

        if (request.getIsFeatured() != null)
            service.setIsFeatured(request.getIsFeatured());

        if (request.getIsActive() != null)
            service.setIsActive(request.getIsActive());

        return serviceRepository.save(service);
    }

    public void deleteService(Long id) {
        serviceRepository.deleteById(id);
    }
}