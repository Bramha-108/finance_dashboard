package com.finance.dashboard.dashboard.service;

import com.finance.dashboard.dashboard.dto.DashboardSummaryResponse;
import com.finance.dashboard.dashboard.dto.TrendPoint;
import com.finance.dashboard.records.dto.RecordResponse;
import com.finance.dashboard.records.model.RecordCategory;
import com.finance.dashboard.records.model.RecordType;
import com.finance.dashboard.records.repository.FinancialRecordRepository;
import com.finance.dashboard.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final FinancialRecordRepository repo;

    public DashboardSummaryResponse getSummary(LocalDate from,
                                               LocalDate to,
                                               Authentication auth) {
        UUID scopedUserId = getScopedUserId(auth);

        BigDecimal income   = repo.sumByType(RecordType.INCOME,  from, to, scopedUserId);
        BigDecimal expenses = repo.sumByType(RecordType.EXPENSE, from, to, scopedUserId);

        Map<String, BigDecimal> byCategory = repo
                .sumByCategory(from, to, scopedUserId)
                .stream()
                .collect(Collectors.toMap(
                        row -> ((RecordCategory) row[0]).name(),
                        row -> (BigDecimal) row[1]
                ));

        List<TrendPoint> trend = repo
                .monthlyTrend(from.minusMonths(5), scopedUserId)
                .stream()
                .map(row -> new TrendPoint(
                        ((Number) row[0]).intValue(),
                        ((Number) row[1]).intValue(),
                        (RecordType) row[2],
                        (BigDecimal) row[3]))
                .toList();

        List<RecordResponse> recent = repo
                .findRecent(scopedUserId, PageRequest.of(0, 10))
                .stream()
                .map(r -> new RecordResponse(
                        r.getId(), r.getAmount(), r.getType(), r.getCategory(),
                        r.getDate(), r.getNotes(),
                        r.getCreatedBy().getEmail(), r.getCreatedAt()))
                .toList();

        return new DashboardSummaryResponse(
                income, expenses, income.subtract(expenses),
                byCategory, trend, recent);
    }

    public Map<String, BigDecimal> getCategoryBreakdown(LocalDate from,
                                                        LocalDate to,
                                                        Authentication auth) {
        UUID scopedUserId = getScopedUserId(auth);
        return repo.sumByCategory(from, to, scopedUserId)
                .stream()
                .collect(Collectors.toMap(
                        row -> ((RecordCategory) row[0]).name(),
                        row -> (BigDecimal) row[1]
                ));
    }

    public List<TrendPoint> getMonthlyTrend(LocalDate since, Authentication auth) {
        UUID scopedUserId = getScopedUserId(auth);
        return repo.monthlyTrend(since, scopedUserId)
                .stream()
                .map(row -> new TrendPoint(
                        ((Number) row[0]).intValue(),
                        ((Number) row[1]).intValue(),
                        (RecordType) row[2],
                        (BigDecimal) row[3]))
                .toList();
    }

    public List<RecordResponse> getRecent(Authentication auth) {
        UUID scopedUserId = getScopedUserId(auth);
        return repo.findRecent(scopedUserId, PageRequest.of(0, 10))
                .stream()
                .map(r -> new RecordResponse(
                        r.getId(), r.getAmount(), r.getType(), r.getCategory(),
                        r.getDate(), r.getNotes(),
                        r.getCreatedBy().getEmail(), r.getCreatedAt()))
                .toList();
    }

    private UUID getScopedUserId(Authentication auth) {
        return SecurityUtils.hasRole(auth, "ROLE_ADMIN")
                ? null
                : SecurityUtils.getCurrentUserId(auth);
    }
}