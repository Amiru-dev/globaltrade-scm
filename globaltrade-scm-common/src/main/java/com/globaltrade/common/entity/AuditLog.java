package com.globaltrade.common.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Represents an audit trail entry for compliance and regulatory tracking.
 * Captures every significant operation performed on supply chain entities.
 *
 * <p>The {@code AuditLogInterceptor} automatically creates AuditLog entries
 * for all business method invocations on annotated EJB service beans.</p>
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_user", columnList = "user_id"),
    @Index(name = "idx_audit_entity", columnList = "entity_type, entity_id"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_action", columnList = "action")
})
@NamedQueries({
    @NamedQuery(
        name = "AuditLog.findByUser",
        query = "SELECT a FROM AuditLog a WHERE a.userId = :userId ORDER BY a.timestamp DESC"
    ),
    @NamedQuery(
        name = "AuditLog.findByEntity",
        query = "SELECT a FROM AuditLog a WHERE a.entityType = :entityType AND a.entityId = :entityId " +
                "ORDER BY a.timestamp DESC"
    ),
    @NamedQuery(
        name = "AuditLog.findByDateRange",
        query = "SELECT a FROM AuditLog a WHERE a.timestamp BETWEEN :startDate AND :endDate " +
                "ORDER BY a.timestamp DESC"
    ),
    @NamedQuery(
        name = "AuditLog.findByAction",
        query = "SELECT a FROM AuditLog a WHERE a.action = :action ORDER BY a.timestamp DESC"
    ),
    @NamedQuery(
        name = "AuditLog.findRecent",
        query = "SELECT a FROM AuditLog a ORDER BY a.timestamp DESC"
    )
})
public class AuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "user_role", length = 50)
    private String userRole;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", length = 50)
    private String entityId;

    @Column(name = "method_name", nullable = false, length = 100)
    private String methodName;

    @Column(name = "details", length = 1000)
    private String details;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "result_status", length = 20)
    private String resultStatus;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
    }

    // ======================== Constructors ========================

    public AuditLog() {
    }

    public AuditLog(String userId, String action, String entityType,
                    String entityId, String methodName, String details) {
        this.userId = userId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.methodName = methodName;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }

    // ======================== Getters and Setters ========================

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getResultStatus() { return resultStatus; }
    public void setResultStatus(String resultStatus) { this.resultStatus = resultStatus; }

    public Long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(Long executionTimeMs) { this.executionTimeMs = executionTimeMs; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "AuditLog{" +
                "logId=" + logId +
                ", userId='" + userId + '\'' +
                ", action='" + action + '\'' +
                ", entityType='" + entityType + '\'' +
                ", entityId='" + entityId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
