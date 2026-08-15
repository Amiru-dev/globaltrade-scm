package com.globaltrade.common.exception;

import jakarta.ejb.EJBException;

/**
 * Thrown when an external carrier system is completely unavailable — e.g.,
 * total API outage, network failure, or authentication rejection.
 *
 * <p><strong>This is a System Exception</strong> (extends {@code EJBException}).
 * Unlike Application Exceptions, system exceptions:</p>
 * <ul>
 *   <li>Cause automatic transaction rollback</li>
 *   <li>Trigger EJB instance discard (the container destroys the bean)</li>
 *   <li>Are logged by the container automatically</li>
 *   <li>Are wrapped in {@code EJBException} if not already one</li>
 * </ul>
 *
 * <p>Used for unrecoverable infrastructure failures that indicate
 * the system is in an inconsistent state and cannot continue safely.</p>
 */
public class CarrierSystemException extends EJBException {

    private final String carrierName;
    private final int retryAttempts;

    public CarrierSystemException(String message, String carrierName, int retryAttempts) {
        super(message);
        this.carrierName = carrierName;
        this.retryAttempts = retryAttempts;
    }

    public CarrierSystemException(String message, String carrierName,
                                   int retryAttempts, Throwable cause) {
        super(message, cause);
        this.carrierName = carrierName;
        this.retryAttempts = retryAttempts;
    }

    public String getCarrierName() { return carrierName; }
    public int getRetryAttempts() { return retryAttempts; }
}
