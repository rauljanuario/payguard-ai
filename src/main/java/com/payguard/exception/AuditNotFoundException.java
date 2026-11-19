package com.payguard.exception;

public class AuditNotFoundException extends RuntimeException {

    public AuditNotFoundException(Long auditId) {
        super("Audit report not found: " + auditId);
    }
}
