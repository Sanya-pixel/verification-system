package com.warehouse.verification.controller;

import com.warehouse.verification.model.Product;
import com.warehouse.verification.model.VerificationLog;
import com.warehouse.verification.repository.ProductRepository;
import com.warehouse.verification.repository.VerificationLogRepository;
import com.warehouse.verification.service.ImageOcrValidationService;
import com.warehouse.verification.service.InventoryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/warehouse")
@CrossOrigin(origins = "*")
public class WarehouseController {

    private final ProductRepository productRepo;
    private final VerificationLogRepository logRepo;
    private final InventoryService inventoryService;
    private final ImageOcrValidationService ocrValidationService;

    public WarehouseController(ProductRepository productRepo,
                               VerificationLogRepository logRepo,
                               InventoryService inventoryService,
                               ImageOcrValidationService ocrValidationService) {
        this.productRepo = productRepo;
        this.logRepo = logRepo;
        this.inventoryService = inventoryService;
        this.ocrValidationService = ocrValidationService;
    }

    // ── 1. Bulk CSV Upload — ADMIN only ─────────────────────────────────────
    @PostMapping("/inventory/bulk-upload")
    public ResponseEntity<String> dispatchBulkUpload(
            @RequestParam("file") MultipartFile uploadedFile) {
        if (uploadedFile.isEmpty() ||
                !uploadedFile.getOriginalFilename().endsWith(".csv")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Validation Error: CSV format is required.");
        }
        inventoryService.executeAsynchronousBulkIngestion(uploadedFile);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body("Upload started in background.");
    }

    // ── 2. Verify Product by WID — ADMIN + OPERATOR ──────────────────────────
    @PostMapping("/verify/{wid}")
    public ResponseEntity<String> getOnTheSpotVerification(
            @PathVariable String wid,
            @RequestParam Integer operatorId,
            @RequestParam("image") MultipartFile productPhoto) {

        // 1. Fetch product from DB
        Product asset = productRepo.findById(wid)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Resource Target Item Not Found for WID: " + wid));

        // 2. Run OCR validation on uploaded image
        boolean datesMatch = ocrValidationService.crossValidateImageDates(productPhoto, asset);

        // 3. Log the verification event
        VerificationLog log = new VerificationLog();
        log.setProduct(asset);
        log.setOperatorId(operatorId);
        log.setVerifiedAt(LocalDateTime.now());
        logRepo.save(log);

        // 4. Return result
        if (!datesMatch) {
            return ResponseEntity.ok(
                    "Verification logged. WARNING: Physical label dates DO NOT match database records!");
        }
        return ResponseEntity.ok(
                "Verification logged successfully. Physical label dates match database records completely.");
    }

    // ── 3. QA Date-Range Report — ADMIN only ────────────────────────────────
    @GetMapping("/reports")
    public ResponseEntity<List<Map<String, Object>>> downloadOperationalReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

        LocalDateTime startBound = start.atStartOfDay();
        LocalDateTime endBound = end.atTime(23, 59, 59);

        List<Map<String, Object>> dataset =
                logRepo.getReportByCustomDateRange(startBound, endBound);
        return ResponseEntity.ok(dataset);
    }
}