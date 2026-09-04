package com.globaltrade.ejb.test.integration;

import com.globaltrade.common.entity.CustomsDeclaration;
import com.globaltrade.common.entity.InventoryItem;
import com.globaltrade.common.entity.Shipment;
import com.globaltrade.common.entity.CarrierRoute;
import com.globaltrade.ejb.service.CustomsService;
import com.globaltrade.ejb.service.InventoryService;
import jakarta.annotation.security.PermitAll;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * A test facade that runs as an unauthenticated EJB to create test data.
 * Arquillian integration tests run unauthenticated by default.
 * To set up test data without triggering EJBAccessException on the secured EJBs,
 * this facade uses EntityManager directly.
 * The original explicit security tests still call the services directly to verify rejection.
 */
@Stateless
@PermitAll
public class TestSecurityFacade {

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    public InventoryItem addInventoryItem(InventoryItem item) {
        em.persist(item);
        return item;
    }

    public InventoryItem deductStock(Long itemId, int quantity) throws Exception {
        InventoryItem item = em.find(InventoryItem.class, itemId);
        item.setQuantity(item.getQuantity() - quantity);
        return em.merge(item);
    }

    public Shipment createShipment(Shipment shipment) {
        em.persist(shipment);
        return shipment;
    }

    public CustomsDeclaration createDeclaration(CustomsDeclaration declaration) throws Exception {
        if (declaration.getTariffRate() != null && declaration.getDeclaredValue() != null) {
            java.math.BigDecimal duty = declaration.getDeclaredValue()
                    .multiply(declaration.getTariffRate())
                    .divide(java.math.BigDecimal.valueOf(100));
            declaration.setDutyAmount(duty);
        }
        em.persist(declaration);
        return declaration;
    }

    public CarrierRoute addRoute(CarrierRoute route) {
        em.persist(route);
        return route;
    }

    public java.util.List<CustomsDeclaration> findPendingDeclarations() {
        return em.createNamedQuery("CustomsDeclaration.findPending", CustomsDeclaration.class).getResultList();
    }

    public java.util.List<CarrierRoute> findAllRoutes() {
        return em.createNamedQuery("CarrierRoute.findAll", CarrierRoute.class).getResultList();
    }

    public CarrierRoute findRouteById(Long routeId) {
        return em.find(CarrierRoute.class, routeId);
    }
}
