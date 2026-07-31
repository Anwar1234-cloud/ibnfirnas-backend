package com.ibnfirnas.service;

import com.ibnfirnas.dto.request.OrderRequest;
import com.ibnfirnas.dto.response.OrderResponse;
import com.ibnfirnas.entity.*;
import com.ibnfirnas.entity.enums.OrderStatus;
import com.ibnfirnas.exception.ResourceNotFoundException;
import com.ibnfirnas.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Order Service Tests")
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private OrderService orderService;

    private User mockUser;
    private Product mockProduct;
    private Order mockOrder;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .email("anwar@test.com")
                .build();

        mockProduct = Product.builder()
                .id(1L)
                .name("Automatic Gate")
                .price(new BigDecimal("2500.00"))
                .build();

        mockOrder = Order.builder()
                .id(1L)
                .user(mockUser)
                .orderNumber("IBN-ABC123")
                .totalAmount(new BigDecimal("2500.00"))
                .status(OrderStatus.PENDING)
                .items(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Create order — success")
    void createOrder_Success() {
        OrderRequest.OrderItemRequest itemRequest =
                new OrderRequest.OrderItemRequest(1L, 1);
        OrderRequest request = new OrderRequest(
                List.of(itemRequest), "Qatar", "Cash", null);

        when(userRepository.findByEmail("anwar@test.com"))
                .thenReturn(Optional.of(mockUser));
        when(productRepository.findById(1L))
                .thenReturn(Optional.of(mockProduct));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);

        OrderResponse response = orderService.createOrder(request, "anwar@test.com");

        assertNotNull(response);
        assertEquals("IBN-ABC123", response.getOrderNumber());
        assertEquals("PENDING", response.getStatus());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("Create order — user not found")
    void createOrder_UserNotFound() {
        OrderRequest request = new OrderRequest(List.of(), "Qatar", "Cash", null);
        when(userRepository.findByEmail("unknown@test.com"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> orderService.createOrder(request, "unknown@test.com"));
    }

    @Test
    @DisplayName("Get user orders — success")
    void getUserOrders_Success() {
        when(userRepository.findByEmail("anwar@test.com"))
                .thenReturn(Optional.of(mockUser));
        when(orderRepository.findByUserId(1L))
                .thenReturn(Arrays.asList(mockOrder));

        List<OrderResponse> orders = orderService.getUserOrders("anwar@test.com");

        assertNotNull(orders);
        assertEquals(1, orders.size());
        assertEquals("IBN-ABC123", orders.get(0).getOrderNumber());
    }

    @Test
    @DisplayName("Update order status — success")
    void updateOrderStatus_Success() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);

        OrderResponse response = orderService.updateOrderStatus(1L, OrderStatus.PROCESSING);

        assertNotNull(response);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("Get order by ID — not found")
    void getOrderById_NotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> orderService.getOrderById(999L, "anwar@test.com", false));
    }

    @Test
    @DisplayName("Get order by ID — owner can view their own order")
    void getOrderById_Owner_Success() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        OrderResponse response = orderService.getOrderById(1L, "anwar@test.com", false);

        assertNotNull(response);
        assertEquals("IBN-ABC123", response.getOrderNumber());
    }

    @Test
    @DisplayName("Get order by ID — non-owner is denied (IDOR fix)")
    void getOrderById_NonOwner_ThrowsAccessDenied() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        assertThrows(AccessDeniedException.class,
                () -> orderService.getOrderById(1L, "someoneelse@test.com", false));
    }

    @Test
    @DisplayName("Get order by ID — admin can view any order")
    void getOrderById_Admin_CanViewAnyOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        OrderResponse response = orderService.getOrderById(1L, "admin@test.com", true);

        assertNotNull(response);
        assertEquals("IBN-ABC123", response.getOrderNumber());
    }
}