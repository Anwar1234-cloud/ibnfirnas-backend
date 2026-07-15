package com.ibnfirnas.service;

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

    public ServiceEntity updateService(Long id, ServiceEntity request) {
        ServiceEntity service = getServiceById(id);

        if (request.getName() != null) service.setName(request.getName());
        if (request.getSlug() != null) service.setSlug(request.getSlug());
        if (request.getDescription() != null) service.setDescription(request.getDescription());
        if (request.getShortDescription() != null)
            service.setShortDescription(request.getShortDescription());
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
