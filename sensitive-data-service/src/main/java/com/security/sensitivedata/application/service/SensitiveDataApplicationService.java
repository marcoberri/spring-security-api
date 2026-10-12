package com.security.sensitivedata.application.service;

import com.security.sensitivedata.application.dto.CreateSensitiveDataCommand;
import com.security.sensitivedata.application.dto.SearchSensitiveDataQuery;
import com.security.sensitivedata.application.dto.SensitiveDataResponse;
import com.security.sensitivedata.domain.model.AuditTrackingLog;
import com.security.sensitivedata.domain.model.SensitiveRecord;
import com.security.sensitivedata.domain.repository.AuditTrackingLogRepository;
import com.security.sensitivedata.domain.repository.SensitiveRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SensitiveDataApplicationService {

    private final SensitiveRecordRepository recordRepository;
    private final AuditTrackingLogRepository auditRepository;

    public SensitiveDataApplicationService(SensitiveRecordRepository recordRepository,
                                           AuditTrackingLogRepository auditRepository) {
        this.recordRepository = recordRepository;
        this.auditRepository = auditRepository;
    }

    @Transactional(readOnly = true)
    public List<SensitiveDataResponse> searchByCategory(SearchSensitiveDataQuery query, String userId) {
        List<SensitiveRecord> records = recordRepository.findByCategoryIgnoreCase(query.category());

        // Audit tracking
        auditRepository.save(new AuditTrackingLog(
                "SEARCH_CATEGORY",
                userId != null ? userId : "ANONYMOUS",
                "category:" + query.category(),
                "SUCCESS",
                "Found " + records.size() + " records"
        ));

        return records.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SensitiveDataResponse getById(UUID id, String userId) {
        SensitiveRecord record = recordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sensitive record not found for id: " + id));

        auditRepository.save(new AuditTrackingLog(
                "READ_BY_ID",
                userId != null ? userId : "ANONYMOUS",
                "record:" + id,
                "SUCCESS",
                "Record retrieved"
        ));

        return toResponse(record);
    }

    @Transactional
    public SensitiveDataResponse save(CreateSensitiveDataCommand command, String userId) {
        String actor = (userId != null && !userId.isBlank()) ? userId : "SYSTEM_USER";

        SensitiveRecord record = new SensitiveRecord(
                command.category(),
                command.payload(),
                command.secretNote(),
                actor
        );
        SensitiveRecord saved = recordRepository.save(record);

        auditRepository.save(new AuditTrackingLog(
                "CREATE_SENSITIVE_RECORD",
                actor,
                "record:" + saved.getId(),
                "SUCCESS",
                "Category: " + saved.getCategory()
        ));

        return toResponse(saved);
    }

    private SensitiveDataResponse toResponse(SensitiveRecord record) {
        return new SensitiveDataResponse(
                record.getId(),
                record.getCategory(),
                record.getEncryptedPayload(),
                record.getSecretNote(),
                record.getCreatedBy(),
                record.getCreatedAt()
        );
    }
}
