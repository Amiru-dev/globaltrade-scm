package com.globaltrade.common.dto;

import com.globaltrade.common.entity.Vendor;
import com.globaltrade.common.enums.ComplianceStatus;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Transfer Object for vendor information.
 * Used for transferring vendor data across service boundaries
 * without exposing JPA entity internals.
 */
public class VendorDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long vendorId;
    private String vendorCode;
    private String name;
    private String country;
    private String city;
    private String contactEmail;
    private String contactPhone;
    private String category;
    private BigDecimal performanceRating;
    private ComplianceStatus complianceStatus;
    private LocalDate contractExpiry;
    private BigDecimal annualVolume;
    private String paymentTerms;

    public VendorDTO() {
    }

    /**
     * Converts a Vendor entity to VendorDTO.
     */
    public static VendorDTO fromEntity(Vendor entity) {
        if (entity == null) {
            return null;
        }

        VendorDTO dto = new VendorDTO();
        dto.setVendorId(entity.getVendorId());
        dto.setVendorCode(entity.getVendorCode());
        dto.setName(entity.getName());
        dto.setCountry(entity.getCountry());
        dto.setCity(entity.getCity());
        dto.setContactEmail(entity.getContactEmail());
        dto.setContactPhone(entity.getContactPhone());
        dto.setCategory(entity.getCategory());
        dto.setPerformanceRating(entity.getPerformanceRating());
        dto.setComplianceStatus(entity.getComplianceStatus());
        dto.setContractExpiry(entity.getContractExpiry());
        dto.setAnnualVolume(entity.getAnnualVolume());
        dto.setPaymentTerms(entity.getPaymentTerms());

        return dto;
    }

    /**
     * Converts this VendorDTO to a Vendor entity.
     */
    public Vendor toEntity() {
        Vendor entity = new Vendor();
        entity.setVendorId(this.vendorId);
        entity.setVendorCode(this.vendorCode);
        entity.setName(this.name);
        entity.setCountry(this.country);
        entity.setCity(this.city);
        entity.setContactEmail(this.contactEmail);
        entity.setContactPhone(this.contactPhone);
        entity.setCategory(this.category);
        entity.setPerformanceRating(this.performanceRating);
        entity.setComplianceStatus(this.complianceStatus);
        entity.setContractExpiry(this.contractExpiry);
        entity.setAnnualVolume(this.annualVolume);
        entity.setPaymentTerms(this.paymentTerms);

        return entity;
    }

    // Getters and Setters
    public Long getVendorId() {
        return vendorId;
    }

    public void setVendorId(Long vendorId) {
        this.vendorId = vendorId;
    }

    public String getVendorCode() {
        return vendorCode;
    }

    public void setVendorCode(String vendorCode) {
        this.vendorCode = vendorCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getPerformanceRating() {
        return performanceRating;
    }

    public void setPerformanceRating(BigDecimal performanceRating) {
        this.performanceRating = performanceRating;
    }

    public ComplianceStatus getComplianceStatus() {
        return complianceStatus;
    }

    public void setComplianceStatus(ComplianceStatus complianceStatus) {
        this.complianceStatus = complianceStatus;
    }

    public LocalDate getContractExpiry() {
        return contractExpiry;
    }

    public void setContractExpiry(LocalDate contractExpiry) {
        this.contractExpiry = contractExpiry;
    }

    public BigDecimal getAnnualVolume() {
        return annualVolume;
    }

    public void setAnnualVolume(BigDecimal annualVolume) {
        this.annualVolume = annualVolume;
    }

    public String getPaymentTerms() {
        return paymentTerms;
    }

    public void setPaymentTerms(String paymentTerms) {
        this.paymentTerms = paymentTerms;
    }

    @Override
    public String toString() {
        return "VendorDTO{" +
                "vendorId=" + vendorId +
                ", vendorCode='" + vendorCode + '\'' +
                ", name='" + name + '\'' +
                ", country='" + country + '\'' +
                ", city='" + city + '\'' +
                ", contactEmail='" + contactEmail + '\'' +
                ", contactPhone='" + contactPhone + '\'' +
                ", category='" + category + '\'' +
                ", performanceRating=" + performanceRating +
                ", complianceStatus=" + complianceStatus +
                ", contractExpiry=" + contractExpiry +
                ", annualVolume=" + annualVolume +
                ", paymentTerms='" + paymentTerms + '\'' +
                '}';
    }
}