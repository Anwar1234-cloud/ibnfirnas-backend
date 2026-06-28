package com.ibnfirnas.controller;

import com.ibnfirnas.dto.response.ApiResponse;
import com.ibnfirnas.entity.ServiceEntity;
import com.ibnfirnas.service.ServiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceService serviceService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ServiceEntity>>> getAll() {
        return ResponseEntity.ok(
                ApiResponse.success("Services fetched",
                        serviceService.getAllServices()));
    }

    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<ServiceEntity>>> getFeatured() {
        return ResponseEntity.ok(
                ApiResponse.success("Featured services",
                        serviceService.getFeaturedServices()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ServiceEntity>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success("Service fetched",
                        serviceService.getServiceById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ServiceEntity>> create(
            @RequestBody ServiceEntity service) {
        return ResponseEntity.ok(
                ApiResponse.success("Service created",
                        serviceService.saveService(service)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        serviceService.deleteService(id);
        return ResponseEntity.ok(ApiResponse.success("Service deleted", null));
    }
}