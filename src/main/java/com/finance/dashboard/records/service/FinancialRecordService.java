package com.finance.dashboard.records.service;

import com.finance.dashboard.auth.model.User;
import com.finance.dashboard.records.dto.*;
import com.finance.dashboard.records.model.FinancialRecord;
import com.finance.dashboard.records.repository.FinancialRecordRepository;
import com.finance.dashboard.shared.exception.ResourceNotFoundException;
import com.finance.dashboard.shared.security.AppUserDetails;
import com.finance.dashboard.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.finance.dashboard.records.repository.RecordSpecification.*;

@Service
@RequiredArgsConstructor
@Transactional
public class FinancialRecordService {

    private final FinancialRecordRepository repo;

    @Transactional(readOnly = true)
    public Page<RecordResponse> findAll(RecordFilterRequest filter, Authentication auth) {

        Specification<FinancialRecord> spec = notDeleted()
                .and(byType(filter.type()))
                .and(byCategory(filter.category()))
                .and(betweenDates(filter.from(), filter.to()));

        if (!SecurityUtils.hasRole(auth, "ROLE_ADMIN")) {
            spec = spec.and(byOwner(SecurityUtils.getCurrentUserId(auth)));
        }

        Pageable pageable = PageRequest.of(
                filter.page() == null ? 0 : filter.page(),
                filter.size() == null ? 20 : filter.size(),
                Sort.by("date").descending()
        );
        return repo.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public RecordResponse findById(UUID id, Authentication auth) {
        FinancialRecord record = getActiveRecord(id);
        checkOwnership(record, auth);
        return toResponse(record);
    }

    public RecordResponse create(RecordRequest req, Authentication auth) {
        User user = ((AppUserDetails) auth.getPrincipal()).getUser();
        FinancialRecord record = new FinancialRecord();
        record.setCreatedBy(user);
        applyRequest(req, record);
        return toResponse(repo.save(record));
    }

    public RecordResponse update(UUID id, RecordRequest req, Authentication auth) {
        FinancialRecord record = getActiveRecord(id);
        checkOwnership(record, auth);
        applyRequest(req, record);
        return toResponse(repo.save(record));
    }

    public void softDelete(UUID id, Authentication auth) {
        FinancialRecord record = getActiveRecord(id);
        checkOwnership(record, auth);
        record.setDeleted(true);
    }

    private void applyRequest(RecordRequest req, FinancialRecord record) {
        record.setAmount(req.amount());
        record.setType(req.type());
        record.setCategory(req.category());
        record.setDate(req.date());
        record.setNotes(req.notes());
    }

    private FinancialRecord getActiveRecord(UUID id) {
        return repo.findById(id)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Record not found: " + id));
    }

    private void checkOwnership(FinancialRecord record, Authentication auth) {
        if (SecurityUtils.hasRole(auth, "ROLE_ADMIN")) return;
        UUID callerId = SecurityUtils.getCurrentUserId(auth);
        if (!record.getCreatedBy().getId().equals(callerId)) {
            throw new AccessDeniedException("You don't own this record");
        }
    }

    private RecordResponse toResponse(FinancialRecord r) {
        return new RecordResponse(
                r.getId(), r.getAmount(), r.getType(), r.getCategory(),
                r.getDate(), r.getNotes(),
                r.getCreatedBy().getEmail(), r.getCreatedAt()
        );
    }
}