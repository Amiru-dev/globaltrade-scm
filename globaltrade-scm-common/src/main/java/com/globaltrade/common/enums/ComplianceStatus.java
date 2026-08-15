package com.globaltrade.common.enums;

/**
 * Represents the compliance status of a vendor or customs declaration
 * within trade regulatory frameworks.
 */
public enum ComplianceStatus {

    PENDING("Compliance check pending"),
    UNDER_REVIEW("Under regulatory review"),
    COMPLIANT("Fully compliant with trade regulations"),
    NON_COMPLIANT("Violation detected — action required"),
    CONDITIONALLY_APPROVED("Approved with conditions"),
    EXPIRED("Compliance certification expired"),
    SUSPENDED("Compliance status suspended");

    private final String description;

    ComplianceStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
