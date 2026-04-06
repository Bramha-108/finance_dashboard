package com.finance.dashboard.records.repository;

import com.finance.dashboard.records.model.FinancialRecord;
import com.finance.dashboard.records.model.RecordCategory;
import com.finance.dashboard.records.model.RecordType;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;

public class RecordSpecification {

    private RecordSpecification() {}

    public static Specification<FinancialRecord> notDeleted() {
        return (root, q, cb) -> cb.isFalse(root.get("deleted"));
    }

    public static Specification<FinancialRecord> byType(RecordType type) {
        return (root, q, cb) ->
                type == null ? null : cb.equal(root.get("type"), type);
    }

    public static Specification<FinancialRecord> byCategory(RecordCategory category) {
        return (root, q, cb) ->
                category == null ? null : cb.equal(root.get("category"), category);
    }

    public static Specification<FinancialRecord> betweenDates(LocalDate from, LocalDate to) {
        return (root, q, cb) -> {
            if (from == null && to == null) return null;
            if (from == null) return cb.lessThanOrEqualTo(root.get("date"), to);
            if (to == null) return cb.greaterThanOrEqualTo(root.get("date"), from);
            return cb.between(root.get("date"), from, to);
        };
    }

    public static Specification<FinancialRecord> byOwner(java.util.UUID userId) {
        return (root, q, cb) ->
                cb.equal(root.get("createdBy").get("id"), userId);
    }
}