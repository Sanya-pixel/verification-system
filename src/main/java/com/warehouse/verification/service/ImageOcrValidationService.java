package com.warehouse.verification.service;

import com.warehouse.verification.model.Product;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ImageOcrValidationService {

    // Simulates OCR extraction (e.g., AWS Rekognition / Tesseract)
    // In production — replace with real OCR API call
    private String executeOpticalTextExtraction(MultipartFile targetFile) {
        return "MFG: 2026-01-15 EXP: 2028-01-15";
    }

    public boolean crossValidateImageDates(MultipartFile labelPhoto,
                                           Product persistentRecord) {
        String extractedText = executeOpticalTextExtraction(labelPhoto);

        Pattern dateExpression = Pattern.compile("\\b(\\d{4}-\\d{2}-\\d{2})\\b");
        Matcher textMatcher = dateExpression.matcher(extractedText);

        LocalDate parsedMfg = null;
        LocalDate parsedExp = null;

        if (textMatcher.find()) parsedMfg = LocalDate.parse(textMatcher.group(1));
        if (textMatcher.find()) parsedExp = LocalDate.parse(textMatcher.group(1));

        if (parsedMfg == null || parsedExp == null) {
            throw new IllegalArgumentException(
                    "OCR Error: Could not extract dates from image.");
        }

        return parsedMfg.equals(persistentRecord.getManufacturingDate())
                && parsedExp.equals(persistentRecord.getExpiryDate());
    }
}