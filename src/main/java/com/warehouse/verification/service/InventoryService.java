package com.warehouse.verification.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class InventoryService {

    private final JdbcTemplate jdbcTemplate;

    @Value("${spring.jpa.properties.hibernate.jdbc.batch_size:5000}")
    private int internalBatchThreshold;

    public InventoryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Async("bulkUploadExecutor")
    @Transactional
    public CompletableFuture<String> executeAsynchronousBulkIngestion(MultipartFile targetFile) {
        // Native transactional UPSERT structure to isolate uniqueness edge anomalies
        String upsertSql = "INSERT INTO products (wid, ean, manufacturing_date, expiry_date) " +
                "VALUES (?, ?, ?, ?) " +
                "ON CONFLICT (wid) DO UPDATE SET " +
                "ean = EXCLUDED.ean, " +
                "manufacturing_date = EXCLUDED.manufacturing_date, " +
                "expiry_date = EXCLUDED.expiry_date";

        try (BufferedReader streamBuffer = new BufferedReader(new InputStreamReader(targetFile.getInputStream(), StandardCharsets.UTF_8));
             CSVParser analyticalParser = new CSVParser(streamBuffer, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim())) {

            List<Object[]> transactionalBatch = new ArrayList<>();
            long itemProcessingCounter = 0;

            for (CSVRecord record : analyticalParser) {
                String wid = record.get("WID");
                String ean = record.get("EAN");
                LocalDate mfgDate = LocalDate.parse(record.get("Manufacturing_Date"), DateTimeFormatter.ISO_LOCAL_DATE);
                LocalDate expDate = LocalDate.parse(record.get("Expiry_Date"), DateTimeFormatter.ISO_LOCAL_DATE);

                transactionalBatch.add(new Object[]{wid, ean, mfgDate, expDate});
                itemProcessingCounter++;

                if (transactionalBatch.size() >= internalBatchThreshold) {
                    jdbcTemplate.batchUpdate(upsertSql, transactionalBatch);
                    transactionalBatch.clear();
                }
            }

            // Flush out remaining structural elements
            if (!transactionalBatch.isEmpty()) {
                jdbcTemplate.batchUpdate(upsertSql, transactionalBatch);
            }

            return CompletableFuture.completedFuture("Bulk data onboarding complete. Rows updated: " + itemProcessingCounter);

        } catch (Exception processAnomaly) {
            return CompletableFuture.failedFuture(new RuntimeException("Core runtime failure: Ingestion sequence broken: " + processAnomaly.getMessage()));
        }
    }
}
