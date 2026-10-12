package com.security.sensitivedata.domain.repository;

import com.security.sensitivedata.domain.model.AuditTrackingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditTrackingLogRepository extends JpaRepository<AuditTrackingLog, UUID> {
    List<AuditTrackingLog> findByUserId(String userId);
}
