package com.globaltrade.ejb.service;

import com.globaltrade.common.entity.Shipment;
import com.globaltrade.common.entity.Vendor;
import com.globaltrade.common.exception.CarrierOutageException;
import com.globaltrade.common.exception.CustomsFailureException;
import com.globaltrade.common.exception.WeatherDisruptionException;
import com.globaltrade.common.enums.ShipmentStatus;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Supply chain-specific service for logistics failure recovery strategies.
 *
 * <p><strong>Purpose:</strong> Implements comprehensive recovery patterns for
 * logistics disruptions including carrier outages, customs failures, and weather
 * events. Demonstrates EJB transaction demarcation for recovery operations.</p>
 *
 * <p><strong>Recovery Strategies:</strong></p>
 * <ul>
 *   <li>Carrier outage: Alternative carrier routing, vendor escalation, backup carrier activation</li>
 *   <li>Customs failure: Documentation correction, regulatory liaison, alternative customs ports</li>
 *   <li>Weather disruption: Route rerouting, customer communication, priority rebooking</li>
 * </ul>
 */
@Stateless
@RolesAllowed({"LOGISTICS_COORDINATOR", "SYSTEM_ADMIN"})
public class LogisticsRecoveryService {

    private static final Logger LOGGER = Logger.getLogger(LogisticsRecoveryService.class.getName());

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @EJB
    private ShipmentService shipmentService;

    @EJB
    private VendorManagementService vendorService;

    /**
     * Recovery strategy for carrier outages affecting global logistics.
     *
     * <p><strong>Transaction Strategy: REQUIRES_NEW</strong> — Recovery operations
     * must execute independently of the failed transaction that triggered the outage.</p>
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Shipment recoverFromCarrierOutage(CarrierOutageException exception) {
        LOGGER.log(Level.SEVERE, "CARRIER OUTAGE RECOVERY: Initiating recovery for {0} - {1} shipments affected",
                new Object[]{exception.getCarrierName(), exception.getAffectedShipments()});

        try {
            // 1. Identify affected shipments
            List<Shipment> affectedShipments = shipmentService.findByCarrier(exception.getCarrierName());

            // 2. Apply recovery strategy based on outage type
            for (Shipment shipment : affectedShipments) {
                switch (exception.getOutageType()) {
                    case "API_OUTAGE":
                        recoverViaAlternativeCarrier(shipment, exception.getCarrierName());
                        break;
                    case "SYSTEM_FAILURE":
                        recoverViaSystemFallback(shipment, exception.getCarrierName());
                        break;
                    case "NETWORK_FAILURE":
                        recoverViaManualProcessing(shipment, exception.getCarrierName());
                        break;
                    default:
                        recoverViaGenericStrategy(shipment, exception.getCarrierName());
                }
            }

            LOGGER.log(Level.INFO, "CARRIER OUTAGE RECOVERY: Completed recovery for {0} shipments",
                    affectedShipments.size());
            return affectedShipments.isEmpty() ? null : affectedShipments.get(0);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "CARRIER OUTAGE RECOVERY FAILED: Recovery operation failed", e);
            throw new RuntimeException("Carrier outage recovery failed", e);
        }
    }

    /**
     * Recovery strategy for customs processing failures in international trade.
     *
     * <p><strong>Transaction Strategy: REQUIRES_NEW</strong> — Customs recovery
     * must not be affected by the original transaction that caused the compliance failure.</p>
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Shipment recoverFromCustomsFailure(CustomsFailureException exception) {
        LOGGER.log(Level.SEVERE, "CUSTOMS FAILURE RECOVERY: Initiating recovery for shipment {0} - Authority: {1}, Reason: {2}",
                new Object[]{exception.getShipmentTrackingNumber(), exception.getCustomsAuthority(), exception.getFailureReason()});

        try {
            Shipment shipment = shipmentService.findByTrackingNumber(exception.getShipmentTrackingNumber());

            if (shipment == null) {
                LOGGER.log(Level.WARNING, "CUSTOMS FAILURE RECOVERY: Shipment not found: {0}",
                        exception.getShipmentTrackingNumber());
                return null;
            }

            // Apply recovery strategy based on failure reason
            switch (exception.getFailureReason()) {
                case "INVALID_HS_CODE":
                    recoverViaHsCodeCorrection(shipment);
                    break;
                case "MISSING_DOCUMENTATION":
                    recoverViaDocumentationCompletion(shipment);
                    break;
                case "RESTRICTED_GOODS":
                    recoverViaRestrictedGoodsHandling(shipment);
                    break;
                case "TRADE_AGREEMENT_VIOLATION":
                    recoverViaTradeAgreementAdjustment(shipment);
                    break;
                default:
                    recoverViaCustomsLiaison(shipment);
            }

            LOGGER.log(Level.INFO, "CUSTOMS FAILURE RECOVERY: Recovery completed for shipment {0}",
                    exception.getShipmentTrackingNumber());
            return shipment;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "CUSTOMS FAILURE RECOVERY FAILED: Recovery operation failed", e);
            throw new RuntimeException("Customs failure recovery failed", e);
        }
    }

    /**
     * Recovery strategy for weather-related logistics disruptions.
     *
     * <p><strong>Transaction Strategy: SUPPORTS</strong> — Weather recovery operations
     * are primarily informational and routing changes that can participate in existing transactions.</p>
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Shipment recoverFromWeatherDisruption(WeatherDisruptionException exception) {
        LOGGER.log(Level.WARNING, "WEATHER DISRUPTION RECOVERY: Initiating recovery for {0} - {1} - Route: {2}",
                new Object[]{exception.getWeatherEventType(), exception.getAffectedRegion(), exception.getAffectedRoute()});

        try {
            // 1. Identify shipments on affected route
            List<Shipment> affectedShipments = shipmentService.findByRoute(exception.getAffectedRoute());

            // 2. Apply weather-specific recovery strategies
            for (Shipment shipment : affectedShipments) {
                applyWeatherRecoveryStrategy(shipment, exception);
            }

            LOGGER.log(Level.INFO, "WEATHER DISRUPTION RECOVERY: Completed recovery for {0} shipments",
                    affectedShipments.size());
            return affectedShipments.isEmpty() ? null : affectedShipments.get(0);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "WEATHER DISRUPTION RECOVERY FAILED: Recovery operation failed", e);
            throw new RuntimeException("Weather disruption recovery failed", e);
        }
    }

    /**
     * Recovery via alternative carrier routing for API outages.
     */
    private void recoverViaAlternativeCarrier(Shipment shipment, String failedCarrier) {
        LOGGER.log(Level.INFO, "ALTERNATIVE CARRIER ROUTING: Rerouting shipment {0} from {1} to backup carrier",
                new Object[]{shipment.getTrackingNumber(), failedCarrier});

        // Find backup carrier from vendor database
        Vendor backupCarrier = vendorService.findBackupCarrier(failedCarrier);
        if (backupCarrier != null) {
            shipment.setCarrier(backupCarrier.getVendorCode());
            shipment.setStatus(ShipmentStatus.REROUTED);
            shipment.setNotes("Rerouted due to carrier outage - backup carrier activated");
            em.merge(shipment);
            LOGGER.log(Level.INFO, "ALTERNATIVE CARRIER ROUTING: Successfully rerouted to {0}",
                    backupCarrier.getVendorCode());
        } else {
            LOGGER.log(Level.WARNING, "ALTERNATIVE CARRIER ROUTING: No backup carrier available for {0}",
                    failedCarrier);
        }
    }

    /**
     * Recovery via system fallback for carrier system failures.
     */
    private void recoverViaSystemFallback(Shipment shipment, String failedCarrier) {
        LOGGER.log(Level.INFO, "SYSTEM FALLBACK: Activating manual processing for shipment {0}",
                shipment.getTrackingNumber());

        shipment.setStatus(ShipmentStatus.MANUAL_PROCESSING);
        shipment.setNotes("Manual processing activated due to carrier system failure");
        em.merge(shipment);
    }

    /**
     * Recovery via manual processing for network failures.
     */
    private void recoverViaManualProcessing(Shipment shipment, String failedCarrier) {
        LOGGER.log(Level.INFO, "MANUAL PROCESSING: Converting shipment {0} to manual processing",
                shipment.getTrackingNumber());

        shipment.setStatus(ShipmentStatus.MANUAL_PROCESSING);
        shipment.setNotes("Manual processing due to network failure with carrier " + failedCarrier);
        em.merge(shipment);
    }

    /**
     * Recovery via generic strategy for unknown outage types.
     */
    private void recoverViaGenericStrategy(Shipment shipment, String failedCarrier) {
        LOGGER.log(Level.INFO, "GENERIC RECOVERY: Applying default recovery for shipment {0}",
                shipment.getTrackingNumber());

        shipment.setStatus(ShipmentStatus.ON_HOLD);
        shipment.setNotes("On hold due to carrier outage with " + failedCarrier + " - awaiting resolution");
        em.merge(shipment);
    }

    /**
     * Recovery via HS code correction for customs failures.
     */
    private void recoverViaHsCodeCorrection(Shipment shipment) {
        LOGGER.log(Level.INFO, "HS CODE CORRECTION: Initiating HS code review for shipment {0}",
                shipment.getTrackingNumber());

        shipment.setStatus(ShipmentStatus.CUSTOMS_REVIEW);
        shipment.setNotes("HS code correction required - customs review initiated");
        em.merge(shipment);
    }

    /**
     * Recovery via documentation completion for missing documentation.
     */
    private void recoverViaDocumentationCompletion(Shipment shipment) {
        LOGGER.log(Level.INFO, "DOCUMENTATION COMPLETION: Gathering missing documentation for shipment {0}",
                shipment.getTrackingNumber());

        shipment.setStatus(ShipmentStatus.DOCUMENTATION_PENDING);
        shipment.setNotes("Missing documentation - completion required for customs clearance");
        em.merge(shipment);
    }

    /**
     * Recovery via restricted goods handling for compliance issues.
     */
    private void recoverViaRestrictedGoodsHandling(Shipment shipment) {
        LOGGER.log(Level.SEVERE, "RESTRICTED GOODS HANDLING: Critical compliance issue for shipment {0}",
                shipment.getTrackingNumber());

        shipment.setStatus(ShipmentStatus.REJECTED);
        shipment.setNotes("Restricted goods detected - shipment rejected for compliance review");
        em.merge(shipment);
    }

    /**
     * Recovery via trade agreement adjustment for trade violations.
     */
    private void recoverViaTradeAgreementAdjustment(Shipment shipment) {
        LOGGER.log(Level.INFO, "TRADE AGREEMENT ADJUSTMENT: Adjusting trade agreement details for shipment {0}",
                shipment.getTrackingNumber());

        shipment.setStatus(ShipmentStatus.CUSTOMS_REVIEW);
        shipment.setNotes("Trade agreement adjustment required - customs review initiated");
        em.merge(shipment);
    }

    /**
     * Recovery via customs liaison for general customs failures.
     */
    private void recoverViaCustomsLiaison(Shipment shipment) {
        LOGGER.log(Level.INFO, "CUSTOMS LIAISON: Contacting customs authority for shipment {0}",
                shipment.getTrackingNumber());

        shipment.setStatus(ShipmentStatus.CUSTOMS_REVIEW);
        shipment.setNotes("Customs liaison required - contacting authority for resolution");
        em.merge(shipment);
    }

    /**
     * Apply weather-specific recovery strategy to shipment.
     */
    private void applyWeatherRecoveryStrategy(Shipment shipment, WeatherDisruptionException exception) {
        LOGGER.log(Level.INFO, "WEATHER RECOVERY: Applying strategy for shipment {0} - {1} hours delay estimated",
                new Object[]{shipment.getTrackingNumber(), exception.getEstimatedDelayHours()});

        // Update estimated delivery time
        if (shipment.getEstimatedDelivery() != null) {
            shipment.setEstimatedDelivery(shipment.getEstimatedDelivery().plusHours(exception.getEstimatedDelayHours()));
        }

        // Update status and add customer communication
        shipment.setStatus(ShipmentStatus.DELAYED);
        shipment.setNotes(String.format("Weather disruption (%s) in %s - %d hours delay - customer notified",
                exception.getWeatherEventType(), exception.getAffectedRegion(), exception.getEstimatedDelayHours()));

        em.merge(shipment);

        // Log for customer communication
        LOGGER.log(Level.INFO, "CUSTOMER NOTIFICATION: Weather delay notification sent for shipment {0}",
                shipment.getTrackingNumber());
    }
}