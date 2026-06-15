package com.coinbase.latencytracker.controller;

import com.coinbase.latencytracker.dto.AlertDto;
import com.coinbase.latencytracker.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    public ResponseEntity<Page<AlertDto>> getActiveAlerts(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                alertService.getActiveAlerts(
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
                )
        );
    }

    @GetMapping("/critical")
    public ResponseEntity<List<AlertDto>> getCriticalAlerts() {
        return ResponseEntity.ok(alertService.getCriticalAlerts());
    }

    @GetMapping("/count")
    public ResponseEntity<Long> countActiveAlerts() {
        return ResponseEntity.ok(alertService.countActiveAlerts());
    }

    @PatchMapping("/{id}/resolve")
    public ResponseEntity<AlertDto> resolveAlert(@PathVariable Long id) {
        return ResponseEntity.ok(alertService.resolveAlert(id));
    }
}
