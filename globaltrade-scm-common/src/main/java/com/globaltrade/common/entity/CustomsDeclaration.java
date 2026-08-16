package com.globaltrade.common.entity;

import com.globaltrade.common.enums.ComplianceStatus;
import jakarta.persistence.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a customs declaration for import/export documentation.
 * Tracks HS codes, tariff rates, and submission deadlines for trade compliance.
 *
 * <p>The {@code CustomsDeadlineTimer} creates programmatic timers based on
 * each declaration's submission deadline to ensure timely processing.</p>
 */
@Entity
@Table(name = "customs_declarations", indexes = {
        @Index(name = "idx_customs_shipment", columnList = "shipment_id"),
        @Index(name = "idx_customs_status", columnList = "status"),
        @Index(name = "idx_customs_deadline", columnList = "submission_deadline")
})
@NamedQueries({
        @NamedQuery(
                name = "CustomsDeclaration.findByShipment",
                query = "SELECT cd FROM CustomsDeclaration cd WHERE cd.shipmentId = :shipmentId"
        ),
        @NamedQuery(
                name = "CustomsDeclaration.findPending",
                query = "SELECT cd FROM CustomsDeclaration cd WHERE cd.status = com.globaltrade.common.enums.ComplianceStatus.PENDING " +
                        "ORDER BY cd.submissionDeadline ASC"
        ),
        @NamedQuery(
                name = "CustomsDeclaration.findApproachingDeadline",
                query = "SELECT cd FROM CustomsDeclaration cd WHERE cd.submissionDeadline <= :deadline " +
                        "AND cd.status IN (com.globaltrade.common.enums.ComplianceStatus.PENDING, " +
                        "com.globaltrade.common.enums.ComplianceStatus.UNDER_REVIEW)"
        ),
        @NamedQuery(
                name = "CustomsDeclaration.findNonCompliant",
                query = "SELECT cd FROM CustomsDeclaration cd WHERE cd.status = com.globaltrade.common.enums.ComplianceStatus.NON_COMPLIANT"
        )
})
public class CustomsDeclaration implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "declaration_id")
    private Long declarationId;

    @Column(name = "declaration_number", unique = true, nullable = false, length = 30)
    private String declarationNumber;

    @Column(name = "shipment_id", nullable = false)
    private Long shipmentId;

    @Column(name = "hs_code", nullable = false, length = 20)
    private String hsCode;

    @Column(name = "goods_description", nullable = false, length = 500)
    private String goodsDescription;

    @Column(name = "declared_value", precision = 15, scale = 2, nullable = false)
    private BigDecimal declaredValue;

    @Column(name = "tariff_rate", precision = 5, scale = 2)
    private BigDecimal tariffRate;

    @Column(name = "duty_amount", precision = 15, scale = 2)
    private BigDecimal dutyAmount;

    @Column(name = "origin_country", nullable = false, length = 100)
    private String originCountry;

    @Column(name = "destination_country", nullable = false, length = 100)
    private String destinationCountry;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ComplianceStatus status;

    @Column(name = "submission_deadline", nullable = false)
    private LocalDate submissionDeadline;

    @Column(name = "submitted_date")
    private LocalDate submittedDate;

    @Column(name = "approved_date")
    private LocalDate approvedDate;

    @Column(name = "trade_agreement", length = 100)
    private String tradeAgreement;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ComplianceStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ======================== Constructors ========================

    public CustomsDeclaration() {
    }

    public CustomsDeclaration(String declarationNumber, Long shipmentId, String hsCode,
                              String goodsDescription, BigDecimal declaredValue,
                              String originCountry, String destinationCountry,
                              LocalDate submissionDeadline) {
        this.declarationNumber = declarationNumber;
        this.shipmentId = shipmentId;
        this.hsCode = hsCode;
        this.goodsDescription = goodsDescription;
        this.declaredValue = declaredValue;
        this.originCountry = originCountry;
        this.destinationCountry = destinationCountry;
        this.submissionDeadline = submissionDeadline;
        this.status = ComplianceStatus.PENDING;
    }

    // ======================== Getters and Setters ========================

    public Long getDeclarationId() {
        return declarationId;
    }

    public void setDeclarationId(Long declarationId) {
        this.declarationId = declarationId;
    }

    public String getDeclarationNumber() {
        return declarationNumber;
    }

    public void setDeclarationNumber(String declarationNumber) {
        this.declarationNumber = declarationNumber;
    }

    public Long getShipmentId() {
        return shipmentId;
    }

    public void setShipmentId(Long shipmentId) {
        this.shipmentId = shipmentId;
    }

    public String getHsCode() {
        return hsCode;
    }

    public void setHsCode(String hsCode) {
        this.hsCode = hsCode;
    }

    public String getGoodsDescription() {
        return goodsDescription;
    }

    public void setGoodsDescription(String goodsDescription) {
        this.goodsDescription = goodsDescription;
    }

    public BigDecimal getDeclaredValue() {
        return declaredValue;
    }

    public void setDeclaredValue(BigDecimal declaredValue) {
        this.declaredValue = declaredValue;
    }

    public BigDecimal getTariffRate() {
        return tariffRate;
    }

    public void setTariffRate(BigDecimal tariffRate) {
        this.tariffRate = tariffRate;
    }

    public BigDecimal getDutyAmount() {
        return dutyAmount;
    }

    public void setDutyAmount(BigDecimal dutyAmount) {
        this.dutyAmount = dutyAmount;
    }

    public String getOriginCountry() {
        return originCountry;
    }

    public void setOriginCountry(String originCountry) {
        this.originCountry = originCountry;
    }

    public String getDestinationCountry() {
        return destinationCountry;
    }

    public void setDestinationCountry(String destinationCountry) {
        this.destinationCountry = destinationCountry;
    }

    public ComplianceStatus getStatus() {
        return status;
    }

    public void setStatus(ComplianceStatus status) {
        this.status = status;
    }

    public LocalDate getSubmissionDeadline() {
        return submissionDeadline;
    }

    public void setSubmissionDeadline(LocalDate submissionDeadline) {
        this.submissionDeadline = submissionDeadline;
    }

    public LocalDate getSubmittedDate() {
        return submittedDate;
    }

    public void setSubmittedDate(LocalDate submittedDate) {
        this.submittedDate = submittedDate;
    }

    public LocalDate getApprovedDate() {
        return approvedDate;
    }

    public void setApprovedDate(LocalDate approvedDate) {
        this.approvedDate = approvedDate;
    }

    public String getTradeAgreement() {
        return tradeAgreement;
    }

    public void setTradeAgreement(String tradeAgreement) {
        this.tradeAgreement = tradeAgreement;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "CustomsDeclaration{" +
                "declarationId=" + declarationId +
                ", declarationNumber='" + declarationNumber + '\'' +
                ", shipmentId=" + shipmentId +
                ", hsCode='" + hsCode + '\'' +
                ", status=" + status +
                ", deadline=" + submissionDeadline +
                '}';
    }
}
