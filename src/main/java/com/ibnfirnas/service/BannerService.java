package com.ibnfirnas.service;

import com.ibnfirnas.entity.Banner;
import com.ibnfirnas.exception.ResourceNotFoundException;
import com.ibnfirnas.repository.BannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BannerService {

    private final BannerRepository bannerRepository;

    public List<Banner> getActiveBanners() {
        return bannerRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
    }

    public List<Banner> getAllBanners() {
        return bannerRepository.findAll();
    }

    public Banner saveBanner(Banner banner) {
        return bannerRepository.save(banner);
    }

    public Banner updateBanner(Long id, Banner updated) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found"));
        banner.setTitle(updated.getTitle());
        banner.setSubtitle(updated.getSubtitle());
        banner.setImageUrl(updated.getImageUrl());
        banner.setCtaText(updated.getCtaText());
        banner.setCtaLink(updated.getCtaLink());
        banner.setIsActive(updated.getIsActive());
        banner.setDisplayOrder(updated.getDisplayOrder());
        return bannerRepository.save(banner);
    }

    public void deleteBanner(Long id) {
        bannerRepository.deleteById(id);
    }
}