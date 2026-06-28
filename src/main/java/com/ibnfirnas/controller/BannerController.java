package com.ibnfirnas.controller;

import com.ibnfirnas.dto.response.ApiResponse;
import com.ibnfirnas.entity.Banner;
import com.ibnfirnas.service.BannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/banners")
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Banner>>> getActive() {
        return ResponseEntity.ok(ApiResponse.success("Banners fetched",
                bannerService.getActiveBanners()));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<Banner>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("All banners",
                bannerService.getAllBanners()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Banner>> create(@RequestBody Banner banner) {
        return ResponseEntity.ok(ApiResponse.success("Banner created",
                bannerService.saveBanner(banner)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Banner>> update(
            @PathVariable Long id, @RequestBody Banner banner) {
        return ResponseEntity.ok(ApiResponse.success("Banner updated",
                bannerService.updateBanner(id, banner)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        bannerService.deleteBanner(id);
        return ResponseEntity.ok(ApiResponse.success("Banner deleted", null));
    }
}