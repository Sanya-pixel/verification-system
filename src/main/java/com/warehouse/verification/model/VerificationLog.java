package com.warehouse.verification.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "verification_logs", indexes = {
        @Index(name = "idx_logs_timestamp", columnList = "verified_at")
})
public class VerificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wid", nullable = false, foreignKey = @ForeignKey(name = "fk_logs_product_wid"))
    private Product product;

    @Column(name = "operator_id", nullable = false)
    private Integer operatorId;

    @Column(name = "verified_at", nullable = false)
    private LocalDateTime verifiedAt;

    @Column(name = "captured_image_url", length = 500)
    private String capturedImageUrl;

    public VerificationLog() {}

    public VerificationLog(Product product, Integer operatorId, LocalDateTime verifiedAt, String capturedImageUrl) {
        this.product = product;
        this.operatorId = operatorId;
        this.verifiedAt = verifiedAt;
        this.capturedImageUrl = capturedImageUrl;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Integer getOperatorId() { return operatorId; }
    public void setOperatorId(Integer operatorId) { this.operatorId = operatorId; }

    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }

    public String getCapturedImageUrl() { return capturedImageUrl; }
    public void setCapturedImageUrl(String capturedImageUrl) { this.capturedImageUrl = capturedImageUrl; }
}
