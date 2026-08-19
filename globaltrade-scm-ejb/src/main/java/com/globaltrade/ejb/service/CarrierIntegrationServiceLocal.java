package com.globaltrade.ejb.service;

import jakarta.ejb.Local;

/**
 * Local interface for carrier integration.
 *
 * <p><strong>Use Case:</strong> Internal system components communicating within
 * the same JVM (e.g., web tier to EJB tier within same application).</p>
 */
@Local
public interface CarrierIntegrationServiceLocal {
    String getCarrierInfoLocal(String carrierCode);
    CarrierQuote getQuoteLocal(String origin, String destination, double weight);
}