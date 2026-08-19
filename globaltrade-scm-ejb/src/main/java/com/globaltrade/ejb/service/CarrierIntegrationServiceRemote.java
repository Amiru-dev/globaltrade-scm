package com.globaltrade.ejb.service;

import jakarta.ejb.Remote;

/**
 * Remote interface for carrier integration.
 *
 * <p><strong>Use Case:</strong> External systems or different applications accessing
 * carrier integration services across JVM boundaries.</p>
 */
@Remote
public interface CarrierIntegrationServiceRemote {
    String getCarrierInfoRemote(String carrierCode);
    CarrierQuote getQuoteRemote(String origin, String destination, double weight);
}