package com.ibnfirnas.controller;

import com.ibnfirnas.dto.response.ApiResponse;
import com.ibnfirnas.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getStats() {
        return ResponseEntity.ok(
                ApiResponse.success("Dashboard stats",
                        dashboardService.getStats()));
    }
}