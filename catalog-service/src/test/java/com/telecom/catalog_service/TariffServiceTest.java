package com.telecom.catalog_service;

import com.telecom.catalog_service.dto.TariffRequest;
import com.telecom.catalog_service.dto.TariffResponse;
import com.telecom.catalog_service.exception.TariffNotFoundException;
import com.telecom.catalog_service.mapper.TariffMapper;
import com.telecom.catalog_service.model.Tariff;
import com.telecom.catalog_service.repository.TariffRepository;
import com.telecom.catalog_service.service.TariffService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TariffServiceTest {

    @Mock
    private TariffRepository tariffRepository;

    @Mock
    private TariffMapper tariffMapper;

    @InjectMocks
    private TariffService tariffService;

    // ========================================================================
    // 1. CREATE TARIFF
    // ========================================================================

    @Test
    @DisplayName("createTariff: Should save entity and return mapped response")
    void createTariff_shouldSaveAndReturnTariffResponse() {
        // Given
        TariffRequest request = new TariffRequest(
                "Pro 50GB", "High data package", new BigDecimal("250.00"),
                50, 1000, 1000, 30
        );

        Tariff tariffEntity = Tariff.builder()
                .id("tar-100")
                .name("Pro 50GB")
                .price(new BigDecimal("250.00"))
                .isActive(false)
                .build();

        TariffResponse expectedResponse = new TariffResponse(
                "tar-100", "Pro 50GB", "High data package", new BigDecimal("250.00"),
                50, 1000, 1000, false, 30
        );

        when(tariffMapper.toEntity(request)).thenReturn(tariffEntity);
        when(tariffRepository.save(tariffEntity)).thenReturn(tariffEntity);
        when(tariffMapper.toResponse(tariffEntity)).thenReturn(expectedResponse);

        // When
        TariffResponse actualResponse = tariffService.createTariff(request);

        // Then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.id()).isEqualTo("tar-100");
        assertThat(actualResponse.isActive()).isFalse();

        verify(tariffRepository, times(1)).save(tariffEntity);
    }

    // ========================================================================
    // 2. READ OPERATIONS
    // ========================================================================

    @Test
    @DisplayName("getTariffById: Should return response when tariff exists")
    void getTariffById_whenExists_shouldReturnResponse() {
        // Given
        String tariffId = "tar-200";
        Tariff tariff = Tariff.builder().id(tariffId).name("Standard 10GB").build();
        TariffResponse expectedResponse = new TariffResponse(
                tariffId, "Standard 10GB", null, BigDecimal.TEN, 10, 500, 500, true, 30
        );

        when(tariffRepository.findById(tariffId)).thenReturn(Optional.of(tariff));
        when(tariffMapper.toResponse(tariff)).thenReturn(expectedResponse);

        // When
        TariffResponse actualResponse = tariffService.getTariffById(tariffId);

        // Then
        assertThat(actualResponse.id()).isEqualTo(tariffId);
        verify(tariffRepository, times(1)).findById(tariffId);
    }

    @Test
    @DisplayName("getTariffById: Should throw TariffNotFoundException when ID does not exist")
    void getTariffById_whenNotFound_shouldThrowException() {
        // Given
        String tariffId = "missing-id";
        when(tariffRepository.findById(tariffId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> tariffService.getTariffById(tariffId))
                .isInstanceOf(TariffNotFoundException.class)
                .hasMessageContaining("Tariff not found");

        verify(tariffRepository, times(1)).findById(tariffId);
    }

    @Test
    @DisplayName("getAllTariffs: Should return list of all tariffs")
    void getAllTariffs_shouldReturnList() {
        // Given
        Tariff t1 = Tariff.builder().id("1").name("Tariff A").build();
        Tariff t2 = Tariff.builder().id("2").name("Tariff B").build();

        when(tariffRepository.findAll()).thenReturn(List.of(t1, t2));
        when(tariffMapper.toResponse(any())).thenReturn(mock(TariffResponse.class));

        // When
        List<TariffResponse> result = tariffService.getAllTariffs();

        // Then
        assertThat(result).hasSize(2);
        verify(tariffRepository, times(1)).findAll();
    }

    // ========================================================================
    // 3. UPDATE TARIFF
    // ========================================================================

    @Test
    @DisplayName("updateTariff: Should update fields, change active status, and return response")
    void updateTariff_whenExists_shouldUpdateAndActivate() {
        // Given
        String tariffId = "tar-300";
        TariffRequest request = new TariffRequest(
                "Pro 50GB Updated", "Updated desc", new BigDecimal("275.00"),
                50, 1000, 1000, 30
        );

        Tariff existingTariff = Tariff.builder()
                .id(tariffId)
                .name("Pro 50GB")
                .isActive(false)
                .build();

        TariffResponse expectedResponse = new TariffResponse(
                tariffId, "Pro 50GB Updated", "Updated desc", new BigDecimal("275.00"),
                50, 1000, 1000, true, 30
        );

        when(tariffRepository.findById(tariffId)).thenReturn(Optional.of(existingTariff));
        doNothing().when(tariffMapper).updateTariffFromRequest(request, existingTariff);
        when(tariffRepository.save(existingTariff)).thenReturn(existingTariff);
        when(tariffMapper.toResponse(existingTariff)).thenReturn(expectedResponse);

        // When
        TariffResponse actualResponse = tariffService.updateTariff(tariffId, request, true);

        // Then
        assertThat(existingTariff.isActive()).isTrue();
        assertThat(actualResponse.isActive()).isTrue();

        verify(tariffRepository, times(1)).findById(tariffId);
        verify(tariffRepository, times(1)).save(existingTariff);
    }

    @Test
    @DisplayName("updateTariff: Should throw TariffNotFoundException when ID does not exist")
    void updateTariff_whenNotFound_shouldThrowException() {
        // Given
        String missingId = "missing-update-id";
        when(tariffRepository.findById(missingId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> tariffService.updateTariff(missingId, mock(TariffRequest.class), true))
                .isInstanceOf(TariffNotFoundException.class);

        verify(tariffRepository, never()).save(any());
    }

    // ========================================================================
    // 4. DELETE TARIFF
    // ========================================================================

    @Test
    @DisplayName("deleteTariff: Should delete tariff when ID exists")
    void deleteTariff_whenExists_shouldDelete() {
        // Given
        String tariffId = "tar-400";
        when(tariffRepository.existsById(tariffId)).thenReturn(true);
        doNothing().when(tariffRepository).deleteById(tariffId);

        // When
        tariffService.deleteTariff(tariffId);

        // Then
        verify(tariffRepository, times(1)).existsById(tariffId);
        verify(tariffRepository, times(1)).deleteById(tariffId);
    }

    @Test
    @DisplayName("deleteTariff: Should throw TariffNotFoundException when ID does not exist")
    void deleteTariff_whenNotFound_shouldThrowException() {
        // Given
        String missingId = "missing-delete-id";
        when(tariffRepository.existsById(missingId)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> tariffService.deleteTariff(missingId))
                .isInstanceOf(TariffNotFoundException.class);

        verify(tariffRepository, never()).deleteById(anyString());
    }
}