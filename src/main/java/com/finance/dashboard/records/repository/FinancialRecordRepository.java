package com.finance.dashboard.records.repository;

import com.finance.dashboard.records.model.FinancialRecord;
import com.finance.dashboard.records.model.RecordType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface FinancialRecordRepository
        extends JpaRepository<FinancialRecord, UUID>,
        JpaSpecificationExecutor<FinancialRecord> {

    @Query("""
        SELECT COALESCE(SUM(r.amount), 0)
        FROM FinancialRecord r
        WHERE r.type = :type
          AND r.deleted = false
          AND r.date BETWEEN :from AND :to
          AND (:userId IS NULL OR r.createdBy.id = :userId)
        """)
    BigDecimal sumByType(@Param("type") RecordType type,
                         @Param("from") LocalDate from,
                         @Param("to") LocalDate to,
                         @Param("userId") UUID userId);

    @Query("""
        SELECT r.category, SUM(r.amount)
        FROM FinancialRecord r
        WHERE r.deleted = false
          AND r.date BETWEEN :from AND :to
          AND (:userId IS NULL OR r.createdBy.id = :userId)
        GROUP BY r.category
        """)
    List<Object[]> sumByCategory(@Param("from") LocalDate from,
                                 @Param("to") LocalDate to,
                                 @Param("userId") UUID userId);

    @Query("""
        SELECT FUNCTION('YEAR', r.date), FUNCTION('MONTH', r.date), r.type, SUM(r.amount)
        FROM FinancialRecord r
        WHERE r.deleted = false
          AND r.date >= :since
          AND (:userId IS NULL OR r.createdBy.id = :userId)
        GROUP BY FUNCTION('YEAR', r.date), FUNCTION('MONTH', r.date), r.type
        ORDER BY FUNCTION('YEAR', r.date), FUNCTION('MONTH', r.date)
        """)
    List<Object[]> monthlyTrend(@Param("since") LocalDate since,
                                @Param("userId") UUID userId);

    @Query("""
        SELECT r FROM FinancialRecord r
        WHERE r.deleted = false
          AND (:userId IS NULL OR r.createdBy.id = :userId)
        ORDER BY r.createdAt DESC
        """)
    List<FinancialRecord> findRecent(@Param("userId") UUID userId, Pageable pageable);
}