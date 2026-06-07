package com.warehouse.verification.controller;

import com.warehouse.verification.model.Product;
import com.warehouse.verification.model.VerificationLog;
import com.warehouse.verification.repository.ProductRepository;
import com.warehouse.verification.repository.VerificationLogRepository;
import com.warehouse.verification.service.InventoryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/warehouse")
@CrossOrigin(origins = "*")
public class WarehouseController {

    private final InventoryService inventoryService;
    private final ProductRepository productRepository;
    private final VerificationLogRepository verificationLogRepository;

    public WarehouseController(InventoryService inventoryService,
                               ProductRepository productRepository,
                               VerificationLogRepository verificationLogRepository) {
        this.inventoryService = inventoryService;
        this.productRepository = productRepository;
        this.verificationLogRepository = verificationLogRepository;
    }

    @PostMapping("/inventory/bulk-upload")
    public ResponseEntity<String> dispatchBulkUpload(@RequestParam("file") MultipartFile uploadedFile) {
        if (uploadedFile.isEmpty() || !uploadedFile.getOriginalFilename().endsWith(".csv")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Validation Error: Terminating pipeline process. CSV format is required.");
        }
        inventoryService.executeAsynchronousBulkIngestion(uploadedFile);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("Processing transaction triggered. Batch operation background routing started.");
    }

    @GetMapping("/verify/{wid}")
    public ResponseEntity<Product> verifyProductAsset(@PathVariable String wid, @RequestParam Integer operatorId) {
        return productRepository.findById(wid)
                .map(matchingProduct -> {
                    VerificationLog entryLog = new VerificationLog(matchingProduct, operatorId, LocalDateTime.now(), null);
                    verificationLogRepository.save(entryLog);
                    return ResponseEntity.ok(matchingProduct);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/reports")
    public ResponseEntity<List<Map<String, Object>>> downloadOperationalReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

        LocalDateTime transactionStartBound = start.atStartOfDay();
        LocalDateTime transactionEndBound = end.atTime(23, 59, 59);

        List<Map<String, Object>> analyticalDataset = verificationLogRepository.getReportByCustomDateRange(transactionStartBound, transactionEndBound);
        return ResponseEntity.ok(analyticalDataset);
    }
}
