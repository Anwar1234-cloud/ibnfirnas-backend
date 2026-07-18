package com.ibnfirnas.service;

import com.ibnfirnas.dto.request.InquiryRequest;
import com.ibnfirnas.dto.response.InquiryResponse;
import com.ibnfirnas.entity.Inquiry;
import com.ibnfirnas.entity.enums.InquiryStatus;
import com.ibnfirnas.exception.ResourceNotFoundException;
import com.ibnfirnas.repository.InquiryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Inquiry Service Tests")
class InquiryServiceTest {

    @Mock private InquiryRepository inquiryRepository;
    @InjectMocks private InquiryService inquiryService;

    private Inquiry mockInquiry;
    private InquiryRequest inquiryRequest;

    @BeforeEach
    void setUp() {
        mockInquiry = Inquiry.builder()
                .id(1L)
                .name("Test User")
                .email("test@test.com")
                .phone("1234567890")
                .subject("Product Inquiry")
                .message("I want to know about gates")
                .status(InquiryStatus.OPEN)
                .priority("NORMAL")
                .build();

        inquiryRequest = new InquiryRequest(
                "Test User", "test@test.com",
                "1234567890", "Product Inquiry",
                "I want to know about gates");
    }

    @Test
    @DisplayName("Submit inquiry — success")
    void submitInquiry_Success() {
        when(inquiryRepository.save(any(Inquiry.class))).thenReturn(mockInquiry);

        InquiryResponse response = inquiryService.submitInquiry(inquiryRequest,null);

        assertNotNull(response);
        assertEquals("Test User", response.getName());
        assertEquals("test@test.com", response.getEmail());
        assertEquals("OPEN", response.getStatus());
        verify(inquiryRepository, times(1)).save(any(Inquiry.class));
    }

    @Test
    @DisplayName("Get all inquiries — success")
    void getAllInquiries_Success() {
        when(inquiryRepository.findAll())
                .thenReturn(Arrays.asList(mockInquiry));

        List<InquiryResponse> inquiries = inquiryService.getAllInquiries();

        assertNotNull(inquiries);
        assertEquals(1, inquiries.size());
        assertEquals("Product Inquiry", inquiries.get(0).getSubject());
    }

    @Test
    @DisplayName("Get inquiry by ID — success")
    void getInquiryById_Success() {
        when(inquiryRepository.findById(1L))
                .thenReturn(Optional.of(mockInquiry));

        InquiryResponse response = inquiryService.getInquiryById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("OPEN", response.getStatus());
    }

    @Test
    @DisplayName("Get inquiry by ID — not found")
    void getInquiryById_NotFound() {
        when(inquiryRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> inquiryService.getInquiryById(999L));
    }

    @Test
    @DisplayName("Mark resolved — success")
    void markResolved_Success() {
        when(inquiryRepository.findById(1L))
                .thenReturn(Optional.of(mockInquiry));
        when(inquiryRepository.save(any(Inquiry.class)))
                .thenReturn(mockInquiry);

        InquiryResponse response = inquiryService.markResolved(1L);

        assertNotNull(response);
        verify(inquiryRepository, times(1)).save(any(Inquiry.class));
    }
}