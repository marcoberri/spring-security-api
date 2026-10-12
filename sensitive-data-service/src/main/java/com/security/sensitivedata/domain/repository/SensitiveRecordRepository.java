package com.security.sensitivedata.domain.repository;

import com.security.sensitivedata.domain.model.SensitiveRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SensitiveRecordRepository extends JpaRepository<SensitiveRecord, UUID> {
    List<SensitiveRecord> findByCategoryIgnoreCase(String category);
    List<SensitiveRecord> findByCreatedBy(String createdBy);
}
