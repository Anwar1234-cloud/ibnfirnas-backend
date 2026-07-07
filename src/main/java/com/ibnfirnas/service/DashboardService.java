package com.ibnfirnas.service;

import com.ibnfirnas.entity.enums.InquiryStatus;
import com.ibnfirnas.entity.enums.OrderStatus;
import com.ibnfirnas.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final InquiryRepository inquiryRepository;
    private final ServiceRepository serviceRepository;
    private final GalleryRepository galleryRepository;
    private final BannerRepository bannerRepository;

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();

        // Basic counts
        stats.put("totalUsers", userRepository.count());
        stats.put("totalProducts", productRepository.count());
        stats.put("totalOrders", orderRepository.count());
        stats.put("totalInquiries", inquiryRepository.count());
        stats.put("totalServices", serviceRepository.count());
        stats.put("totalGallery", galleryRepository.count());
        stats.put("totalBanners", bannerRepository.count());

        // Order breakdown
        stats.put("pendingOrders", orderRepository
                .countByStatus(OrderStatus.PENDING));
        stats.put("completedOrders", orderRepository
                .countByStatus(OrderStatus.COMPLETED));
        stats.put("processingOrders", orderRepository
                .countByStatus(OrderStatus.PROCESSING));

        // Inquiry breakdown
        stats.put("openInquiries", inquiryRepository
                .countByStatus(InquiryStatus.OPEN));
        stats.put("resolvedInquiries", inquiryRepository
                .countByStatus(InquiryStatus.RESOLVED));

        // Active counts
        stats.put("activeProducts", productRepository
                .countByIsActiveTrue());
        stats.put("featuredProducts", productRepository
                .countByIsFeaturedTrue());

        return stats;
    }
}