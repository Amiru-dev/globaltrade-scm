package com.globaltrade.web.servlet;

import com.globaltrade.ejb.service.InventoryService;
import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Transaction servlet for demonstrating EJB transaction management.
 * 
 * <p><strong>Critical Analysis:</strong> This servlet demonstrates both
 * Container-Managed Transactions (CMT) and Bean-Managed Transactions (BMT)
 * concepts by showing commit and rollback behavior in real-time.</p>
 */
@WebServlet("/transaction")
public class TransactionServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(TransactionServlet.class.getName());

    @EJB
    private InventoryService inventoryService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Check authentication
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userRole") == null) {
            response.sendRedirect("login");
            return;
        }
        
        String userRole = (String) session.getAttribute("userRole");
        String username = (String) session.getAttribute("username");
        
        LOGGER.log(Level.INFO, "Transaction demo accessed by user: {0} with role: {1}", 
                new Object[]{username, userRole});
        
        try {
            // Fetch transaction demo data
            Map<String, Object> transactionData = fetchTransactionData(userRole);
            
            // Set data for JSP
            request.setAttribute("transactionData", transactionData);
            request.setAttribute("username", username);
            request.setAttribute("userRole", userRole);
            request.setAttribute("roleDescription", session.getAttribute("roleDescription"));
            request.setAttribute("currentTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            // Forward to transaction JSP
            request.getRequestDispatcher("/transaction.jsp").forward(request, response);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading transaction demo data", e);
            
            // Create fallback data
            Map<String, Object> fallbackData = new HashMap<>();
            fallbackData.put("accessLevel", "Demo Mode - EJB Access Restricted");
            fallbackData.put("canExecuteTransactions", false);
            fallbackData.put("error", "EJB Security: " + e.getMessage());
            
            request.setAttribute("transactionData", fallbackData);
            request.setAttribute("username", username);
            request.setAttribute("userRole", userRole);
            request.setAttribute("roleDescription", session.getAttribute("roleDescription"));
            request.setAttribute("currentTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            request.getRequestDispatcher("/transaction.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Check authentication
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userRole") == null) {
            response.sendRedirect("login");
            return;
        }
        
        String userRole = (String) session.getAttribute("userRole");
        String operation = request.getParameter("operation");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            switch (operation) {
                case "transfer":
                    result = performTransfer(request);
                    break;
                case "deduct":
                    result = performDeduct(request);
                    break;
                case "replenish":
                    result = performReplenish(request);
                    break;
                case "rollback":
                    result = performRollbackDemo(request);
                    break;
                default:
                    result.put("success", false);
                    result.put("message", "Invalid operation");
            }
            
            // Store result in session for display
            session.setAttribute("transactionResult", result);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error performing transaction operation", e);
            result.put("success", false);
            result.put("message", "Transaction error: " + e.getMessage());
            session.setAttribute("transactionResult", result);
        }
        
        // Redirect back to transaction page
        response.sendRedirect("transaction");
    }

    /**
     * Fetches transaction demo data for display.
     */
    private Map<String, Object> fetchTransactionData(String userRole) {
        Map<String, Object> data = new HashMap<>();
        
        // Transaction information
        Map<String, Object> transactionInfo = new HashMap<>();
        transactionInfo.put("cmtExplanation", "Container-Managed Transactions (CMT): Container handles commit/rollback automatically based on @TransactionAttribute annotations");
        transactionInfo.put("bmtExplanation", "Bean-Managed Transactions (BMT): Developer explicitly controls transaction boundaries using UserTransaction");
        transactionInfo.put("defaultAttribute", "REQUIRED - Default: Joins existing transaction or creates new one");
        transactionInfo.put("rollbackConditions", "System exceptions trigger automatic rollback. Application exceptions require explicit @ApplicationException(rollback=true)");
        data.put("transactionInfo", transactionInfo);
        
        // Transaction attributes explanation
        Map<String, String> transactionAttributes = new HashMap<>();
        transactionAttributes.put("REQUIRED", "Default - Join existing transaction or create new");
        transactionAttributes.put("REQUIRES_NEW", "Always create new transaction, suspend existing");
        transactionAttributes.put("MANDATORY", "Must be called within existing transaction");
        transactionAttributes.put("SUPPORTS", "Use existing transaction if available, else non-transactional");
        transactionAttributes.put("NOT_SUPPORTED", "Execute non-transactionally, suspend existing");
        transactionAttributes.put("NEVER", "Execute non-transactionally, throw exception if transaction exists");
        data.put("transactionAttributes", transactionAttributes);
        
        // Role-based access control
        if ("SYSTEM_ADMIN".equals(userRole) || "LOGISTICS_COORDINATOR".equals(userRole) || "WAREHOUSE_MANAGER".equals(userRole)) {
            data.put("canExecuteTransactions", true);
            data.put("accessLevel", "Full Transaction Access");
        } else {
            data.put("canExecuteTransactions", false);
            data.put("accessLevel", "Read-Only Access");
        }
        
        return data;
    }
    
    /**
     * Performs inventory transfer transaction.
     */
    private Map<String, Object> performTransfer(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Long fromItemId = Long.parseLong(request.getParameter("fromItemId"));
            Long toItemId = Long.parseLong(request.getParameter("toItemId"));
            int quantity = Integer.parseInt(request.getParameter("quantity"));
            
            LOGGER.log(Level.INFO, "Executing transfer transaction: from={0}, to={1}, qty={2}", 
                    new Object[]{fromItemId, toItemId, quantity});
            
            // This method uses CMT with REQUIRED attribute
            inventoryService.transferInventory(fromItemId, toItemId, quantity);
            
            result.put("success", true);
            result.put("message", "Transfer completed successfully");
            result.put("operation", "TRANSFER");
            result.put("transactionType", "CMT (Container-Managed)");
            result.put("transactionAttribute", "REQUIRED");
            result.put("outcome", "COMMIT");
            result.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Transfer transaction failed", e);
            result.put("success", false);
            result.put("message", "Transfer failed: " + e.getMessage());
            result.put("operation", "TRANSFER");
            result.put("transactionType", "CMT (Container-Managed)");
            result.put("transactionAttribute", "REQUIRED");
            result.put("outcome", "ROLLBACK");
            result.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        
        return result;
    }
    
    /**
     * Performs stock deduction transaction.
     */
    private Map<String, Object> performDeduct(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Long itemId = Long.parseLong(request.getParameter("itemId"));
            int quantity = Integer.parseInt(request.getParameter("quantity"));
            
            LOGGER.log(Level.INFO, "Executing deduct transaction: item={0}, qty={1}", 
                    new Object[]{itemId, quantity});
            
            inventoryService.deductStock(itemId, quantity);
            
            result.put("success", true);
            result.put("message", "Stock deduction completed successfully");
            result.put("operation", "DEDUCT");
            result.put("transactionType", "CMT (Container-Managed)");
            result.put("transactionAttribute", "REQUIRED");
            result.put("outcome", "COMMIT");
            result.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Deduct transaction failed", e);
            result.put("success", false);
            result.put("message", "Deduct failed: " + e.getMessage());
            result.put("operation", "DEDUCT");
            result.put("transactionType", "CMT (Container-Managed)");
            result.put("transactionAttribute", "REQUIRED");
            result.put("outcome", "ROLLBACK");
            result.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        
        return result;
    }
    
    /**
     * Performs stock replenishment transaction.
     */
    private Map<String, Object> performReplenish(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Long itemId = Long.parseLong(request.getParameter("itemId"));
            int quantity = Integer.parseInt(request.getParameter("quantity"));
            
            LOGGER.log(Level.INFO, "Executing replenish transaction: item={0}, qty={1}", 
                    new Object[]{itemId, quantity});
            
            inventoryService.replenishStock(itemId, quantity);
            
            result.put("success", true);
            result.put("message", "Stock replenishment completed successfully");
            result.put("operation", "REPLENISH");
            result.put("transactionType", "CMT (Container-Managed)");
            result.put("transactionAttribute", "REQUIRED");
            result.put("outcome", "COMMIT");
            result.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Replenish transaction failed", e);
            result.put("success", false);
            result.put("message", "Replenish failed: " + e.getMessage());
            result.put("operation", "REPLENISH");
            result.put("transactionType", "CMT (Container-Managed)");
            result.put("transactionAttribute", "REQUIRED");
            result.put("outcome", "ROLLBACK");
            result.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        
        return result;
    }
    
    /**
     * Performs rollback demonstration.
     */
    private Map<String, Object> performRollbackDemo(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Simulate a transaction that will fail and rollback
            Long itemId = Long.parseLong(request.getParameter("itemId"));
            int quantity = Integer.parseInt(request.getParameter("quantity"));
            
            LOGGER.log(Level.INFO, "Executing rollback demo: item={0}, qty={1}", 
                    new Object[]{itemId, quantity});
            
            // Try to deduct more than available to trigger rollback
            try {
                inventoryService.deductStock(itemId, quantity);
                result.put("success", true);
                result.put("message", "Transaction completed (unexpected success)");
                result.put("outcome", "COMMIT");
            } catch (Exception e) {
                result.put("success", false);
                result.put("message", "Transaction rolled back due to: " + e.getMessage());
                result.put("outcome", "ROLLBACK");
                result.put("rollbackReason", e.getMessage());
            }
            
            result.put("operation", "ROLLBACK_DEMO");
            result.put("transactionType", "CMT (Container-Managed)");
            result.put("transactionAttribute", "REQUIRED");
            result.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Rollback demo failed", e);
            result.put("success", false);
            result.put("message", "Rollback demo error: " + e.getMessage());
            result.put("operation", "ROLLBACK_DEMO");
            result.put("outcome", "ERROR");
            result.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        
        return result;
    }
}