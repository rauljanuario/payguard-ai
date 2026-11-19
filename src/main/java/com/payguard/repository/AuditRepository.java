package com.payguard.repository;

import com.payguard.model.AuditReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditRepository extends JpaRepository<AuditReport, Long> {
    List<AuditReport> findAllByOrderByCreatedAtDesc();
}
