package com.telecom.catalog_service.controller;

import com.telecom.catalog_service.dto.TariffRequest;
import com.telecom.catalog_service.dto.TariffResponse;
import com.telecom.catalog_service.service.TariffService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tariffs")
@RequiredArgsConstructor
public class TariffController {

    private final TariffService tariffService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public TariffResponse createTariff(@RequestBody TariffRequest request) {
        return tariffService.createTariff(request);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public List<TariffResponse> getAllTariffs() {
        return tariffService.getAllTariffs();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<TariffResponse> getTariffById(@PathVariable String id) {
        return ResponseEntity.ok(tariffService.getTariffById(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTariff(@PathVariable String id) {
        tariffService.deleteTariff(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TariffResponse> updateTariff(
            @PathVariable String id,
            @RequestBody TariffRequest request,
            @RequestParam boolean activate) {

        return ResponseEntity.ok(tariffService.updateTariff(id, request, activate));
    }
}