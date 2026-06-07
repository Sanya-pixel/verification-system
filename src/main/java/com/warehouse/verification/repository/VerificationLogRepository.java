package com.warehouse.verification.repository;

import com.warehouse.verification.model.VerificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface VerificationLogRepository extends JpaRepository<VerificationLog, Long> {

    @Query("SELECT v.id as logId, v.product.wid as wid, v.verifiedAt as verifiedAt, v.operatorId as operatorId, " +
            "p.ean as ean, p.manufacturingDate as manufacturingDate, p.expiryDate as expiryDate, v.capturedImageUrl as imageUrl " +
            "FROM VerificationLog v JOIN v.product p " +
            "WHERE v.verifiedAt BETWEEN :startFrame AND :endFrame " +
            "ORDER BY v.verifiedAt DESC")
    List<Map<String, Object>> getReportByCustomDateRange(
            @Param("startFrame") LocalDateTime startFrame,
            @Param("endFrame") LocalDateTime endFrame
    );
}
