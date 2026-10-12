package com.security.sensitivedata.infrastructure.config;

import com.security.sensitivedata.domain.model.AuditTrackingLog;
import com.security.sensitivedata.domain.model.SensitiveRecord;
import com.security.sensitivedata.domain.repository.AuditTrackingLogRepository;
import com.security.sensitivedata.domain.repository.SensitiveRecordRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestDataInitializer {

    @Bean
    public CommandLineRunner initTestData(
            SensitiveRecordRepository recordRepository,
            AuditTrackingLogRepository auditRepository) {
        return args -> {
            if (recordRepository.count() == 0) {
                SensitiveRecord rec1 = new SensitiveRecord(
                        "financial",
                        "{\"accountNumber\": \"IT99A1234567890\", \"balance\": 450000.50, \"currency\": \"EUR\"}",
                        "VIP Private Banking Account",
                        "system-init"
                );
                SensitiveRecord rec2 = new SensitiveRecord(
                        "medical",
                        "{\"patientId\": \"PT-998822\", \"diagnosis\": \"Hypertension stage 2\", \"bloodType\": \"0+\"}",
                        "Confidential medical records",
                        "system-init"
                );
                SensitiveRecord rec3 = new SensitiveRecord(
                        "identity",
                        "{\"ssn\": \"RSSMRA85M01H501Z\", \"passportNumber\": \"YA1234567\"}",
                        "User KYC Profile",
                        "system-init"
                );

                recordRepository.save(rec1);
                recordRepository.save(rec2);
                recordRepository.save(rec3);

                auditRepository.save(new AuditTrackingLog(
                        "SEED_TEST_DATA",
                        "SYSTEM",
                        "sensitive_records",
                        "INITIALIZED",
                        "Seeded 3 test sensitive records into shared database"
                ));
            }
        };
    }
}
