package com.ibnfirnas.controller;

import com.ibnfirnas.dto.request.ServiceRequest;
import com.ibnfirnas.dto.response.ApiResponse;
import com.ibnfirnas.dto.response.PageResponse;
import com.ibnfirnas.entity.ServiceEntity;
import com.ibnfirnas.service.ServiceService;
import jakarta.validation.Valid;
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
    public ResponseEntity<ApiResponse<PageResponse<ServiceEntity>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                ApiResponse.success("Services fetched",
                        serviceService.getAllServices(page, size)));
    }

    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<ServiceEntity>>> getFeatured() {
        return ResponseEntity.ok(
                ApiResponse.success("Featured services",
                        serviceService.getFeaturedServices()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ServiceEntity>> getById(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success("Service fetched",
                        serviceService.getServiceById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ServiceEntity>> create(
            @Valid @RequestBody ServiceRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Service created",
                        serviceService.createService(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ServiceEntity>> update(
            @PathVariable Long id,
            @Valid @RequestBody ServiceRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Service updated",
                        serviceService.updateService(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id) {
        serviceService.deleteService(id);
        return ResponseEntity.ok(
                ApiResponse.success("Service deleted", null));
    }
}