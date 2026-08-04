package com.ibnfirnas.service;

import com.ibnfirnas.dto.request.OrderRequest;
import com.ibnfirnas.dto.response.OrderResponse;
import com.ibnfirnas.entity.*;
import com.ibnfirnas.entity.enums.OrderStatus;
import com.ibnfirnas.exception.ResourceNotFoundException;
import com.ibnfirnas.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    // ============ toDTO ============
    public OrderResponse toDTO(Order order) {
        if (order == null) return null;

        List<OrderResponse.OrderItemResponse> itemDTOs = null;
        if (order.getItems() != null) {
            itemDTOs = order.getItems().stream()
                    .map(item -> {
                        String imageUrl = null;
                        if (item.getProduct() != null
                                && item.getProduct().getImages() != null
                                && !item.getProduct().getImages().isEmpty()) {
                            imageUrl = item.getProduct().getImages()
                                    .get(0).getImageUrl();
                        }
                        return OrderResponse.OrderItemResponse.builder()
                                .id(item.getId())
                                .productId(item.getProduct() != null
                                        ? item.getProduct().getId() : null)
                                .productName(item.getProduct() != null
                                        ? item.getProduct().getName() : null)
                                .productImage(imageUrl)
                                .quantity(item.getQuantity())
                                .unitPrice(item.getUnitPrice())
                                .totalPrice(item.getTotalPrice())
                                .build();
                    })
                    .collect(Collectors.toList());
        }

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus() != null
                        ? order.getStatus().name() : null)
                .paymentStatus(order.getPaymentStatus() != null
                        ? order.getPaymentStatus().name() : null)
                .shippingAddress(order.getShippingAddress())
                .paymentMethod(order.getPaymentMethod())
                .trackingNumber(order.getTrackingNumber())
                .items(itemDTOs)
                .createdAt(order.getCreatedAt())
                .build();
    }

    // ============ CRUD ============
    public OrderResponse createOrder(OrderRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + userEmail));

        Order order = Order.builder()
                .user(user)
                .orderNumber("IBN-" + UUID.randomUUID()
                        .toString().substring(0, 8).toUpperCase())
                .shippingAddress(request.getShippingAddress())
                .paymentMethod(request.getPaymentMethod())
                .notes(request.getNotes())
                .status(OrderStatus.PENDING)
                .build();

        List<OrderItem> items = request.getItems().stream()
                .map(itemReq -> {
                    Product product = productRepository
                            .findById(itemReq.getProductId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Product not found: " + itemReq.getProductId()));

                    BigDecimal unitPrice = product.getDiscountPrice() != null
                            ? product.getDiscountPrice() : product.getPrice();
                    BigDecimal totalPrice = unitPrice.multiply(
                            BigDecimal.valueOf(itemReq.getQuantity()));

                    return OrderItem.builder()
                            .order(order)
                            .product(product)
                            .quantity(itemReq.getQuantity())
                            .unitPrice(unitPrice)
                            .totalPrice(totalPrice)
                            .build();
                }).collect(Collectors.toList());

        BigDecimal totalAmount = items.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setItems(items);
        order.setTotalAmount(totalAmount);

        return toDTO(orderRepository.save(order));
    }

    public List<OrderResponse> getUserOrders(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + userEmail));
        return orderRepository.findByUserId(user.getId())
                .stream().map(this::toDTO)
                .collect(Collectors.toList());
    }

    public OrderResponse getOrderById(Long id, String requesterEmail, boolean isAdmin) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with id: " + id));
        boolean isOwner = order.getUser() != null
                && order.getUser().getEmail().equals(requesterEmail);
        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("You do not have permission to view this order");
        }
        return toDTO(order);
    }

    public OrderResponse updateOrderStatus(Long id, OrderStatus status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with id: " + id));
        order.setStatus(status);
        return toDTO(orderRepository.save(order));
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll()
                .stream().map(this::toDTO)
                .collect(Collectors.toList());
    }
}