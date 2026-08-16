package com.globaltrade.ejb.service;

import com.globaltrade.common.entity.CarrierRoute;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stateless Session Bean for carrier route optimization.
 *
 * <p>Calculates optimal shipping routes based on cost, transit time,
 * and carrier reliability. Invoked by the RouteOptimizationTimer
 * and directly by logistics coordinators.</p>
 */
@Stateless
@RolesAllowed({"LOGISTICS_COORDINATOR", "SYSTEM_ADMIN"})
public class RouteOptimizationService {

    private static final Logger LOGGER = Logger.getLogger(RouteOptimizationService.class.getName());

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    /**
     * Finds the most cost-effective route between two locations.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public CarrierRoute findCheapestRoute(String origin, String destination) {
        List<CarrierRoute> routes = em.createNamedQuery(
                        "CarrierRoute.findByOriginDestination", CarrierRoute.class)
                .setParameter("origin", origin)
                .setParameter("destination", destination)
                .getResultList();

        return routes.stream()
                .filter(CarrierRoute::isActive)
                .min(Comparator.comparing(CarrierRoute::getCostPerKg))
                .orElse(null);
    }

    /**
     * Finds the fastest route between two locations.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public CarrierRoute findFastestRoute(String origin, String destination) {
        List<CarrierRoute> routes = em.createNamedQuery(
                        "CarrierRoute.findByOriginDestination", CarrierRoute.class)
                .setParameter("origin", origin)
                .setParameter("destination", destination)
                .getResultList();

        return routes.stream()
                .filter(CarrierRoute::isActive)
                .min(Comparator.comparingInt(CarrierRoute::getTransitDays))
                .orElse(null);
    }

    /**
     * Finds the most reliable route between two locations.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public CarrierRoute findMostReliableRoute(String origin, String destination) {
        List<CarrierRoute> routes = em.createNamedQuery(
                        "CarrierRoute.findMostReliable", CarrierRoute.class)
                .setParameter("origin", origin)
                .setParameter("destination", destination)
                .setMaxResults(1)
                .getResultList();

        return routes.isEmpty() ? null : routes.get(0);
    }

    /**
     * Calculates a weighted optimal route score combining cost, time, and reliability.
     * Score = (0.4 × reliabilityScore) + (0.35 × costEfficiency) + (0.25 × speedScore)
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public CarrierRoute findOptimalRoute(String origin, String destination) {
        List<CarrierRoute> routes = em.createNamedQuery(
                        "CarrierRoute.findByOriginDestination", CarrierRoute.class)
                .setParameter("origin", origin)
                .setParameter("destination", destination)
                .getResultList();

        if (routes.isEmpty()) {
            LOGGER.log(Level.WARNING, "No routes found: {0} → {1}",
                    new Object[]{origin, destination});
            return null;
        }

        // Calculate max values for normalization
        BigDecimal maxCost = routes.stream()
                .map(CarrierRoute::getCostPerKg)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ONE);
        int maxDays = routes.stream()
                .mapToInt(CarrierRoute::getTransitDays)
                .max().orElse(1);

        // Find route with highest weighted score
        CarrierRoute optimal = routes.stream()
                .filter(CarrierRoute::isActive)
                .max(Comparator.comparingDouble(route -> {
                    double reliability = route.getReliabilityScore().doubleValue();
                    double costEfficiency = 1.0 - (route.getCostPerKg().doubleValue()
                            / maxCost.doubleValue());
                    double speedScore = 1.0 - ((double) route.getTransitDays() / maxDays);
                    return (0.4 * reliability) + (0.35 * costEfficiency) + (0.25 * speedScore);
                }))
                .orElse(null);

        if (optimal != null) {
            LOGGER.log(Level.INFO, "Optimal route {0} → {1}: {2} via {3} ({4} days, ${5}/kg)",
                    new Object[]{origin, destination, optimal.getRouteCode(),
                            optimal.getCarrier(), optimal.getTransitDays(),
                            optimal.getCostPerKg()});
        }
        return optimal;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<CarrierRoute> findAllRoutes() {
        return em.createNamedQuery("CarrierRoute.findAll", CarrierRoute.class)
                .getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public CarrierRoute addRoute(CarrierRoute route) {
        em.persist(route);
        return route;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void updateReliabilityScore(Long routeId, BigDecimal score) {
        CarrierRoute route = em.find(CarrierRoute.class, routeId);
        if (route != null) {
            route.setReliabilityScore(score);
            em.merge(route);
        }
    }
}
