package com.warehouse.verification.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_products_ean", columnList = "ean")
})
public class Product {

    @Id
    @Column(name = "wid", length = 100, nullable = false, unique = true)
    private String wid;

    @Column(name = "ean", length = 13, nullable = false)
    private String ean;

    @Column(name = "manufacturing_date", nullable = false)
    private LocalDate manufacturingDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<VerificationLog> verificationLogs = new ArrayList<>();

    public Product() {}

    public Product(String wid, String ean, LocalDate manufacturingDate, LocalDate expiryDate) {
        this.wid = wid;
        this.ean = ean;
        this.manufacturingDate = manufacturingDate;
        this.expiryDate = expiryDate;
    }

    // Getters and Setters
    public String getWid() { return wid; }
    public void setWid(String wid) { this.wid = wid; }

    public String getEan() { return ean; }
    public void setEan(String ean) { this.ean = ean; }

    public LocalDate getManufacturingDate() { return manufacturingDate; }
    public void setManufacturingDate(LocalDate manufacturingDate) { this.manufacturingDate = manufacturingDate; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public List<VerificationLog> getVerificationLogs() { return verificationLogs; }
    public void setVerificationLogs(List<VerificationLog> verificationLogs) { this.verificationLogs = verificationLogs; }
}
