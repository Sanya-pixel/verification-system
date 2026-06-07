package com.warehouse.verification.controller;

import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/pod")
@CrossOrigin(origins = "*")
public class ProofOfDeliveryController {

    private final S3Client s3Client;
    private final String activeBucketName = "logistics-pod-records-bucket";

    public ProofOfDeliveryController(@Lazy S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @PostMapping("/synchronize")
    public ResponseEntity<Map<String, Object>> commitProofOfDelivery(
            @RequestParam("awbNumber") String awbNumber,
            @RequestParam("driverId") Integer driverId,
            @RequestParam("latitude") Double latitude,
            @RequestParam("longitude") Double longitude,
            @RequestParam("mediaFile") MultipartFile mediaFile) {

        try {
            String s3TargetKeyPath = "pod-records/" + LocalDate.now().getYear() + "/" + awbNumber + "_" + System.currentTimeMillis() + ".jpg";

            // Streaming file directly into cloud bucket without disk footprint allocation
            PutObjectRequest targetPutRequest = PutObjectRequest.builder()
                    .bucket(activeBucketName)
                    .key(s3TargetKeyPath)
                    .contentType(mediaFile.getContentType())
                    .build();

            s3Client.putObject(targetPutRequest, RequestBody.fromInputStream(mediaFile.getInputStream(), mediaFile.getSize()));

            Map<String, Object> stateResponseContract = new HashMap<>();
            stateResponseContract.put("air_waybill_number", awbNumber);
            stateResponseContract.put("status", "SYNCHRONIZED");
            stateResponseContract.put("cloud_resource_uri", "s3://" + activeBucketName + "/" + s3TargetKeyPath);
            stateResponseContract.put("processed_timestamp", LocalDateTime.now().toString());

            return ResponseEntity.ok(stateResponseContract);

        } catch (Exception synchronizationAnomaly) {
            Map<String, Object> errorErrorContract = new HashMap<>();
            errorErrorContract.put("error", "Failed to write cloud asset record trace: " + synchronizationAnomaly.getMessage());
            return ResponseEntity.internalServerError().body(errorErrorContract);
        }
    }
}
